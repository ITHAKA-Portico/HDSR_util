package org.portico.warc;


import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.portico.hdsr.HDSRArticle;
import org.portico.hdsr.HDSRContentChecker;
import org.portico.hdsr.Indexer;
import org.portico.hdsr.Utility;

public class WARCTool {
	

	static Logger logger = LogManager.getLogger(WARCTool.class.getName());
	final String programName = "WARCTool";
	static String divider = "+++++++++++++++++++++++++++++++++++++++++++++++";
	static String emptyline = "\n";
	
	String input_dir = "input";
	String output_dir = "output";
	String warc_subdir = "warc";
	String index_subdir = "index";
	
	String HDSR_Article_Prefix = "https://hdsr.mitpress.mit.edu/pub/";


	public static void main(String[] args) {

		WARCTool tool = new WARCTool();
		
		String browsertrix_beatles_warcfile = "browsertrix-crawler-20210211-hdsr-beatles.warc";				//xcq8a1v1
		String status = tool.checkWarcFileCompleteness(browsertrix_beatles_warcfile);
		System.out.println(browsertrix_beatles_warcfile + " is " + status);
		
		//String browsertrix_covid_india_warcfile = "browsertrix-crawler-20210211-hdsr-covid-india.warc";				//r1qq01kw
		//String status = tool.checkWarcFileCompleteness(browsertrix_covid_india_warcfile);
		//System.out.println(browsertrix_covid_india_warcfile + " is " + status);
		
		
		//String browsertrix_mitigating_bias_warcfile = "browsertrix-crawler-20210211-hdsr-mitigating-bias.warc";		//y9vc2u36
		//String status = tool.checkWarcFileCompleteness(browsertrix_mitigating_bias_warcfile);
		//System.out.println(browsertrix_mitigating_bias_warcfile + " is " + status);
		
		//String browsertrix_video_notworking_warcfile = "browsertrix-crawler-20210211-hdsr-video-notworking.warc";		//3csmghzj
		//String status = tool.checkWarcFileCompleteness(browsertrix_video_notworking_warcfile);
		//System.out.println(browsertrix_video_notworking_warcfile + " is " + status);
		
		//String browsertrix_spatiotemporal_warcfile = "browsertrix-crawler-20210211-hdsr-spatiotemporal.warc";		//qqg19a0r
		//String status = tool.checkWarcFileCompleteness(browsertrix_spatiotemporal_warcfile);
		//System.out.println(browsertrix_spatiotemporal_warcfile + " is " + status);

	}


	public String checkWarcFileCompleteness(String warcfilename) {
		
		//Create .cdx index file and a list of Index object
		List<Index> indexes = Indexer.createIndexFromWarc(warcfilename);
		
		//get article pub id from Index objects
		String HDSR_article_pub_id = Utility.findHDSRArticlePubId( indexes );
		
		//Construct an HDSRContentChecker
		HDSRContentChecker checker = new HDSRContentChecker();
		
		//Construct an HDSR article URL
		String article_url = HDSR_Article_Prefix + HDSR_article_pub_id;
		
		//save some local copies of the article
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
		
		//get preservation list from 
		//List<String> linksToPreserve = checker.getPreservationLinksFromArticleURL(article_url);
		
		List<String> linksToPreserve = article.getPreservationLinks();
		
		String warcStatus = Utility.checkWarcFileCompletenessWithList( warcfilename, indexes, linksToPreserve);
		
		
		return warcStatus;
	}

}
