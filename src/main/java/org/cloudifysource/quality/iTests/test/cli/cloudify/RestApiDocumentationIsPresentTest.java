package org.cloudifysource.quality.iTests.test.cli.cloudify;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.framework.utils.LogUtils;

/**
 * Local cloud test to verify rest API is present.
 * @author yael
 *
 */
public class RestApiDocumentationIsPresentTest extends AbstractLocalCloudTest {
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void test() {
		final DefaultHttpClient httpClient = new DefaultHttpClient();
		String url = restUrl + "/resources/restdoclet/restdoclet.html";
		HttpGet get = new HttpGet(url);
		HttpResponse response = null;
		LogUtils.log("Validating that rest documentation exists - sending get request to " + url);
		try {
			response = httpClient.execute(get);
		} catch (IOException e) {
			AbstractTestSupport.AssertFail("Failed to send request to " + url, e);
		}
		AbstractTestSupport.assertNotNull(response);
		AbstractTestSupport.assertNotNull(response.getStatusLine());
		AbstractTestSupport.assertEquals(200, response.getStatusLine().getStatusCode());
	}
}
