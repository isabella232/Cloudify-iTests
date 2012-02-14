package test.cli.cloudify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import framework.utils.LogUtils;

public class AdminApiControllerTest extends AbstractLocalCloudTest {

	private static final int RECURSIVE_ITERATIONS = 5;
	protected static final String REST_ROOT = "/admin";
	private String regex = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|\\s\\[]*(]*+)";
	private Pattern pattern;
	private List<String> failedUrls; 

	@BeforeTest
	public void beforeMethod(){
		this.pattern = Pattern.compile(regex);
		failedUrls = new ArrayList<String>();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 5, groups = "1", enabled = true)
	public void testController() throws IOException{
		String htmlPage = getHtmlFromURL(this.restUrl);
		System.out.println(htmlPage);
		htmlPage = htmlPage.replace("href=\"", this.restUrl);
		System.out.println(htmlPage);
		List<String> urls = getUrlsFromHTML(htmlPage);
		assertTrue("No Urls found in main index page.", urls.size() != 0);
		recurseThroughLinks(urls, RECURSIVE_ITERATIONS);
		
		//if there were any bad urls, print them and fail test 
		if (!(failedUrls.size() == 0)){
			LogUtils.log("Failed Urls:");
			for (String urlMssage : failedUrls) {
				LogUtils.log(urlMssage);
			}
			Assert.fail("Some Urls failed. " +
					"Check logs for more detailes.");
		}

	}

	private void recurseThroughLinks(List<String> urls, int rounds){

		try{
			if (rounds > 0){

				for (String url : urls){
					String html = getHtmlFromURL(url);
					List<String> links = getUrlsFromHTML(html);
					recurseThroughLinks(links, rounds - 1);
				}
			}
		}catch(Exception e){
			LogUtils.log("FAILED " + e.getMessage());
			failedUrls.add(e.getMessage());
		}
	}


	private List<String> getUrlsFromHTML(String htmlPage) {
		String link;
		List<String> links = new ArrayList<String>();
		Matcher matcher = pattern.matcher(htmlPage);
		while (matcher.find()){
			link = matcher.group();
			if (link.contains(REST_ROOT)){
				links.add(link);
			}
		}

		return links;
	}

	private String getHtmlFromURL(String url) throws IOException{
		URL urlObject = new URL(url.replaceAll(" ", "%20"));
		BufferedReader in = new BufferedReader(new InputStreamReader(urlObject.openStream()));
		StringBuilder sb = new StringBuilder();
		String inputLine;
		while ((inputLine = in.readLine()) != null){
			sb.append(inputLine);
		}
		return sb.toString();
	}
}
