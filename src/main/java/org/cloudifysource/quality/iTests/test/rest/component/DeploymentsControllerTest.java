package org.cloudifysource.quality.iTests.test.rest.component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpStatus;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.request.SetApplicationAttributesRequest;
import org.cloudifysource.dsl.rest.response.DeleteServiceInstanceAttributeResponse;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.dsl.rest.response.ServiceDetails;
import org.cloudifysource.dsl.rest.response.ServiceInstanceDetails;
import org.openspaces.admin.application.Application;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractLocalCloudTest;

import com.j_spaces.kernel.PlatformVersion;

import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.rest.CloudifyRestClient;
import org.cloudifysource.quality.iTests.framework.utils.rest.RestClientException;

public class DeploymentsControllerTest extends AbstractLocalCloudTest {

	private CloudifyRestClient client;

	@BeforeClass
	public void installTravel() throws IOException, InterruptedException {
		ApplicationInstaller installer = new ApplicationInstaller(restUrl, "helloworld");
		installer.recipePath("helloworld");
		installer.install();
		client = new CloudifyRestClient(restUrl + "/" + PlatformVersion.getVersion() + "/deployments");
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testGoodGetServiceDetails() throws Exception {
		Application app = admin.getApplications().waitFor("helloworld", OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		AssertUtils.assertTrue("Failed to discover application helloworld" , app != null);
		ServiceDetails details = client.getServiceDetails("helloworld", "tomcat");
		AssertUtils.assertEquals("helloworld", details.getApplicationName());
		AssertUtils.assertEquals("tomcat", details.getName());
		AssertUtils.assertEquals(1, details.getNumberOfInstances());
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testBadGetServiceDetails() throws Exception {
		try {
			client.getServiceDetails("helloworldBad", "tomcat");
			AssertUtils.assertFail("getServiceDetails request should have thrown an exception due to a wrong application name");
		} catch (RestClientException e) {
			AssertUtils.assertEquals(CloudifyMessageKeys.APPLICATION_WAIT_TIMEOUT.getName(), e.getMessageId());
			AssertUtils.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatus());
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testSetApplicationAttributes() throws Exception {
		SetApplicationAttributesRequest request = new SetApplicationAttributesRequest();
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("A", "ValueA");
		request.setAttributes(attributes);
		Response<Void> response = client.setApplicationAttributes("helloworld", request);
		AssertUtils.assertEquals(CloudifyMessageKeys.OPERATION_SUCCESSFULL.getName(), response.getMessageId());
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testDeleteServiceInstanceAttribute() throws Exception {
		DeleteServiceInstanceAttributeResponse response = client.deleteServiceInstanceAttribute("helloworld", "tomcat", 1, "Key");
		AssertUtils.assertEquals(response.getAttributeName(), "Key");
		AssertUtils.assertEquals(response.getAttributeLastValue(), null);
	}
	
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testGoodGetServiceInstanceDetails() throws Exception {
		Application app = admin.getApplications().waitFor("helloworld", OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		AssertUtils.assertTrue("Failed to discover application helloworld" , app != null);
		ServiceInstanceDetails details = client.getServiceInstanceDetails("helloworld", "tomcat",1);
		AssertUtils.assertEquals("helloworld", details.getApplicationName());
		AssertUtils.assertEquals("tomcat", details.getServiceName());
		AssertUtils.assertEquals(1, details.getInstanceId());
		AssertUtils.assertEquals("helloworld.tomcat", details.getServiceInstanceName());
		AssertUtils.assertEquals("localcloud", details.getHardwareId());
		AssertUtils.assertEquals("localcloud", details.getImageId());
		AssertUtils.assertEquals("localcloud", details.getMachineId());
		AssertUtils.assertEquals("127.0.0.1", details.getPrivateIp());
		AssertUtils.assertEquals("127.0.0.1", details.getPublicIp());
		AssertUtils.assertEquals("localcloud", details.getTemplateName());
		AssertUtils.assertNotNull("missing data ,ProcessDetails object is null",details.getProcessDetails());
		AssertUtils.assertEquals("127.0.0.1", details.getProcessDetails().get("Cloud Private IP"));
		AssertUtils.assertEquals("127.0.0.1", details.getProcessDetails().get("Cloud Public IP"));
		AssertUtils.assertEquals(1, details.getProcessDetails().get("Instance ID"));
	}  
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testBadGetServiceInstanceDetails() throws Exception {
		try {
			client.getServiceInstanceDetails("helloworld", "tomcat",100); // Bad instance Id
			AssertUtils.assertFail("getServiceInstanceDetails request should have thrown an exception due to a wrong instance name");
		} catch (RestClientException e) {
			AssertUtils.assertEquals(CloudifyMessageKeys.MISSING_SERVICE_INSTANCE.getName(), e.getMessageId());
			AssertUtils.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatus());
		}
	}

	@Override
	@AfterMethod(alwaysRun = true)
	public void cleanup() throws Exception {
		// dont uninstall. we need it for other tests
	}
	
	@AfterClass(alwaysRun = true)
	public void uninstall() throws Exception {
		// here is where we cleanup
		super.cleanup();
	}
}
