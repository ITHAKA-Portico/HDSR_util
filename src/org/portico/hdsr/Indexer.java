package org.portico.hdsr;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.netpreserve.jwarc.HttpRequest;
import org.netpreserve.jwarc.HttpResponse;
import org.netpreserve.jwarc.IOUtils;
import org.netpreserve.jwarc.MediaType;
import org.netpreserve.jwarc.MessageBody;
import org.netpreserve.jwarc.WarcCaptureRecord;
import org.netpreserve.jwarc.WarcDigest;
import org.netpreserve.jwarc.WarcPayload;
import org.netpreserve.jwarc.WarcReader;
import org.netpreserve.jwarc.WarcRecord;
import org.netpreserve.jwarc.WarcRequest;
import org.netpreserve.jwarc.WarcResource;
import org.netpreserve.jwarc.WarcResponse;
import org.portico.warc.Index;

public class Indexer {
	
	static Logger logger = LogManager.getLogger(Indexer.class.getName());
	final static String programName = "Indexer";
	
	static String input_dir = "input";
	static String output_dir = "output";
	static String warc_subdir = "warc";
	static String index_subdir = "index";

	public static void main(String[] args) {
		Indexer indexer = new Indexer();
		List<Index> indexes = new ArrayList<>();
		
		String warcfile =  "rec-20210121212655838041-Issue1.1_article1.warc";
		
		String browsertrix_beatles_warcfile = "browsertrix-crawler-20210111-hdsr-beatles.warc";
		indexes = Indexer.createIndexFromWarc(browsertrix_beatles_warcfile);
		indexer.playWithWarcFile(browsertrix_beatles_warcfile, indexes);
		
		String browsertrix_error_warcfile = "browsertrix-crawler-20210127-hdsr-error.warc";
		//indexes = Indexer.createIndexFromWarc(browsertrix_error_warcfile);
		//indexer.playWithWarcFile(browsertrix_beatles_warcfile, indexes);
		
		String browsertrix_covid_india_warcfile = "browsertrix-crawler-20210209-hdsr-covid-india.warc";
		indexes = Indexer.createIndexFromWarc(browsertrix_covid_india_warcfile);
		indexer.playWithWarcFile(browsertrix_covid_india_warcfile, indexes);
		
		String browsertrix_mitigating_bias_warcfile = "browsertrix-crawler-20210209-hdsr-mitigating-bias.warc";
		indexes = Indexer.createIndexFromWarc(browsertrix_mitigating_bias_warcfile);
		indexer.playWithWarcFile(browsertrix_mitigating_bias_warcfile, indexes);
		
		String browsertrix_spatiotemporal_warcfile = "browsertrix-crawler-20210127-hdsr-spatiotemporal.warc";
		//indexes = Indexer.createIndexFromWarc(browsertrix_spatiotemporal_warcfile);
		
		String browsertrix_unicorn_warcfile = "browsertrix-crawler-20210127-hdsr-unicorn.warc";
		//indexes = Indexer.createIndexFromWarc(browsertrix_unicorn_warcfile);
		
		String brozzler_beatlesarticle_warcfile = "brozzler-20200727151952769-hdsr-beatlesarticle.warc";
		//indexes = Indexer.createIndexFromWarc(brozzler_beatlesarticle_warcfile);
		
		
		
		

	}


