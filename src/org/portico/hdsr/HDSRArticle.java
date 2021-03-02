package org.portico.hdsr;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.apache.commons.io.FileUtils;

public class HDSRArticle {


	static Logger logger = LogManager.getLogger(HDSRArticle.class.getName());
	static Properties props = new Properties();
	final String programName = "HDSRArticle";
	static String divider = "+++++++++++++++++++++++++++++++++++++++++++++++";
	static String emptyline = "\n";
	
	String DOI;							// ie 10.1162/99608f92.130f856e
	String issue_title;
	String pub_id;						//xcq8a1v1
	String current_release_no;			//6
	String url;							//ie "https://hdsr.mitpress.mit.edu/pub/xcq8a1v1"
	String resolved_url;				//ie. "https://hdsr.mitpress.mit.edu/pub/xcq8a1v1/release/6"
	
	String article_title;				//ie. (A) Data in the Life: Authorship Attribution in Lennon-McCartney Songs
	String article_authors;				//ie. Mark Glickman, Jason Brown, and Ryan Song
	
	String pub_date;					//Jul 02, 2019
	
	String section_name;
	int seq_on_page;
	
	String stats_file_name;				//File to save stats data, ie output/stats/xcq8a1v1_release_6_stats.txt
	
	String jats_xml_url;				//jats xml file url, ie https://assets.pubpub.org/vvalarda/627fc51d-ba40-4fe9-b17f-5b97e0f833d2.xml
	String jats_file_name;				//ie 627fc51d-ba40-4fe9-b17f-5b97e0f833d2.xml
	String xml_file_name;				//jats_xml_url saved as local copy, ie input/xml/xcq8a1v1_release_6.xml, copy of https://assets.pubpub.org/vvalarda/627fc51d-ba40-4fe9-b17f-5b97e0f833d2.xml
	
	String html_file_url;				//https://assets.pubpub.org/lhvheqbk/130f856e-38db-4586-a498-4debbfe370b1.html
	String html_file_name;				//html url saved as local copy, ie input/html/xcq8a1v1_release_6.html
	
	List<String> linksToPreserveFromHTML;
	List<String> linksToPreserveFromXML;
	
	public HDSRArticle(String article_url) {
		this.url = article_url;
		
		if ( article_url.startsWith("https://hdsr.mitpress.mit.edu/pub/")) {
			Pattern p = Pattern.compile("https://hdsr.mitpress.mit.edu/pub/([^/]*)(/release/(\\d*))*");
			Matcher m = p.matcher(article_url);
			
			if ( m.find()) {
				String id = m.group(1);
				String release_no = m.group(3);
				
				setPub_id(id);
				setCurrent_release_no(release_no);

			}
			else {
				logger.error( programName + ":new HDSRArticle :could not find pub_id");
			}
		}
	}
	
	
	
