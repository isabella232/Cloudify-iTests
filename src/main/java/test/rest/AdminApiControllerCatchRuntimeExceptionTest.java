package test.rest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.client.HttpResponseException;
import org.testng.annotations.Test;

import test.cli.cloudify.AbstractLocalCloudTest;
import framework.utils.AssertUtils;
import framework.utils.WebUtils;

public class AdminApiControllerCatchRuntimeExceptionTest extends AbstractLocalCloudTest {

	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 5, groups = "1", enabled = true)
	public void arrayIlegalIndexTest() throws IOException, KeyManagementException, NoSuchAlgorithmException, URISyntaxException{		
		String machinesUrl = restUrl + "/admin/Machines/Machines/10";
			// try to access a none existing url
		try {
			WebUtils.getURLContent(new URL(machinesUrl));
			AssertUtils.assertFail("Should be able to run commands on a non existing url = " + machinesUrl);
		} catch (HttpResponseException e) {
			AssertUtils.assertTrue("Http exception should be HttpNotFound", e.getMessage().contains("Not Found"));
		}
	}
}
