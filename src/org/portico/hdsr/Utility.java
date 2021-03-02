package org.portico.hdsr;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.netpreserve.jwarc.HttpRequest;
import org.netpreserve.jwarc.HttpResponse;
import org.netpreserve.jwarc.IOUtils;
import org.netpreserve.jwarc.MessageBody;
import org.netpreserve.jwarc.WarcReader;
import org.netpreserve.jwarc.WarcRecord;
import org.netpreserve.jwarc.WarcRequest;
import org.netpreserve.jwarc.WarcResponse;
import org.portico.warc.Index;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;


public class Utility {
	
	static Logger logger = LogManager.getLogger(Utility.class.getName());
	final static String programName = "Utility";
	
	static String input_dir = "input";
	static String output_dir = "output";
	static String warc_subdir = "warc";
	static String index_subdir = "index";

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	
	/**
	 * This method returns the html page content in list of Strings.
	 * @param urlString 
	 * @param printFlag 
	 * @return
	 * @throws IOException 
	 */
	public static List<String> getWebPageContent(String urlString, boolean printFlag ) throws IOException {
		List<String> content = new ArrayList<>();
		
		URL url;
	    InputStream is = null;
	    BufferedReader br;
	    String line;
	    String redirectUrl = null;

	    try {

	        //Scenario 3: from java tutorial  https://docs.oracle.com/javase/tutorial/deployment/doingMoreWithRIA/accessingCookies.html
	        CookieManager manager = new CookieManager();
	        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
	        CookieHandler.setDefault(manager);
	        url = new URL(urlString);
	        if ( urlString.startsWith("https://")) {
	        	HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		        connection.addRequestProperty("User-Agent", "Portico Issue Checker");
		        //print cookies
		        CookieStore cookieJar = manager.getCookieStore();  // get cookies from underlying CookieStore
		        List<HttpCookie> cookies = cookieJar.getCookies();
		        for (HttpCookie cookie: cookies) {
		        	logger.info("CookieHandler retrieved cookie: " + cookie);
		        }

		        is = connection.getInputStream();
		        if( !urlString.equalsIgnoreCase(connection.getURL().toString())) {
		        	redirectUrl = connection.getURL().toString();
		        	logger.info("Redirect to page: " + redirectUrl);
		        }
	        }
	        else {
	        	URLConnection connection = url.openConnection();
	        	connection.addRequestProperty("User-Agent", "Portico Issue Checker");
	        	//print cookies
	        	CookieStore cookieJar = manager.getCookieStore();  // get cookies from underlying CookieStore
	        	List<HttpCookie> cookies = cookieJar.getCookies();
	        	if ( printFlag ) {
	        		for (HttpCookie cookie: cookies) {
	        			logger.info("CookieHandler retrieved cookie: " + cookie);
	        		}
	        	}

	        	is = connection.getInputStream();
	        	if( !urlString.equalsIgnoreCase(connection.getURL().toString())) {
	        		redirectUrl = connection.getURL().toString();
	        		logger.info("Redirect to page: " + redirectUrl);
		        }
	        }
	        br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

	        int i =0;
	        while ((line = br.readLine()) != null) {
	        	if ( printFlag ) {
	        		System.out.println("line " + i++ + "------>" +  line);
	        	}
 	        	content.add(line);

	        }
	        
	    } catch (MalformedURLException mue) {
	         //mue.printStackTrace();
	    	throw mue;
	    } catch (IOException ioe) {
	        // ioe.printStackTrace();
	         System.out.println(ioe.getMessage());
	    	throw ioe;
	    } finally {
	        try {
	            if (is != null) is.close();
	        } catch (IOException ioe) {
	            
	        }
	    }
	    
		return content;
	}


