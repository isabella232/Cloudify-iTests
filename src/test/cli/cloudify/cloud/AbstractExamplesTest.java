package test.cli.cloudify.cloud;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

import test.cli.cloudify.CommandTestUtils;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import framework.utils.AssertUtils;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;
import framework.utils.WebUtils;

public abstract class AbstractExamplesTest extends NewAbstractCloudTest {
	
	private static final int WINDOWS_INSTALLATION_TIMEOUT = 50;
	private String applicationName;

	public AbstractExamplesTest() {
		LogUtils.log("Instansiated " + AbstractExamplesTest.class.getName());
	}

	protected void testTravel() throws Exception {
		doTest("travel", "travel");
	}

	protected void testPetclinic() throws Exception {
		doTest("petclinic", "petclinic");
	}
	
	protected void testTravelChef() throws Exception {
		doTest("travel-chef", "travel");
	}

	// petclinic-simple is covered by {@link ScalingRulesCloudTest}

	protected void doTest(String applicationFolderName, String applicationName)
			throws Exception {
		LogUtils.log("installing application " + applicationName + " on " + getCloudName());
		this.applicationName = applicationName;
		String applicationPath; 
		if (getCloudName().endsWith("-win")) {
			applicationPath = CommandTestUtils.getPath("apps/USM/usm/applications/" + applicationName + "-win");
		} else {
			applicationPath = ScriptUtils.getBuildPath() + "/recipes/apps/" + applicationFolderName;
		}
		try {
			if (getCloudName().endsWith("-win")) {
				installApplicationAndWait(applicationPath, applicationName, WINDOWS_INSTALLATION_TIMEOUT);
			} else {
				installApplicationAndWait(applicationPath, applicationName);
			}
			if (applicationFolderName.equals("travel") && getCloudName().equals("ec2")) {
				// verify image and hardware ID only for travel on EC2
				Client client = Client.create(new DefaultClientConfig());
				final WebResource service = client.resource(this.getRestUrl());
				String hardwareIDResult = service.path( "/admin/ProcessingUnits/Names/travel.tomcat/Instances/0/Statistics/Monitors/USM/Details/Attributes/"
										+ CloudifyConstants.USM_DETAILS_HARDWARE_ID).get(String.class);
				assertTrue("error reading hardware id", !hardwareIDResult.contains("error"));
				assertTrue("Could not find expected hardware ID: m1.small in response", hardwareIDResult.contains("m1.small"));

				String imageIDResult = service.path("/admin/ProcessingUnits/Names/travel.tomcat/Instances/0/Statistics/Monitors/USM/Details/Attributes/" 
										+ CloudifyConstants.USM_DETAILS_IMAGE_ID).get(String.class);
				assertTrue("error reading image id", !imageIDResult.toLowerCase().contains("error"));

			}
			
			if (applicationName.equals("petclinic")){
				
				String[] services = {"mongod", "mongoConfig", "mongos", "tomcat", "apacheLB"};
				
				Client client = Client.create(new DefaultClientConfig());
				final WebResource service = client.resource(this.getRestUrl());
				
				String command = "connect " + getRestUrl() + ";use-application petclinic;list-services";
				String output = CommandTestUtils.runCommandAndWait(command);
				
				for(String singleService : services){
					AssertUtils.assertTrue("the service " + singleService + " is not running", output.contains(singleService));					
				}
				
				String restApacheService = service.path("/admin/ProcessingUnits/Names/petclinic.apacheLB/ProcessingUnitInstances/0/ServiceDetailsByServiceId/USM/Attributes/Cloud%20Public%20IP").get(String.class);
				int urlStartIndex = restApacheService.indexOf(":") + 2;
				int urlEndIndex = restApacheService.indexOf("\"", urlStartIndex);
				
				String apacheServiceHostURL = restApacheService.substring(urlStartIndex, urlEndIndex);
				String apachePort = "8090";
				
				assertPageExists("http://" + apacheServiceHostURL + ":" + apachePort + "/");
				
			}
			
			if (applicationFolderName.equals("travel-chef")){
				
				String apacheUrlRestPath = "/admin/ProcessingUnits/Names/travel.apacheLB/ProcessingUnitInstances/0/ServiceDetailsByServiceId/USM/Attributes/Cloud%20Public%20IP";
				String[] services = {"chef-server", "apacheLB", "mysql"};
				String restUrl = getRestUrl();
				String apachePort = "8090";
								
				LogUtils.log("verifing successful installation");

				Client client = Client.create(new DefaultClientConfig());
				final WebResource service = client.resource(this.getRestUrl());
				
				String command = "connect " + restUrl + ";use-application " + applicationName + ";list-services";
				String output = CommandTestUtils.runCommandAndWait(command);
				
				for(String singleService : services){
					AssertUtils.assertTrue("the service " + singleService + " is not running", output.contains(singleService));					
				}
				
				String restApacheService = service.path(apacheUrlRestPath).get(String.class);
				Map<String, Object> apacheUrlMap = jsonToMap(restApacheService);
				String apacheServiceHostURL = (String) apacheUrlMap.get(restApacheService);
				
				assertPageExists("http://" + apacheServiceHostURL + ":" + apachePort + "/");
				
				super.uninstallApplicationAndWait(applicationName);

				LogUtils.log("verifing successful uninstallation");
				
				AssertUtils.assertTrue("the application is running",!WebUtils.isURLAvailable(new URL(getWebuiUrl() + "/travel")));
			}
		} 
		finally {
			if ((getService() != null) && (getService().getRestUrls() != null)) {
				String command = "connect " + getRestUrl() + ";list-applications";
				String output = CommandTestUtils.runCommandAndWait(command);
				if (output.contains(applicationName)) {
					uninstallApplicationAndWait(applicationName);
				}
			}
		}
	}

	public void uninstallApplicationIfFound() {
		if ((getService() != null) && (getService().getRestUrls() != null)) {
			String command = "connect " + getRestUrl() + ";list-applications";
			String output;
			try {
				output = CommandTestUtils.runCommandAndWait(command);
				if (output.contains(applicationName)) {
					uninstallApplicationAndWait(this.applicationName);
				}
			} catch (IOException e) {
				LogUtils.log(e.getMessage(), e);
				AssertFail(e.getMessage());
			} catch (InterruptedException e) {
				LogUtils.log(e.getMessage(), e);
				AssertFail(e.getMessage());
			}
		}
	}

	
	@Override
	protected boolean isReusableCloud() {
		return false;
	}

	@Override
	protected void customizeCloud() {
		
	}
	
	protected void assertPageExists(String url) {

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