	/**
	 * 
	 */
	public void checkHTMLPage() throws Exception {
		
		String article_url = getUrl();
		
		logger.info(divider);
		logger.info(programName + ":checkHTMLPage Check " + article_url);
		
		List<String> stats = new ArrayList<>();
		
		//resolve url and find release no
		String redirect_url = article_url;
		try {
			redirect_url = Utility.resolveURL(article_url);
			setResolved_url(redirect_url);
			logger.info( programName + ":checkHTMLPage " + article_url + " resolved to " + redirect_url);
			
			if ( redirect_url.indexOf("release") != -1 && getCurrent_release_no() == null ) {
				String release_no = redirect_url.replaceAll(".*/release/", "");
				setCurrent_release_no(release_no);
				logger.info( programName + ":checkHTMLPage found release no: " + release_no);
			}
			
		} catch (IOException e1) {
			logger.error( programName + ":checkHTMLPage " + article_url + " " + e1.getMessage());
			e1.printStackTrace();
		}
		
		String html_file_name = "input" + File.separator + "html" + File.separator + getPub_id() + ".html";	//default name without release no
		String stats_file_name = "output" + File.separator + "stats" + File.separator + getPub_id() + "_stats.txt";

		if ( getCurrent_release_no() != null ) {
			html_file_name = "input" + File.separator + "html" + File.separator + getPub_id() + "_release_" + getCurrent_release_no() + ".html";
			stats_file_name = "output" + File.separator + "stats" + File.separator + getPub_id() + "_release_" + getCurrent_release_no() + "_stats.txt";
		}
		
		setHtml_file_name(html_file_name);
		setStats_file_name(stats_file_name);
		
		
		logger.info( programName + ":checkHTMLPage reading html content" + redirect_url);
		//get whole article page content back
		List<String> page_content_in_list = null;
		try {
			page_content_in_list = Utility.getWebPageContent(redirect_url, false);
		} catch (IOException e) {
			logger.error( programName + ":checkHTMLPage :Error getting html page content " + redirect_url + " " + e.getMessage());
			e.printStackTrace();
			throw new Exception("Error reading html content");
		}
		
		//save html content to local directory
		logger.info( programName + ":checkHTMLPage saving html content to " + html_file_name);
		try {
			Files.write(Paths.get(html_file_name), page_content_in_list, Charset.forName("UTF-8"));
		} catch (IOException e) {
			logger.error( programName + ":checkHTMLPage: Error writing to " + html_file_name + " " + e.getMessage());
			e.printStackTrace();
		}
		

		stats.add(divider);
		stats.add("Check article html page " + redirect_url);
		stats.add(divider);
		
		//find <a> links
		List<String> a_tags = Utility.getATags(page_content_in_list);
		int i = 1;
		stats.add("Total of " + a_tags.size() + " <a> links on page " + redirect_url);

		for(String a_link_str: a_tags) {
			stats.add(i++ + ", "  + a_link_str );
		}
		stats.add(emptyline);

		//find meta tags (could limit to image only)
		List<String> meta_tags = Utility.getMetaTags(page_content_in_list);
		stats.add("Total of " + meta_tags.size() + " <meta> tags on page " + redirect_url);
		i = 1;
		for(String meta_tag_str: meta_tags) {
			stats.add(i++ + ", "  + meta_tag_str );
		}
		stats.add(emptyline);

		//find link tags
		List<String> link_tags = Utility.getLinkTags(page_content_in_list);
		stats.add("Total of " + link_tags.size() + " <link> tags on page " + redirect_url);
		i = 1;
		for(String link_tag_str: link_tags) {
			stats.add(i++ + ", "  + link_tag_str );
		}
		stats.add(emptyline);

		//find https urls (exclude the ones inside <script>
		List<String> http_links = Utility.getHttpLinksExcludeScript(page_content_in_list);
		stats.add("Total of " + http_links.size() + " distinct https urls on page " + redirect_url);
		i = 1;
		for(String link_tag_str: http_links) {
			stats.add(i++ + ", "  + link_tag_str );
		}
		stats.add(emptyline);
		
		//find https urls ends with .xml
		List<String> http_xml_links = Utility.getHttpXMLLinks(page_content_in_list);
		stats.add("Total of " + http_xml_links.size() + " distinct xml https urls on page " + redirect_url);
		i = 1;
		for(String link_tag_str: http_xml_links) {
			stats.add(i++ + ", "  + link_tag_str );
			
			if ( link_tag_str.indexOf("assets.pubpub.org") != -1 && link_tag_str.length()> 40 ) {
				logger.info( programName + ":checkHTMLPage set xml url " + link_tag_str );
				setJats_file_url(link_tag_str);
			}
			
		}
		stats.add(emptyline);
		
		//find https urls ends with .html
		List<String> http_html_links = Utility.getHttpHTMLLinks(page_content_in_list);
		stats.add("Total of " + http_html_links.size() + " distinct html https urls on page " + redirect_url);
		i = 1;
		for(String link_tag_str: http_html_links) {
			stats.add(i++ + ", "  + link_tag_str );
			
			if ( link_tag_str.indexOf("assets.pubpub.org") != -1 && link_tag_str.length()> 40 ) {
				logger.info( programName + ":checkHTMLPage set html url " + link_tag_str );
				setHtml_file_url(link_tag_str);
			}
		}
		stats.add(emptyline);
		
		//find all links that should be reserved
		List<String> preserve_links = Utility.getNeedPreserveLinks(page_content_in_list);
		stats.add("Total of " + preserve_links.size() + " links should be preserved on page " + redirect_url);
		i = 1;
		for(String link_tag_str: preserve_links) {
			stats.add(i++ + ", "  + link_tag_str );

		}
		stats.add(emptyline);
		
		
		//find DOI <meta name="citation_doi" content="doi:10.1162/99608f92.ba20f892"/>
		String doi_pattern_str = "<meta name=\"citation_doi\" content=\"doi:([^\"]*)\"/>";
		Pattern doiPattern = Pattern.compile( doi_pattern_str,  Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);
		
		String page_content = String.join("", page_content_in_list);
		Matcher m = doiPattern.matcher(page_content);

		if ( m.find()) {
			String doi = m.group(1);
			logger.info( programName + ":checkHTMLPage Found DOI:" + doi);
			setDOI(doi);
		}
		else {
			logger.error(programName + ":checkHTMLPage Can't find DOI on page " + redirect_url);
		}

		
		//find title
		String title_pattern_str = "<h1 class=\"title\"><span class=\"text-wrapper\">([^<]*)</span></h1>";
		Pattern titlePattern = Pattern.compile( title_pattern_str,  Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);

		m = titlePattern.matcher(page_content);

		String title = "";
		if ( m.find()) {
			String all = m.group(0);
			title = m.group(1);
			logger.info( programName + ":checkHTMLPage Found title:" + title);
			setArticle_title(title);
		}
		else {
			logger.error( programName + ":checkHTMLPage Can't find author on page " + redirect_url);
		}

		
		//find authors
		//<span>by<!-- --> </span><span><a href="/user/xiao-li-meng" class="hoverline">Xiao-Li Meng</a></span></span></div>
		//<span>by<!-- --> </span><span><a href="/user/laura-haas" class="hoverline">Laura Haas</a></span>, <span><a href="/user/alfred-hero" class="hoverline">Alfred Hero</a></span>, and <span><a href="/user/robert-lue" class="hoverline">Robert A. Lue</a></span></span></div>
		//String author_pattern_str = "<meta name=\"citation_author\" content=\"([^\"]*)\"/>";
		String authors_pattern_str = "<span>by<!-- --> </span><span><a href=\"[^\"]*\" class=\"hoverline\">([^<]*)</a></span>(, (and )*<span><a href=\"[^\"]*\" class=\"hoverline\">([^<]*)</a></span>)*</span></div>";
		Pattern authorPattern = Pattern.compile( authors_pattern_str,  Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);

		m = authorPattern.matcher(page_content);

		String authors = "";
		if ( m.find()) {
			String all = m.group(0);
			authors = all.replace("by<!-- --> ", "").replaceAll("</*[^>]*>", "");
			logger.info( programName + ":checkHTMLPage Found author:" + authors);
			setArticle_authors(authors);
		}
		else {
			logger.error( programName + ":checkHTMLPage Can't find author on page " + redirect_url);
		}

		
		//find pub date  
		//<div class="published-date"><span class="pub-header-themed-secondary">Published on</span><span>Jul 01, 2019</span></div>
		String date_pattern_str = "<div class=\"published-date\"><span class=\"pub-header-themed-secondary\">Published on</span><span>([^<]*)</span></div>";
		Pattern datePattern = Pattern.compile( date_pattern_str,  Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);

		m = datePattern.matcher(page_content);

		if ( m.find()) {
			String date = m.group(1);
			logger.info(programName + ":checkHTMLPage Found pub date:" + date);
			setPub_date(date);
		}
		else {
			logger.error( programName + ":checkHTMLPage Can't find publish date on page " + redirect_url);
		}

		
		//find section
		
		
		//write to stats file
		stats.addAll(printArticleInfo());
		try {
			Files.write(Paths.get(stats_file_name), stats, Charset.forName("UTF-8"));
		} catch (IOException e) {
			logger.error( programName + ":checkHTMLPage: Error writing to " + stats_file_name + " " + e.getMessage());
			e.printStackTrace();
		}
		
		
		logger.info(divider);
		
	}
	
	