	/**
	 * THe method retrieves all <a> tags from page_content_in_list
	 * @param page_content_in_list
	 * @return
	 */
	public static List<String> getATags(List<String> page_content_in_list) {
		List<String> a_links = new ArrayList<>();
		
		//Search pattern in joined String
		String search_pattern = "<a href=\"([^>]*)\"([^>]*)>.*?</a>";
		Pattern linkPattern = Pattern.compile( search_pattern,  Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);
        Matcher m = linkPattern.matcher(String.join("", page_content_in_list).replaceAll("\\s+", " "));
        String target_content = null;
        
        String matched_content = "";
        
        while(m.find()) {
        	matched_content += m.group(0) + "\n";
        	String link = m.group(1);
        	a_links.add(m.group(0));
        }
        
        if ( matched_content.isEmpty()) {
        	matched_content = null;
        }
        else {
        	//System.out.println(matched_content);
        }
		
		return a_links;
	}


	public static List<String> getMetaTags(List<String> page_content_in_list) {
		List<String> meta_tags = new ArrayList<>();
		
		//Search pattern in joined String
		String search_pattern = "<meta [^>]*/>";
		Pattern linkPattern = Pattern.compile( search_pattern,  Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);
        Matcher m = linkPattern.matcher(String.join("", page_content_in_list).replaceAll("\\s+", " "));
        
        String matched_content = "";
        
        while(m.find()) {
        	matched_content += m.group(0) + "\n";
        	meta_tags.add(m.group(0));
        }
        
        if ( matched_content.isEmpty()) {
        	matched_content = null;
        }
        else {
        	//System.out.println(matched_content);
        }
		
		return meta_tags;
	}


	public static List<String> getLinkTags(List<String> page_content_in_list) {
		List<String> link_tags = new ArrayList<>();
		
		//Search pattern in joined String
		String search_pattern = "<link [^>]*/>";
		Pattern linkPattern = Pattern.compile( search_pattern,  Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);
        Matcher m = linkPattern.matcher(String.join("", page_content_in_list).replaceAll("\\s+", " "));
        
        String matched_content = "";
        
        while(m.find()) {
        	matched_content += m.group(0) + "\n";
        	link_tags.add(m.group(0));
        }
        
        if ( matched_content.isEmpty()) {
        	matched_content = null;
        }
        else {
        	//System.out.println(matched_content);
        }
		
		return link_tags;
	}

	

