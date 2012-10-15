package test.cli.cloudify.cloud.ec2;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.cloud.NewAbstractCloudTest;
import framework.utils.AssertUtils;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;
import framework.utils.WebUtils;

public class TravelChefTest extends NewAbstractCloudTest{

	private static final String apacheUrlRestPath = "/admin/ProcessingUnits/Names/travel.apacheLB/ProcessingUnitInstances/0/ServiceDetailsByServiceId/USM/Attributes/Cloud%20Public%20IP";
	private String appName = "travel";
	private String appFolderName = "travel-chef";
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() {
//		super.teardown();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testTravelChef() throws Exception {

		String[] services = {"chef-server", "apacheLB", "mysql"};
		String restUrl = getRestUrl();
		String apachePort = "8090";
		
		installApplicationAndWait(ScriptUtils.getBuildPath() + "/recipes/apps/" + appFolderName, appName, 0);
		
		LogUtils.log("verifing successful installation");

		Client client = Client.create(new DefaultClientConfig());
		final WebResource service = client.resource(this.getRestUrl());
		
		String command = "connect " + restUrl + ";use-application " + appName + ";list-services";
		String output = CommandTestUtils.runCommandAndWait(command);
		
		for(String singleService : services){
			AssertUtils.assertTrue("the service " + singleService + " is not running", output.contains(singleService));					
		}
		
		String restApacheService = service.path(apacheUrlRestPath).get(String.class);
		Map<String, Object> apacheUrlMap = jsonToMap(restApacheService);
		String apacheServiceHostURL = (String) apacheUrlMap.get(restApacheService);
		
		assertPageExists("http://" + apacheServiceHostURL + ":" + apachePort + "/");
		
		super.uninstallApplicationAndWait(appName);

		LogUtils.log("verifing successful uninstallation");
		
		AssertUtils.assertTrue("the application is running",!WebUtils.isURLAvailable(new URL(getWebuiUrl() + "/travel")));
	}
	
	@Override
	protected void customizeCloud() throws Exception {
		// TODO Auto-generated method stub		
	}

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
	
	private void assertPageExists(String url) {

		try {
			WebUtils.isURLAvailable(new URL(url));
		} catch (Exception e) {
			AssertUtils.AssertFail(e.getMessage());
		}
	}
	
	private static Map<String, Object> jsonToMap(final String response) throws IOException {
		final JavaType javaType = TypeFactory.type(Map.class);
		ObjectMapper om = new ObjectMapper();
		return om.readValue(response, javaType);
	}

}
