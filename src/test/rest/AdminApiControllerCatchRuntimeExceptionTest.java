package test.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.testng.annotations.Test;

import test.cli.cloudify.AbstractLocalCloudTest;

public class AdminApiControllerCatchRuntimeExceptionTest extends AbstractLocalCloudTest {

	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 5, groups = "1", enabled = true)
	public void arrayIlegalIndexTest() throws IOException{		
		String machinesUrl = restUrl + "/admin/Machines/Machines/10";
		try{
			// try to access a none existing url
			getHtmlFromURL(machinesUrl);
		}catch(Exception e){
			assertTrue("cought exception should contain 404 error" , e.getMessage().contains("404"));
		}
	}

	private String getHtmlFromURL(String urlStr) throws IOException{
		URL url = new URL(urlStr);
		URLConnection conn = url.openConnection ();

		// Get the response
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuffer sb = new StringBuffer();
		String line;
		while ((line = rd.readLine()) != null)
			sb.append(line);
		
		return sb.toString();
		}
}
