package org.portico.hdsr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class HDSRContentChecker {
	
	static Logger logger = LogManager.getLogger(HDSRContentChecker.class.getName());
	static Properties props = new Properties();
	final String programName = "HDSRWebChecker";
	
	
	String[] issueUrls = { "https://hdsr.mitpress.mit.edu/volume1issue1", 
							"https://hdsr.mitpress.mit.edu/volume1issue2",
							"https://hdsr.mitpress.mit.edu/volume2issue1",
							"https://hdsr.mitpress.mit.edu/volume2issue2",
							"https://hdsr.mitpress.mit.edu/volume2issue3",
							"https://hdsr.mitpress.mit.edu/volume2issue4",
							"https://hdsr.mitpress.mit.edu/specialissue1" 
						};


	public static void main(String[] args) {
		
		HDSRContentChecker checker = new HDSRContentChecker();
		
		checker.listIssuePages();
		
		//checker.checkOneIssuePage("https://hdsr.mitpress.mit.edu/volume1issue1");
		
		//checker.checkArticlePages();
		
		String article_url = "https://hdsr.mitpress.mit.edu/pub/xcq8a1v1/release/6";		//beatles
		checker.checkOneArticle(article_url);
		
		

	}


	private void checkOneArticle(String article_url) {
		
		
		HDSRArticle article = new HDSRArticle(article_url);

		//check html page
		try {
			article.checkHTMLPage();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		//check xml file
		try {
			article.checkXML();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}


	private void listIssuePages() {
		
		
	}


	private void checkArticlePages() {
		// TODO Auto-generated method stub
		
	}


	public List<String> getPreservationLinksFromArticleURL(String article_url) {
		
		HDSRArticle article = new HDSRArticle(article_url);
	
		List<String> linksToPreserve = article.getPreservationLinks();
		
		
		return linksToPreserve;
		
	}


	/**
	 * This method checks content on an issue page. List some stats data.
	 * @param issueURL 
	 */
	/*private void checkOneIssuePage(String issueURL) {
		
		//get whole issue page content back
		List<String> page_content_in_list = null;
		try {
			page_content_in_list = Utility.getWebPageContent(issueURL, false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//find <a> links
		List<String> a_tags = Utility.getATags(page_content_in_list);
		int i = 1;
		System.out.println("Total of " + a_tags.size() + " <a> links on page " + issueURL);
		
		for(String a_link_str: a_tags) {
			System.out.println(i++ + ", "  + a_link_str );
		}
		System.out.println();
		
		//find articles
		String a_pattern = "<a href=\"([^>]*)\" title=\"([^>]*)\"><h3 class=\"pub-title\">(.*?)</h3></a>";
		Pattern linkPattern = Pattern.compile( a_pattern,  Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);
        
		i = 1;
		for(String a_link_str: a_tags) {
			Matcher m = linkPattern.matcher(a_link_str);
			
			if ( m.find()) {
				String url = m.group(1);
				String title = m.group(2);
				
				System.out.println("Article " + i++ + ", "  + url + "\t" + title);
			}
		}
		System.out.println();
		
		//find meta tags (could limit to image only)
		List<String> meta_tags = Utility.getMetaTags(page_content_in_list);
		System.out.println("Total of " + meta_tags.size() + " <meta> tags on page " + issueURL);
		i = 1;
		for(String meta_tag_str: meta_tags) {
			System.out.println(i++ + ", "  + meta_tag_str );
		}
		System.out.println();
		
		//find link tags
		List<String> link_tags = Utility.getLinkTags(page_content_in_list);
		System.out.println("Total of " + link_tags.size() + " <link> tags on page " + issueURL);
		i = 1;
		for(String link_tag_str: link_tags) {
			System.out.println(i++ + ", "  + link_tag_str );
		}
		System.out.println();
		
		//find https urls (exclude the ones inside <script>
		List<String> http_links = Utility.getHttpLinks(page_content_in_list);
		System.out.println("Total of " + http_links.size() + " distinct https urls on page " + issueURL);
		i = 1;
		for(String link_tag_str: http_links) {
			System.out.println(i++ + ", "  + link_tag_str );
		}
		System.out.println();
		
		//populate HDSR articles
		i = 1;
		for(String a_link_str: a_tags) {
			Matcher m = linkPattern.matcher(a_link_str);
			
			if ( m.find()) {
				String url = m.group(1);
				String title = m.group(2);
				
				url = url.replaceAll("\\?.*$", "");
				HDSRArticle article = new HDSRArticle(url);
				article.setArticle_title(title);
				
				try {
					article.checkXML();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		
	}
*/


	
	
	
	

}
