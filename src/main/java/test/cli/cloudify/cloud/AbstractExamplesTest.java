package test.cli.cloudify.cloud;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.testng.annotations.AfterMethod;

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
	
	@AfterMethod
	public void cleanup() throws IOException, InterruptedException {
		super.uninstallApplicationIfFound(applicationName);
		super.scanForLeakedAgentNodes();
	}
	
	
	protected void testTravel() throws Exception {
		doTest("travel", "travel");
	}

	protected void testPetclinic() throws Exception {
		doTest("petclinic", "petclinic");
	}

	protected void testPetclinicSimple() throws Exception {
		doTest("petclinic-simple", "petclinic-simple");
	}

	protected void testHelloWorld() throws Exception {
		doTest("helloworld", "helloworld");
	}

	protected void testTravelChef() throws Exception {
		doTest("travel-chef", "travel-chef");
	}
	
	protected void testComputers(String localGitAppsPath) throws Exception {
		doTest(localGitAppsPath, "computers", "computers");
	}
	
	protected void testBabies(String localGitAppsPath) throws Exception {
		doTest(localGitAppsPath, "drupal-babies", "babies");
	}
	
	protected void testBiginsights(String localGitAppsPath) throws Exception {
		doTest(localGitAppsPath, "hadoop-biginsights", "biginsights");
	}
	
	protected void testPetclinicJboss(String localGitAppsPath) throws Exception {
		doTest(localGitAppsPath, "jboss-petclinic", "petclinic-mongo");
	}
	
	protected void testLamp(String localGitAppsPath) throws Exception {
		doTest(localGitAppsPath, "lamp", "lamp");
	}
	
	protected void testMasterSlave(String localGitAppsPath) throws Exception {
		doTest(localGitAppsPath, "masterslave", "masterslave");
	}
	
	protected void testPetclinicWas(String localGitAppsPath) throws Exception {
		doTest(localGitAppsPath, "petclinic-was", "petclinic");
	}
	
	protected void testStorm(String localGitAppsPath) throws Exception {
		doTest(localGitAppsPath, "storm", "storm");
	}
	
	protected void testTravelLb(String localGitAppsPath) throws Exception {
		doTest(localGitAppsPath, "travel-lb", "travel");
	}

	// petclinic-simple is covered by {@link ScalingRulesCloudTest}

	protected void doTest(String applicationFolderName, String applicationName) throws Exception {
		doTest(null, applicationFolderName, applicationName);
	}
	
	protected void doTest(String applicationPath, String applicationFolderName, String applicationName) throws Exception {
		LogUtils.log("installing application " + applicationName + " on " + getCloudName());
		this.applicationName = applicationName;
		
		if(applicationPath == null){
			applicationPath = ScriptUtils.getBuildPath() + "/recipes/apps/" + applicationFolderName;; 			
		}
		else{
			applicationPath = applicationPath + "/" + applicationFolderName;
		}
		
		if (getCloudName().endsWith("-win")) {
			applicationPath = applicationPath + "-win";
			applicationName = applicationName + "-win";
			installApplicationAndWait(applicationPath, applicationName, WINDOWS_INSTALLATION_TIMEOUT);
		} else {
			installApplicationAndWait(applicationPath, applicationName);
		}
		if (applicationFolderName.equals("travel")) {

			String[] services = {"cassandra", "tomcat"};

			verifyServices(applicationName, services);

			verifyApplicationUrls(applicationName, false);

			// verify image and hardware ID only for travel on EC2
			if(getCloudName().equals("ec2")){

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

		}

		if (applicationFolderName.equals("petclinic")){

			String[] services = {"mongod", "mongoConfig", "mongos", "tomcat", "apacheLB"};

			verifyServices(applicationName, services);

			verifyApplicationUrls(applicationName, true);				
		}

		if (applicationFolderName.equals("petclinic-simple")){

			String[] services = {"mongod", "tomcat"};

			verifyServices(applicationName, services);

			verifyApplicationUrls(applicationName, false);				
		}

		if (applicationName.equals("helloworld")){

			String[] services = {"tomcat"};

			verifyServices(applicationName, services);

			verifyApplicationUrls(applicationName, false);				
		}

		if (applicationFolderName.equals("travel-chef")){

			String[] services = {"chef-server", "apacheLB", "mysql"};

			LogUtils.log("verifing successful installation");

			verifyServices(applicationName, services);

			verifyApplicationUrls(applicationName, true);
		}
		
		if (applicationFolderName.equals("computers")){

			String[] services = {"mysql", "apacheLB", "play"};

			verifyServices(applicationName, services);

			verifyApplicationUrls(applicationName, true);				
		}
		
		if (applicationFolderName.equals("drupal-babies")){
			
			String[] services = {"mysql", "drupal"};
			
			verifyServices(applicationName, services);
			
			verifyApplicationUrls(applicationName, false);				
		}
		
		if (applicationFolderName.equals("hadoop-biginsights")){
			
			String[] services = {"master", "data", "dataOnDemand"};
			
			verifyServices(applicationName, services);
			
			verifyApplicationUrls(applicationName, false);				
		}
		
		if (applicationFolderName.equals("jboss-petclinic")){
			
			String[] services = {"mongod", "mongoConfig", "mongos", "apacheLB", "jboss"};
			
			verifyServices(applicationName, services);
			
			verifyApplicationUrls(applicationName, true);				
		}
		
		if (applicationFolderName.equals("lamp")){
			
			String[] services = {"mysql", "apache", "apacheLB"};
			
			verifyServices(applicationName, services);
			
			verifyApplicationUrls(applicationName, true);				
		}
		
		if (applicationFolderName.equals("masterslave")){
			
			String[] services = {"mysql"};
			
			verifyServices(applicationName, services);
			
			verifyApplicationUrls(applicationName, false);				
		}
		
		if (applicationFolderName.equals("petclinic-was")){
			
			String[] services = {"mongod", "mongoConfig", "mongos", "websphere"};
			
			verifyServices(applicationName, services);
			
			verifyApplicationUrls(applicationName, false);				
		}
		
		if (applicationFolderName.equals("storm")){
			
			String[] services = {"zookeeper", "storm-nimbus", "storm-supervisor"};
			
			verifyServices(applicationName, services);
			
			verifyApplicationUrls(applicationName, false);				
		}
		
		if (applicationFolderName.equals("travel-lb")){
			
			String[] services = {"cassandra", "apacheLB", "tomcat"};
			
			verifyServices(applicationName, services);
			
			verifyApplicationUrls(applicationName, true);				
		}
		
//		uninstallApplicationAndWait(applicationName);
//		super.scanForLeakedAgentNodes();
	}

	private void verifyApplicationUrls(String appName, boolean hasApacheLB) {

		Client client = Client.create(new DefaultClientConfig());
		final WebResource service = client.resource(this.getRestUrl());

		if(hasApacheLB){

			String restApacheService = service.path("/admin/ProcessingUnits/Names/" + appName + ".apacheLB/ProcessingUnitInstances/0/ServiceDetailsByServiceId/USM/Attributes/Cloud%20Public%20IP").get(String.class);
			int urlStartIndex = restApacheService.indexOf(":") + 2;
			int urlEndIndex = restApacheService.indexOf("\"", urlStartIndex);

			String apacheServiceHostURL = restApacheService.substring(urlStartIndex, urlEndIndex);
			String apachePort = "8090";

			assertPageExists("http://" + apacheServiceHostURL + ":" + apachePort + "/");
		}
	}

	private void verifyServices(String applicationName, String[] services) throws IOException, InterruptedException {

		String command = "connect " + getRestUrl() + ";use-application " + applicationName + ";list-services";
		String output = CommandTestUtils.runCommandAndWait(command);

		for(String singleService : services){
			AssertUtils.assertTrue("the service " + singleService + " is not running", output.contains(singleService));					
		}
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

	protected void assertPageExists(String url) {

		try {
			WebUtils.isURLAvailable(new URL(url));
		} catch (Exception e) {
			AssertUtils.assertFail(e.getMessage());
		}
	}

	private static Map<String, Object> jsonToMap(final String response) throws IOException {
		final JavaType javaType = TypeFactory.type(Map.class);
		ObjectMapper om = new ObjectMapper();
		return om.readValue(response, javaType);
	}
}