	/**
	 * This method excludes <script> </script> part before searching http links
	 * @param page_content_in_list
	 * @return
	 */
	public static List<String> getHttpLinksExcludeScript(List<String> page_content_in_list) {
		Set<String> http_urls = new HashSet<>();
		
		//Code of Patterns from https://github.com/android/platform_frameworks_base/blob/master/core/java/android/util/Patterns.java
		String GOOD_IRI_CHAR = "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";
		String IRI = "[" + GOOD_IRI_CHAR + "]([" + GOOD_IRI_CHAR + "\\-]{0,61}[" + GOOD_IRI_CHAR + "]){0,1}";
		String GOOD_GTLD_CHAR = "a-zA-Z\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";
		String GTLD = "[" + GOOD_GTLD_CHAR + "]{2,63}";
		String HOST_NAME = "(" + IRI + "\\.)+" + GTLD;
		Pattern IP_ADDRESS = Pattern.compile(
				"((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
						+ "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
						+ "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
						+ "|[1-9][0-9]|[0-9]))");
		Pattern DOMAIN_NAME = Pattern.compile("(" + HOST_NAME + "|" + IP_ADDRESS + ")");
		
		//Search pattern in joined String
		String search_pattern = "((?:(http|https|Http|Https):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
				+ "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
				+ "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
				+ "(?:" + DOMAIN_NAME + ")"
				+ "(?:\\:\\d{1,5})?)" // plus option port number
				+ "(\\/(?:(?:[" + GOOD_IRI_CHAR + "\\;\\/\\?\\:\\@\\&\\=\\#\\~"  // plus option query params
				+ "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
				+ "(?:\\b|$)"; // and finally, a word boundary or end of
                        // input.  This is to stop foo.sure from
                        // matching as foo.su;
        
		Pattern linkPattern = Pattern.compile( search_pattern,  Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);
		String page_content = String.join("", page_content_in_list);
		//remove big chunk of javascript
		page_content = page_content.replaceAll("<script .*?>.*?</script>", "");
        Matcher m = linkPattern.matcher(page_content.replaceAll("\\s+", " "));
        
        String matched_content = "";
        
        while(m.find()) {
        	matched_content = m.group(0) ;
        
        	matched_content = matched_content.replaceAll("&quot;.*", "");
        	if ( matched_content.equals("hdsr3.first")) {
        		continue;
        	}
        	if ( ! matched_content.toLowerCase().startsWith("http")) {
        		continue;
        	}
        	http_urls.add(matched_content);
        }
        
        if ( matched_content.isEmpty()) {
        	matched_content = null;
        }
        else {
        	//System.out.println(matched_content);
        }
        
        TreeSet<String> sets = new TreeSet<String>(http_urls);
        List<String> lists = new ArrayList<String>(sets);
		
		return lists;
	}
	
	
	/**
	 * This method includes <script> </script> part to search http links.
	 * @param page_content_in_list
	 * @return
	 */
	public static List<String> getHttpLinksIncludeScript(List<String> page_content_in_list) {

		//Code of Patterns from https://github.com/android/platform_frameworks_base/blob/master/core/java/android/util/Patterns.java
		String GOOD_IRI_CHAR = "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";
		String IRI = "[" + GOOD_IRI_CHAR + "]([" + GOOD_IRI_CHAR + "\\-]{0,61}[" + GOOD_IRI_CHAR + "]){0,1}";
		String GOOD_GTLD_CHAR = "a-zA-Z\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";
		String GTLD = "[" + GOOD_GTLD_CHAR + "]{2,63}";
		String HOST_NAME = "(" + IRI + "\\.)+" + GTLD;
		Pattern IP_ADDRESS = Pattern.compile(
				"((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
						+ "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
						+ "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
						+ "|[1-9][0-9]|[0-9]))");
		Pattern DOMAIN_NAME = Pattern.compile("(" + HOST_NAME + "|" + IP_ADDRESS + ")");
		
		//Search pattern in joined String
		String search_pattern = "((?:(http|https|Http|Https):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
				+ "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
				+ "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
				+ "(?:" + DOMAIN_NAME + ")"
				+ "(?:\\:\\d{1,5})?)" // plus option port number
				+ "(\\/(?:(?:[" + GOOD_IRI_CHAR + "\\;\\/\\?\\:\\@\\&\\=\\#\\~"  // plus option query params
				+ "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
				+ "(?:\\b|$)"; // and finally, a word boundary or end of
                        // input.  This is to stop foo.sure from
                        // matching as foo.su;
        
		Pattern linkPattern = Pattern.compile( search_pattern,  Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);
		String page_content = String.join("", page_content_in_list);

        Matcher m = linkPattern.matcher(page_content.replaceAll("\\s+", " "));

		List<String> matched_content_list = new ArrayList<>();
		
        while(m.find()) {
        	String matched_content = m.group(0) ;

        	matched_content_list.add(matched_content);
        }
        
        if ( matched_content_list.isEmpty()) {
        	matched_content_list = null;
        }

		
		return matched_content_list;
	}
	
	
	
	/**
	 * Search http xml links
	 * Will look into <script> tags
	 */
	public static List<String> getHttpXMLLinks(List<String> page_content_in_list) {
		Set<String> http_urls = new HashSet<>();
		
		List<String> http_links = getHttpLinksIncludeScript(page_content_in_list);
		
        for(String http_link: http_links) {
        	
        	http_link = http_link.replaceAll("&quot;.*", "");

        	if ( http_link.endsWith(".xml")) {
        		http_urls.add(http_link);
        	}
        }
        
        
        TreeSet<String> sets = new TreeSet<String>(http_urls);
        List<String> lists = new ArrayList<String>(sets);
		
		return lists;
	}
	
	

