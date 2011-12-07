package test.cli.cloudify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.LogUtils;



public class AdminApiControllerTest extends AbstractCommandTest {

	private static final int RECURSIVE_ITERATIONS = 5;
	
	protected static final int PROCESSINGUNIT_TIMEOUT_SEC = 20;
	protected static final String REST_ROOT = "/rest/admin";
	
	
	@BeforeMethod
	public void beforeTest() {
		super.beforeTest();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 5, groups = "1", enabled = true)
	public void testController() throws IOException{
		String htmlPage = getHtmlFromURL(this.restUrl);
		System.out.println(htmlPage);
		htmlPage = htmlPage.replace("href=\"/rest", restUrl);
		System.out.println(htmlPage);
		List<String> urls = getUrlsFromHTML(htmlPage);
		recurseThroughLinks(urls, RECURSIVE_ITERATIONS);

	}

	private void recurseThroughLinks(List<String> urls, int rounds) throws IOException {

		try{
			if (rounds > 0){

				for (String url : urls){
					String html = getHtmlFromURL(url);
					//LogUtils.log("OK " + url);
					List<String> links = getUrlsFromHTML(html);
					recurseThroughLinks(links, rounds - 1);
				}
			}
		}catch(Exception e){
			LogUtils.log("FAILED " + e.getMessage());
			Assert.fail();
		}
	}


	private List<String> getUrlsFromHTML(String htmlPage) {
		String link;
		List<String> links = new ArrayList<String>();
		String regex = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
		Pattern pattern = Pattern.compile(regex);
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
		URL urlObject = new URL(url);
		BufferedReader in = new BufferedReader(new InputStreamReader(urlObject.openStream()));
		StringBuilder sb = new StringBuilder();
		String inputLine;
		while ((inputLine = in.readLine()) != null){
			sb.append(inputLine);
		}
		return sb.toString();
	}
}