	public void checkXML() throws Exception  {
		
		String jats_file_url = getJats_file_url();
		
		logger.info(divider);
		logger.info( programName + ":checkXML Check article xml file " + jats_file_url);
		logger.info(divider);
		
		if ( jats_file_url == null ) {
			logger.error( programName + ":checkXML: Xml file url has not been set for article " + getUrl());
			throw new Exception("No xml file");
		}
		
		//download xml to local, save to xml_file_name
		String xml_file_name = null;
		try {
			xml_file_name = downloadXmlFile(jats_file_url);
			setXml_file_name(xml_file_name);
			logger.info( programName + ":checkXML Save " + jats_file_url + " to " + xml_file_name);
		} catch (Exception e1) {
			logger.error( programName + ":checkXML error downling xml file from " + jats_file_url + " " + e1.getMessage());
			e1.printStackTrace();
			throw new Exception("Error downloading xml");
		}

		String stats_file_name = getStats_file_name();
		List<String> stats = new ArrayList<>();
		//parse xml file
		Document doc;
		try {
			doc = Utility.parseXML( new File(xml_file_name) );
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new Exception( xml_file_name + " cannot be parsed. " );
		}

		Element root = doc.getDocumentElement();

		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList nodes = (NodeList)xPath.evaluate("//@*[name()='xlink:href']", doc, XPathConstants.NODESET);
		
		stats.add("Totoal " + nodes.getLength() + " xlink:href used in " + xml_file_name );
		for (int i = 0; i < nodes.getLength(); i++) {
		      Node node = nodes.item(i);
		      
		      stats.add((i+1) + ", " + node.getNodeName()  + " " + node.getTextContent());
		}
		
		try {
			Files.write(Paths.get(stats_file_name), stats, Charset.forName("UTF-8"), StandardOpenOption.APPEND);
		} catch (IOException e) {
			logger.error( programName + ":checkXML: Error writing to " + stats_file_name + " " + e.getMessage());
			e.printStackTrace();
		}
		
		logger.info(divider);
	}