	/**
	 * Use jwarc method to create .cdx index file saved under output/index/ directory. Also return a list of Index object.
	 * @param warcFileName
	 * @return 
	 */
	public static List<Index> createIndexFromWarc(String warcFileName) {
		
		String input_warc_file = input_dir + File.separator + warc_subdir + File.separator + warcFileName;
		String output_index_file = output_dir + File.separator + index_subdir + File.separator + warcFileName + ".cdx";
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(output_index_file, true));
		} catch (IOException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		
		DateTimeFormatter arcDate = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(UTC);
		
		List<Index> indexes = new ArrayList<>();

		try (WarcReader reader = new WarcReader(Paths.get(input_warc_file))) {
			WarcRecord record = reader.next().orElse(null);
			while (record != null) {
				if ((record instanceof WarcResponse || record instanceof WarcResource) &&
						((WarcCaptureRecord) record).payload().isPresent()) {
					WarcPayload payload = null;
					try {
						payload = ((WarcCaptureRecord) record).payload().get();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					MediaType type;
					try {
						type = payload.type().base();
					} catch (IllegalArgumentException e) {
						type = MediaType.OCTET_STREAM;
					}
					URI uri = ((WarcCaptureRecord) record).targetURI();
					String date = arcDate.format(record.date());
					int status = 0;
					try {
						status = record instanceof WarcResponse ? ((WarcResponse) record).http().status() : 200;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String digest = payload.digest().map(WarcDigest::base32).orElse("-");
					long position = reader.position();

					// advance to the next record so we can calculate the length
					try {
						record = reader.next().orElse(null);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					long length = reader.position() - position;

					writer.append(uri +"\t" + date +"\t" +  uri +"\t" + type +"\t" +  status +"\t" + digest +"\t" +  length +"\t" +  position +"\t" + warcFileName + "\n");
					
					Index index = new Index(uri);
					index.setDate(date);
					index.setMime_type(type.toString());
					index.setResponse_code(status + "");
					index.setLength(length );
					index.setOffset(position);
					index.setFilename(warcFileName);
					
					indexes.add(index);
					
					String uri_string = uri.toString();
					if ( uri_string.startsWith("https://hdsr.mitpress.mit.edu/pub/")) {
						Pattern p = Pattern.compile("https://hdsr.mitpress.mit.edu/pub/([^/]*)(/release/(\\d*))*");
						Matcher m = p.matcher(uri_string);
						
						if ( m.find()) {
							String id = m.group(1);
							String release_no = m.group(3);
							
							index.setHDSR_article_record(true);
							index.setHDSR_pub_id(id);
							index.setHDSR_pub_id_release_no(release_no);

						}
						else {
							index.setHDSR_article_record(false);
						}
					}
					
					
				} 
				else {
					try {
						record = reader.next().orElse(null);
					} catch (IOException e) {
						logger.error( programName + ":createIndexFromWarc " + e.getMessage());
						e.printStackTrace();
					}
				}
			}
			
			writer.close();

		} catch (IOException e2) {
			logger.error( programName + ":creatIndexFromWarc Error creating index for file " + warcFileName + " " + e2.getMessage());
			e2.printStackTrace();
		}
		
		return indexes;
		
	}
	
	
	private void playWithWarcFile(String warcfile, List<Index> indexes) {

		String HDSR_article_pub_id = Utility.findHDSRArticlePubId( indexes);
		
		for(int i = 0; i< indexes.size(); i++) {
			
			Index index = indexes.get(i);
			
			String mime_type = index.getMime_type();
			
			if ( mime_type.equalsIgnoreCase("image/jpeg") || mime_type.equalsIgnoreCase("image/png") ) {
				//extract this jpg file
				Utility.extractFileOfIndex(index, HDSR_article_pub_id);
			}
			else if ( mime_type.equalsIgnoreCase("audio/mp3")) {
				//extract this mp3 file
				Utility.extractFileOfIndex(index, HDSR_article_pub_id);
			}
			else if ( mime_type.equalsIgnoreCase("text/css")) {
				//extract this css file
				Utility.extractFileOfIndex(index, HDSR_article_pub_id);
			}
			else if ( mime_type.equalsIgnoreCase("application/pdf")) {
				//extract this css file
				Utility.extractFileOfIndex(index, HDSR_article_pub_id);
			}
		}
		
	}

	


	/**
	 * Look into warc file
	 * @param warcFileName
	 */
	private void pokeWarcFile(String warcFileName) {
		
		

		

		
		
	}

	public String getIndex_subdir() {
		return index_subdir;
	}

	public void setIndex_subdir(String index_subdir) {
		this.index_subdir = index_subdir;
	}

	public String getWarc_subdir() {
		return warc_subdir;
	}

	public void setWarc_subdir(String warc_subdir) {
		this.warc_subdir = warc_subdir;
	}

}