	public static List<String> getHttpHTMLLinks(List<String> page_content_in_list) {
		Set<String> http_urls = new HashSet<>();
		
		List<String> http_links = getHttpLinksIncludeScript(page_content_in_list);
		
        for(String http_link: http_links) {
        	http_link = http_link.replaceAll("&quot;.*", "");

        	if ( http_link.endsWith(".html")) {
        		http_urls.add(http_link);
        	}
        }
        
        
        TreeSet<String> sets = new TreeSet<String>(http_urls);
        List<String> lists = new ArrayList<String>(sets);
		
		return lists;
	}


	public static List<String> getNeedPreserveLinks(List<String> page_content_in_list) {
		Set<String> http_urls = new HashSet<>();
		
		//List<String> http_links = getHttpLinksIncludeScript(page_content_in_list);
		List<String> http_links = getHttpLinksExcludeScript(page_content_in_list);
		
        for(String http_link: http_links) {
        	http_link = http_link.replaceAll("&quot;.*", "");

        	if ( http_link.startsWith("https://assets.pubpub.org/")) {
        		http_urls.add(http_link);
        	}
        }
        
        
        TreeSet<String> sets = new TreeSet<String>(http_urls);
        List<String> lists = new ArrayList<String>(sets);
		
		return lists;
	}