	public List<String> getPreservationLinks() {
		
		List<String> linksToPreserve = new ArrayList<>();
		
		//For now, use HTML page
		try {
			linksToPreserve = getPreservationLinksFromHtmlPage();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//When JATS XML is ready, use xml file to get preservation links
		/*try {
			linksToPreserve = getPreservationLinksFromXmlFile();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		return linksToPreserve;
		
	}

	

	private List<String> getPreservationLinksFromXmlFile() throws Exception {
		List<String> linksToPreserve = new ArrayList<>();
		
		String jats_file_url = getJats_file_url();
		
		logger.info(divider);
		logger.info( programName + ":getPreservationLinksFromXmlFile Check article xml file " + jats_file_url);
		logger.info(divider);
		
		if ( jats_file_url == null ) {
			logger.error( programName + ":getPreservationLinksFromXmlFile: Xml file url has not been set for article " + getUrl());
			throw new Exception("No xml file");
		}
		
		//download xml to local, save to xml_file_name
		String xml_file_name = null;
		try {
			xml_file_name = downloadXmlFile(jats_file_url);
			setXml_file_name(xml_file_name);
			logger.info( programName + ":getPreservationLinksFromXmlFile Save " + jats_file_url + " to " + xml_file_name);
		} catch (Exception e1) {
			logger.error( programName + ":getPreservationLinksFromXmlFile error downling xml file from " + jats_file_url + " " + e1.getMessage());
			e1.printStackTrace();
			throw new Exception("Error downloading xml");
		}


		//parse xml file
		Document doc;
		try {
			doc = Utility.parseXML( new File(xml_file_name) );
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new Exception( xml_file_name + " cannot be parsed. " );
		}

		Element root = doc.getDocumentElement();

		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList nodes = (NodeList)xPath.evaluate("//@*[name()='xlink:href']", doc, XPathConstants.NODESET);
		
		for (int i = 0; i < nodes.getLength(); i++) {
		      Node node = nodes.item(i);
		      
		      String link_text = node.getTextContent();
		      
		      if ( link_text.startsWith("https://assets.pubpub.org/")) {
		    	  linksToPreserve.add(link_text);
		      }

		}
		
		setLinksToPreserveFromXML(linksToPreserve);
		
		logger.info(divider);
		
		return linksToPreserve;
	}


	/**
	 * Save html content to local input/html directory. (This may not be necessary)
	 * Get preservation links from html page.
	 */
	public List<String> getPreservationLinksFromHtmlPage() throws Exception {
		
		
		String article_url = getUrl();
		
		logger.info(divider);
		logger.info(programName + ":getPreservationLinksFromHtmlPage Check " + article_url);
		
		//resolve url and find release no
		String redirect_url = article_url;
		try {
			redirect_url = Utility.resolveURL(article_url);
			setResolved_url(redirect_url);
			logger.info( programName + ":getPreservationLinksFromHtmlPage " + article_url + " resolved to " + redirect_url);
			
			if ( redirect_url.indexOf("release") != -1 && getCurrent_release_no() == null ) {
				String release_no = redirect_url.replaceAll(".*/release/", "");
				setCurrent_release_no(release_no);
				logger.info( programName + ":getPreservationLinksFromHtmlPage found release no: " + release_no);
			}
			
		} catch (IOException e1) {
			logger.error( programName + ":getPreservationLinksFromHtmlPage " + article_url + " " + e1.getMessage());
			e1.printStackTrace();
		}
		
		String html_file_name = "input" + File.separator + "html" + File.separator + getPub_id() + ".html";	//default name without release no

		if ( getCurrent_release_no() != null ) {
			html_file_name = "input" + File.separator + "html" + File.separator + getPub_id() + "_release_" + getCurrent_release_no() + ".html";
		}
		
		setHtml_file_name(html_file_name);
		
		logger.info( programName + ":getPreservationLinksFromHtmlPage reading html content" + redirect_url);
		//get whole article page content back
		List<String> page_content_in_list = null;
		try {
			page_content_in_list = Utility.getWebPageContent(redirect_url, false);
		} catch (IOException e) {
			logger.error( programName + ":getPreservationLinksFromHtmlPage :Error getting html page content " + redirect_url + " " + e.getMessage());
			e.printStackTrace();
			throw new Exception("Error reading html content");
		}

		//save html content to local directory
		logger.info( programName + ":getPreservationLinksFromHtmlPage saving html content to " + html_file_name);
		try {
			Files.write(Paths.get(html_file_name), page_content_in_list, Charset.forName("UTF-8"));
		} catch (IOException e) {
			logger.error( programName + ":getPreservationLinksFromHtmlPage: Error writing to " + html_file_name + " " + e.getMessage());
			e.printStackTrace();
		}
		
		//find all links that should be reserved
		List<String> linksToPreserve = Utility.getNeedPreserveLinks(page_content_in_list);
		
		return linksToPreserve;
		
	}



	private String downloadXmlFile(String xml_file_url) throws Exception {
		
		String jats_file_url = getJats_file_url();
		String pub_id = getPub_id();
		String release_no = getCurrent_release_no();
		
		
		String jats_file_name = jats_file_url.substring(jats_file_url.lastIndexOf("/") + 1);
		String xml_file_name = "input" + File.separator + "xml" + File.separator + pub_id ;
		if ( release_no != null ) {
			xml_file_name += "_release_" + release_no + ".xml";
		}
		else {
			xml_file_name += ".xml";
		}

		
		List<String> page_content_in_list = null;
		try {
			page_content_in_list = Utility.getWebPageContent(xml_file_url, false);
		} catch (IOException e) {
			logger.error( programName + ":downloadXmlFile Error reading from" + xml_file_url + " " + e.getMessage());
			e.printStackTrace();
			throw new Exception("Error read from xml file");
		}
		
		try {
			Files.write(Paths.get(xml_file_name), page_content_in_list, Charset.forName("UTF-8"));
		} catch (IOException e) {
			logger.error( programName + ":downloadXmlFile: Error writing to " + xml_file_name + " " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
		
		return xml_file_name;
	}



	private List<String> printArticleInfo() {
		
		List<String> info = new ArrayList<>();
		
		info.add("Article url:\t\t" + getUrl());
		info.add("Article redirect url:\t" + getResolved_url());
		info.add("Article title:\t\t" + getArticle_title());
		info.add("Article author(s):\t" + getArticle_authors());
		info.add("Article pub date:\t" + getPub_date());
		info.add("Article DOI:\t\t" + getDOI());
		info.add("");
		
		for(String line: info) {
			System.out.println(line);
		}
		
		return info;
		
		
	}



	public String getDOI() {
		return DOI;
	}

	public void setDOI(String dOI) {
		DOI = dOI;
	}

	public String getIssue_title() {
		return issue_title;
	}

	public void setIssue_title(String issue_title) {
		this.issue_title = issue_title;
	}

	public String getPub_id() {
		return pub_id;
	}

	public void setPub_id(String pub_id) {
		this.pub_id = pub_id;
	}

	public String getCurrent_release_no() {
		return current_release_no;
	}

	public void setCurrent_release_no(String current_release_no) {
		this.current_release_no = current_release_no;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getResolved_url() {
		return resolved_url;
	}

	public void setResolved_url(String resolved_url) {
		this.resolved_url = resolved_url;
	}

	public String getArticle_title() {
		return article_title;
	}

	public void setArticle_title(String article_title) {
		this.article_title = article_title;
	}

	public String getArticle_authors() {
		return article_authors;
	}

	public void setArticle_authors(String article_authors) {
		this.article_authors = article_authors;
	}

	public String getPub_date() {
		return pub_date;
	}

	public void setPub_date(String pub_date) {
		this.pub_date = pub_date;
	}

	public String getSection_name() {
		return section_name;
	}

	public void setSection_name(String section_name) {
		this.section_name = section_name;
	}

	public int getSeq_on_page() {
		return seq_on_page;
	}

	public void setSeq_on_page(int seq_on_page) {
		this.seq_on_page = seq_on_page;
	}



	public String getXml_file_name() {
		return xml_file_name;
	}



	public void setXml_file_name(String xml_file_name) {
		this.xml_file_name = xml_file_name;
	}


	public String getStats_file_name() {
		return stats_file_name;
	}



	public void setStats_file_name(String stats_file_name) {
		this.stats_file_name = stats_file_name;
	}



	public String getHtml_file_name() {
		return html_file_name;
	}



	public void setHtml_file_name(String html_file_name) {
		this.html_file_name = html_file_name;
	}



	public String getJats_file_url() {
		return jats_xml_url;
	}



	public void setJats_file_url(String jats_file_url) {
		this.jats_xml_url = jats_file_url;
	}



	public String getJats_file_name() {
		return jats_file_name;
	}



	public void setJats_file_name(String jats_file_name) {
		this.jats_file_name = jats_file_name;
	}



	public String getJats_xml_url() {
		return jats_xml_url;
	}



	public void setJats_xml_url(String jats_xml_url) {
		this.jats_xml_url = jats_xml_url;
	}



	public String getHtml_file_url() {
		return html_file_url;
	}



	public void setHtml_file_url(String html_file_url) {
		this.html_file_url = html_file_url;
	}



	public List<String> getLinksToPreserveFromHTML() {
		return linksToPreserveFromHTML;
	}



	public void setLinksToPreserveFromHTML(List<String> linksToPreserveFromHTML) {
		this.linksToPreserveFromHTML = linksToPreserveFromHTML;
	}



	public List<String> getLinksToPreserveFromXML() {
		return linksToPreserveFromXML;
	}



	public void setLinksToPreserveFromXML(List<String> linksToPreserveFromXML) {
		this.linksToPreserveFromXML = linksToPreserveFromXML;
	}




	
}
