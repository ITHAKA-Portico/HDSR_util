package org.portico.warc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * https://iipc.github.io/warc-specifications/specifications/cdx-format/cdx-2015/
 * Default: 		CDX A b e a m s c k r V v D d g M n 
 * 11 fields:		CDX N b a m s k r M S V g
 * Old 9 fields: 	CDX N b a m s k r V g
 * @author dxie
 *
 */
public class Index {
	
	URI uri;
	String url;							//A canonized url
	String date;						//b date **
	String ip;							//e IP **
	String orig_url;					//a original url **
	String mime_type;					//m mime type of original document *
	String response_code;				//s response code *
	String old_checksum;				//c old style checksum *
	String new_checksum;				//k new style checksum *
	String redirect;					//r redirect *
	String offset_compressed;			//V compressed arc file offset *
	String offset_uncompressed;			//uncompressed arc file offset *
	String offset_dat;					//D compressed dat file offset
	String offset_date_uncompressed;	//uncompressed dat file offset
	String filename;					//file name
	long length;
	long offset;
	
	boolean HDSR_article_record;
	String HDSR_pub_id;					//If HDSR article indexes, there is an article pub_id
	String HDSR_pub_id_release_no;
	
	public Index() {
		
	}
	
	
	public Index(URI uri) {
		this.uri = uri;
		
		this.url = uri.toString();
	}


	public static void main(String[] args) {
		Index index = new Index();
		
		
		index.test();
	}
	
	
	
	
	
	
	private void test() {
		//
		String browsertrix_beatles_warcfile = "input/warc/browsertrix-crawler-20210111-hdsr-beatles.warc";
		
		RandomAccessFile randomAccessFile = null;
		try {
			randomAccessFile = new RandomAccessFile(browsertrix_beatles_warcfile, "r");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			randomAccessFile.seek(870);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		byte[] dest      = new byte[984];
		int    offset    = 0;
		int    length    = 984;
		try {
			int    bytesRead = randomAccessFile.read(dest, offset, length);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			randomAccessFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String s = new String(dest, StandardCharsets.UTF_8);
		System.out.println(s);
	}


	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getOrig_url() {
		return orig_url;
	}
	public void setOrig_url(String orig_url) {
		this.orig_url = orig_url;
	}
	public String getMime_type() {
		return mime_type;
	}
	public void setMime_type(String mime_type) {
		this.mime_type = mime_type;
	}
	public String getResponse_code() {
		return response_code;
	}
	public void setResponse_code(String response_code) {
		this.response_code = response_code;
	}
	public String getOld_checksum() {
		return old_checksum;
	}
	public void setOld_checksum(String old_checksum) {
		this.old_checksum = old_checksum;
	}
	public String getNew_checksum() {
		return new_checksum;
	}
	public void setNew_checksum(String new_checksum) {
		this.new_checksum = new_checksum;
	}
	public String getRedirect() {
		return redirect;
	}
	public void setRedirect(String redirect) {
		this.redirect = redirect;
	}
	public String getOffset_compressed() {
		return offset_compressed;
	}
	public void setOffset_compressed(String offset_compressed) {
		this.offset_compressed = offset_compressed;
	}
	public String getOffset_uncompressed() {
		return offset_uncompressed;
	}
	public void setOffset_uncompressed(String offset_uncompressed) {
		this.offset_uncompressed = offset_uncompressed;
	}
	public String getOffset_dat() {
		return offset_dat;
	}
	public void setOffset_dat(String offset_dat) {
		this.offset_dat = offset_dat;
	}
	public String getOffset_date_uncompressed() {
		return offset_date_uncompressed;
	}
	public void setOffset_date_uncompressed(String offset_date_uncompressed) {
		this.offset_date_uncompressed = offset_date_uncompressed;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public long getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	public long getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}


	public URI getUri() {
		return uri;
	}


	public void setUri(URI uri) {
		this.uri = uri;
	}


	public void setLength(long length) {
		this.length = length;
	}


	public void setOffset(long offset) {
		this.offset = offset;
	}


	public String getHDSR_pub_id() {
		return HDSR_pub_id;
	}


	public void setHDSR_pub_id(String hDSR_pub_id) {
		HDSR_pub_id = hDSR_pub_id;
	}


	public String getHDSR_pub_id_release_no() {
		return HDSR_pub_id_release_no;
	}


	public void setHDSR_pub_id_release_no(String hDSR_pub_id_release_no) {
		HDSR_pub_id_release_no = hDSR_pub_id_release_no;
	}


	public boolean isHDSR_article_record() {
		return HDSR_article_record;
	}


	public void setHDSR_article_record(boolean hDSR_article_record) {
		HDSR_article_record = hDSR_article_record;
	}
	
	
	

}