	public static String resolveURL(String url) throws IOException {
		String orgin_url = url;
		String last_url = url;
		Map<String, Integer>visited = new HashMap<>();
		String url_trace = "";
		
		String http_code = "";

		
        CookieManager manager = new CookieManager();
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(manager);
		
		while (true)
		{
			int times = visited.compute(url, (key, count) -> count == null ? 1 : count + 1);

			if (times > 3)
				throw new IOException("Stuck in redirect loop");

			URL resourceUrl = null;
			try {
				resourceUrl = new URL(url);
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			HttpURLConnection conn = null;
			try {
				conn = (HttpURLConnection) resourceUrl.openConnection();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			conn.setConnectTimeout(15000);
			conn.setReadTimeout(15000);
			conn.setInstanceFollowRedirects(false);   // Make the logic below easier to detect redirections
			//conn.setRequestProperty("User-Agent", "Mozilla/5.0...");
			conn.addRequestProperty("User-Agent", "Portico Issue Checker");
			conn.setRequestProperty("Accept","*/*");
			
			int code = 0;
			try {
				code = conn.getResponseCode();
			} 
			catch (IOException e) {
				try{
					if(e instanceof SocketTimeoutException) {
						throw new SocketTimeoutException();
					}
				} 
				catch (SocketTimeoutException f){
					throw e;
				}
				e.printStackTrace();
			}
			switch (code)	{
			case HttpURLConnection.HTTP_MOVED_PERM:
			case HttpURLConnection.HTTP_MOVED_TEMP:
			case 307:
				http_code += code + ", ";
				String location = conn.getHeaderField("Location");   ///crawlprevention/governor?content=%2fannweh%2farticle%2f45%2fsuppl_1%2fS107%2f161549%2fCrossing-the-river-stone-by-stone-approaches-for
				try {
					location = URLDecoder.decode(location, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				URL base = null;
				try {
					base = new URL(url);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}               
				URL next = null;
				try {
					next = new URL(base, location);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}  // Deal with relative URLs
				url      = next.toExternalForm();		//https://academic.oup.com/crawlprevention/governor?content=/annweh/article/45/suppl_1/S107/161549/Crossing-the-river-stone-by-stone-approaches-for

				url_trace += url + "\n";

				last_url = url;
				continue;
			case 200:
				
					url_trace += url + "\n";
					http_code += code;
					break;

			default:
				http_code += code + ", ";
				url_trace += url + "\n";
			}
			

			break;
		}
		
		http_code = http_code.replaceAll(", $", "");
		

		if ( ! url.equalsIgnoreCase(orgin_url)  ) {
			logger.info("\tRedirect to " + url );

		}
		
		return url;
	}


	public static Document parseXML(File file) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		factory.setFeature("http://xml.org/sax/features/namespaces", false);
		factory.setFeature("http://xml.org/sax/features/validation", false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		
		DocumentBuilder builder = factory.newDocumentBuilder();
		
		Document document = builder.parse( file );
		document.getDocumentElement().normalize();
		 
		return document;
	}


	/**
	 * Note: has 403 error
	 * This method download a file from a given URL and save it to input/xml/ folder
	 * @param file_url
	 * @return 
	 * @throws IOException 
	 */
	public static String downloadFileFromURLUsingStream(String file_url) throws IOException {
		
		String file_name = file_url.substring(file_url.lastIndexOf("/") + 1);
		String file_full_name = "input" + File.separator + "xml" + File.separator + file_name;
		File saveToFile = new File( file_full_name );
		
		URL url = new URL(file_url);
		
        BufferedInputStream bis = new BufferedInputStream(url.openStream());
        FileOutputStream fis = new FileOutputStream(saveToFile);
        byte[] buffer = new byte[1024];
        int count=0;
        while((count = bis.read(buffer,0,1024)) != -1)
        {
            fis.write(buffer, 0, count);
        }
        fis.close();
        bis.close();
        
        return file_full_name;
		
	}


	/**
	 * Find out the HDSR article Pub id from the Index list
	 * @param indexes
	 * @return ie xcq8a1v1
	 */
	public static String findHDSRArticlePubId(List<Index> indexes) {
		String article_pub_id = null;
		
		for(Index index: indexes) {
			if ( index.isHDSR_article_record() ) {
				article_pub_id = index.getHDSR_pub_id();
			}
		}
		
		
		return article_pub_id;
	}


	public static void extractFileOfIndex(Index index, String HDSR_article_pub_id) {
		
		long offset = index.getOffset();
		long length = index.getLength();
		String mime_type = index.getMime_type();
		String warc_file_name = index.getFilename();
		String uri = index.getUri().toString();
		String record_file_name = uri.substring( uri.lastIndexOf("/") + 1);
		
		Path warcFile = Paths.get(input_dir + File.separator + warc_subdir + File.separator + warc_file_name);
		
		try (FileChannel channel = FileChannel.open(warcFile);
                WarcReader reader = new WarcReader(channel.position(offset))) {
            Optional<WarcRecord> record = reader.next();
            if (!record.isPresent()) {
                System.err.println("No record found at position " + offset);
                System.exit(1);
            }
            WritableByteChannel out = Channels.newChannel(System.out);
            
            writeWarcHeaders(out, record.get());
            writeHttpHeaders(out, record.get());
            
            Path outputFilePath = Paths.get(output_dir + File.separator +  HDSR_article_pub_id + File.separator + record_file_name);
            
            try {
                Files.createFile(outputFilePath);
            } 
            catch (FileAlreadyExistsException ignored) {
            }
            
            out = Files.newByteChannel(outputFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            
            writePayload(out, record.get());
            
            out.close();
            
		}
		catch(Exception e) {
			logger.error( programName + ":extractFileOfIndex " + e.getMessage());
			e.printStackTrace();
		}
		
	}


	private static void writePayload(WritableByteChannel out, WarcRecord record) throws Exception  {
		
		MessageBody payload;
        List<String> contentEncodings = Collections.emptyList();
        
        if (record instanceof WarcResponse) {
            HttpResponse response = null;
			try {
				response = ((WarcResponse) record).http();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            payload = response.body();
            contentEncodings = response.headers().all("Content-Encoding");
        } 
        else if (record instanceof WarcRequest) {
            HttpRequest request = null;
			try {
				request = ((WarcRequest) record).http();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            payload = request.body();
            contentEncodings = request.headers().all("Content-Encoding");
        } 
        else {
            payload = record.body();
        }
        
        if (contentEncodings.isEmpty()) {
            try {
				writeBody(out, payload);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        } 
         else {
            if (contentEncodings.size() > 1) {
                System.err.println("Multiple Content-Encodings not supported: " + contentEncodings);
                throw new Exception("Multiple Content-Encoding not supported");
            } 
            else if (contentEncodings.get(0).equalsIgnoreCase("identity")
                    || contentEncodings.get(0).equalsIgnoreCase("none")) {
                try {
					writeBody(out, payload);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            } 
            else if (contentEncodings.get(0).equalsIgnoreCase("gzip")
                    || contentEncodings.get(0).equalsIgnoreCase("x-gzip")) {
                try {
					writeBody(out, IOUtils.gunzipChannel(payload));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            } 
            else if (contentEncodings.get(0).equalsIgnoreCase("deflate")) {
                try {
					writeBody(out, IOUtils.inflateChannel(payload));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            } 
            else {
                System.err.println("Content-Encoding not supported: " + contentEncodings.get(0));
                throw new Exception("Content-Encoding not supported");
            }
        }
	}


	private static void writeBody(WritableByteChannel out, ReadableByteChannel body) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(8192);
        while (body.read(buffer) > -1) {
            buffer.flip();
            out.write(buffer);
            buffer.compact();
        }
	}


	private static void writeHttpHeaders(WritableByteChannel out, WarcRecord record) throws IOException {
		if (record instanceof WarcResponse) {
            HttpResponse response = ((WarcResponse) record).http();
            out.write(ByteBuffer.wrap(response.serializeHeader()));
        } else if (record instanceof WarcRequest) {
            HttpRequest request = ((WarcRequest) record).http();
            out.write(ByteBuffer.wrap(request.serializeHeader()));
        }
	}


	private static void writeWarcHeaders(WritableByteChannel out, WarcRecord record) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(record.version().toString()).append("\r\n");
        record.headers().appendTo(sb);
        sb.append("\r\n");
        out.write(ByteBuffer.wrap(sb.toString().getBytes(UTF_8)));
	}


	/**
	 * 
	 * @param warcfilename		Name of the warc file
	 * @param indexes			List of Index created from warc file
	 * @param linksToPreserve	List of URLs that should be preseved in the warc file
	 * @return  "approved-complete" - all of the provided criteria are matched (all URLs present with expected status, size etc)
	 *			"not-approved" - some or all resources were not matched and at least some of the missing resources were required per (2d) of the parameter list
	 *			"approved-incomplete" - some or all resources were not matched but none of the missing items were marked as required per (2d) of the parameter list
	 */
	public static String checkWarcFileCompletenessWithList(String warcfilename, List<Index> indexes,
			List<String> linksToPreserve) {
		
		String status = null;
		List<String> report = new ArrayList<>();
		
		int failedCount = 0;
		int redirectCount = 0;
		int preservedCount = 0;
		for(int i=0; i< linksToPreserve.size(); i++) {
			
			String linkToPreserve = linksToPreserve.get(i);
			
			Index index = Utility.searchURLInIndexList( linkToPreserve, indexes );
			
			if ( index != null ) {
				boolean isArticleRecord = index.isHDSR_article_record();
				String response_code = index.getResponse_code();
				long length = index.getLength();
				long offset = index.getOffset();
				
				if ( response_code.equals("200")) {
					report.add(linkToPreserve + "\tPreserved\tat location " + offset + "\t of " + length + " bytes\n");
					preservedCount ++;
				}
				else if ( response_code.startsWith("30")) {
					report.add(linkToPreserve + "\tRedirected(" + response_code + ")\n");
					redirectCount ++;
				}
				
			}
			else {
				report.add( linkToPreserve + "\t" + "Not preserved" + "\n" );
				failedCount++;
			}
			
			
		}
		
		if ( failedCount == 0 ) {
			status = "approved-complete";
		}
		else {
			status = "not-approved";
		}
		
		System.out.println(Strings.join(report, ' '));
		
		return status;
	}


	private static Index searchURLInIndexList(String linkToPreserve, List<Index> indexes) {
		
		for(Index index: indexes) {
			String url = index.getUrl();
			
			if ( linkToPreserve.equalsIgnoreCase(url)) {
				return index;
			}
		}
		
		return null;
		
	}


	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}
