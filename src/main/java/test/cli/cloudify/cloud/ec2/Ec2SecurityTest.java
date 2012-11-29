package test.cli.cloudify.cloud.ec2;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.restclient.RestException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.cloud.NewAbstractSecurityCloudTest;
import test.cli.cloudify.cloud.services.ec2.SecuredEc2CloudService;
import test.cli.cloudify.security.SecuredCloudService;
import test.cli.cloudify.security.SecurityConstants;
import framework.tools.SGTestHelper;

public class Ec2SecurityTest extends NewAbstractSecurityCloudTest {

	private static final String FILE_SEP = System.getProperty("file.separator");
	private static final String SIMPLE_APP_NAME = "simple";
	private static final String SIMPLE_APP_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/" + SIMPLE_APP_NAME);
	private static final String SERVICE_NAME = "simple";
	private static final String SIMPLE_SERVICE_NAME = "simple";
	private static final String TRAVEL_APP_NAME = "travelExtended";
	private static final String TRAVEL_APP_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/" + TRAVEL_APP_NAME);
	private static final String TOMCAT_SERVICE_NAME = "tomcat-extend";
	private static final String CASSANDRA_SERVICE_NAME = "cassandra-extend";
	private static final int TIMEOUT_IN_MINUTES = 60;
	String pathToSecurityFile = SGTestHelper.getSGTestRootDir() + FILE_SEP + "config" + FILE_SEP + "security"
			+ FILE_SEP + "spring-security.xml";

	SecuredCloudService cloudService;

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		cloudService = new SecuredEc2CloudService();
		super.bootstrap(cloudService);		
	}

	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	@AfterMethod(alwaysRun = true)
	protected void uninstall() throws Exception {

		uninstallApplicationIfFound(SIMPLE_APP_NAME, SecurityConstants.ALL_ROLES_USER_PWD, SecurityConstants.ALL_ROLES_USER_PWD);
		uninstallApplicationIfFound(TRAVEL_APP_NAME, SecurityConstants.ALL_ROLES_USER_PWD, SecurityConstants.ALL_ROLES_USER_PWD);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT*4, enabled = true)
	public void installAndUninstallWithCloudAdminTest() throws IOException, InterruptedException {

		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD, false, null);
		uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD, false, null);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installAndUninstallWithDifferentUsersTest() throws IOException, InterruptedException {

		String output = "no output";
		
		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, false, null);

		output = uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, true, null);
		assertTrue("uninstall access granted to " + SecurityConstants.VIEWER_USER_PWD, output.contains("no_permission_access_is_denied"));

		uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD, false, null);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installWithViewerTest() throws IOException, InterruptedException{

		String output = "no output";

		output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, true, null);
		assertTrue("install access granted to " + SecurityConstants.VIEWER_USER_PWD, output.contains("no_permission_access_is_denied"));
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installWithUserWithNoRolesTest() throws IOException, InterruptedException{

		String output = "no output";

		output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.NO_ROLE_USER_PWD, SecurityConstants.NO_ROLE_USER_PWD, true, null);
		assertTrue("install access granted to " + SecurityConstants.NO_ROLE_USER_PWD, output.contains("no_permission_access_is_denied"));
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installWithoutCredentialsTest() throws IOException, InterruptedException{

		String output = "no output";
		output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, null, null, true, null);

		assertTrue("install access granted to an Anonymous user" , output.contains("bad_credentials"));
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installingAndViewingTest() throws IOException, InterruptedException{

		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD, false, null);

		installApplicationAndWait(TRAVEL_APP_PATH, TRAVEL_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, false, null);

		verifyVisibleLists(SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD, SIMPLE_APP_NAME, false);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD, TRAVEL_APP_NAME, false);

		verifyVisibleLists(SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, SIMPLE_APP_NAME, true);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, TRAVEL_APP_NAME, true);

		verifyVisibleLists(SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD, SIMPLE_APP_NAME, true);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD, TRAVEL_APP_NAME, true);

		verifyVisibleLists(SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SIMPLE_APP_NAME, true);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, TRAVEL_APP_NAME, true);

		verifyVisibleLists(SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, SIMPLE_APP_NAME, false);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, TRAVEL_APP_NAME, true);

		verifyVisibleLists(SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.NO_ROLE_USER_PWD, SecurityConstants.NO_ROLE_USER_PWD, SIMPLE_APP_NAME, false);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.NO_ROLE_USER_PWD, SecurityConstants.NO_ROLE_USER_PWD, TRAVEL_APP_NAME, false);

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void loginTest() throws IOException, InterruptedException{

		String output = login(SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, false);		
		assertTrue("login failed for user: " + SecurityConstants.VIEWER_USER_PWD, output.contains("Logged in successfully"));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void connectWithNonexistentUserTest() throws IOException, InterruptedException{

		String output = connect(SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", SecurityConstants.CLOUD_ADMIN_USER_PWD);		
		assertTrue("connect succeeded for user: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains("bad credentials"));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void loginWithNonexistentUserTest() throws IOException, InterruptedException{

		String output = login(SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", SecurityConstants.CLOUD_ADMIN_USER_PWD, true);		
		assertTrue("login succeeded for user: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains("Bad credentials"));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void connectWithWrongPassword() {

		String output = connect(SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad");		
		assertTrue("connect succeeded for password: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains("Access denied"));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void loginWithWrongPassword() throws IOException, InterruptedException {

		String output = login(SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", true);
		assertTrue("login succeeded for password: " + SecurityConstants.APP_MANAGER_USER_PWD + "bad", output.contains("Bad credentials"));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void TamperWithSecurityFileTest() throws IOException, InterruptedException {

		String fakeCloudAdminUserAndPassword = "John";

		String originalFilePath = getDefaultSecurityFilePath();
		String backupFilePath = originalFilePath + ".backup";
		String fakeFilePath = SGTestHelper.getSGTestRootDir() + "\\src\\main\\config\\security\\fake-spring-security.xml";
		File originalFile = new File(originalFilePath);
		File backupFile = new File(backupFilePath);
		File fakeFile = new File(fakeFilePath);

		FileUtils.moveFile(originalFile, backupFile);
		FileUtils.copyFile(fakeFile, originalFile);

		String output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, fakeCloudAdminUserAndPassword, fakeCloudAdminUserAndPassword, true, null);
		assertTrue("install access granted to " + fakeCloudAdminUserAndPassword, output.contains("no_permission_access_is_denied"));
	
		FileUtils.deleteQuietly(originalFile);
		FileUtils.moveFile(backupFile, originalFile);			

	}

	private void verifyVisibleLists(String installer, String viewerName, String viewerPassword, String appName, boolean isVisible) {

		String output = "no output";

		output = listApplications(viewerName, viewerPassword);

		if(isVisible){
			assertTrue(viewerName + " doesn't see the application of " + installer, output.contains(appName));
		}
		else{			
			assertTrue(viewerName + " sees the application of " + installer, !output.contains(appName));
		}

		output = listServices(viewerName, viewerPassword);

		if(isVisible){
			if(appName.equalsIgnoreCase(SIMPLE_APP_NAME)){					
				assertTrue(viewerName + " doesn't see the services of " + installer, output.contains(SIMPLE_SERVICE_NAME));
			}
			else{
				assertTrue(viewerName + " doesn't see the services of " + installer, output.contains(TOMCAT_SERVICE_NAME) && output.contains(CASSANDRA_SERVICE_NAME));

			}

		}
		else{	
			if(appName.equalsIgnoreCase(SIMPLE_APP_NAME)){	
				assertTrue(viewerName + " sees the services of " + installer, !output.contains(SIMPLE_SERVICE_NAME));			
			}
			else{
				assertTrue(viewerName + " sees the services of " + installer, !(output.contains(TOMCAT_SERVICE_NAME) || output.contains(CASSANDRA_SERVICE_NAME)));							
			}
		}
	}
	
	
	
	
	
	

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 20, enabled = false)
	public void testRoles() throws IOException, InterruptedException, RestException {
		doTest(SIMPLE_APP_NAME, SERVICE_NAME);
	}

	private void doTest(final String appName, final String serviceName) throws IOException, InterruptedException, RestException {

		// test each Rest server found
//		for (String restUrl : cloudService.getRestUrls()) {

//			installApplication(restUrl, SIMPLE_APP_NAME, SecurityConstants.CLOUD_ADMIN_USER_PWD,
//					SecurityConstants.CLOUD_ADMIN_USER_PWD, null/*authGroups*/);
//			String output = listApplications(restUrl, SecurityConstants.CLOUD_ADMIN_USER_PWD,
//					SecurityConstants.CLOUD_ADMIN_USER_PWD);
//			assertTrue("list-applications did not display application " + SIMPLE_APP_NAME + " for user: "
//					+ SecurityConstants.VIEWER_USER_PWD, output.contains(SIMPLE_APP_NAME));
//			uninstallApplication(restUrl, SIMPLE_APP_NAME, SecurityConstants.CLOUD_ADMIN_USER_PWD,
//					SecurityConstants.CLOUD_ADMIN_USER_PWD, null/*authGroups*/);

			/*output = listApplications(restUrl, SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD);
			assertTrue("list-applications did not display application " + APP_NAME + " for user: "
					+ SecurityConstants.APP_MANAGER_USER_PWD, output.contains("Recipe test completed"));

			output = listApplications(restUrl, SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD);
			assertTrue("list-applications did not display application " + APP_NAME + " for user: "
					+ SecurityConstants.VIEWER_USER_PWD, output.contains("Recipe test completed"));

			installApplication(restUrl, APP_NAME, SecurityConstants.CLOUD_ADMIN_USER_PWD,
					SecurityConstants.CLOUD_ADMIN_USER_PWD, "ROLE_APP_MANAGERS");
			output = listApplications(restUrl, SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD);
			assertTrue("list-applications did not display application " + APP_NAME + " for user: "
					+ SecurityConstants.CLOUD_ADMIN_USER_PWD, output.contains("Recipe test completed"));

			output = listApplications(restUrl, SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD);
			assertTrue("list-applications did not display application " + APP_NAME + " for user: "
					+ SecurityConstants.APP_MANAGER_USER_PWD, output.contains("Recipe test completed"));

			output = listApplications(restUrl, SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD);
			assertTrue("list-applications display application " + APP_NAME + " for unauthorized user: "
					+ SecurityConstants.VIEWER_USER_PWD, output.contains("Recipe test completed"));*/
//		}
	}

//	private String listApplications(final String restUrl, final String user, final String password) throws IOException,
//			InterruptedException {
//		
//		return CommandTestUtils.runCommandAndWait("connect -user " + user + " -pwd " + password + " " + restUrl 
//				+ ";list-applications");
//	}

//	private void checkPuStatus(final String restUrl, final String cloudifyUsername, final String cloudifyPassword) throws IOException, InterruptedException, RestException {
//		String absolutePUName = ServiceUtils.getAbsolutePUName(SIMPLE_APP_NAME, SERVICE_NAME);
//		GSRestClient client = new GSRestClient(cloudifyUsername, cloudifyPassword, new URL(restUrl),
//				PlatformVersion.getVersionNumber());
//		Map<String, Object> entriesJsonMap  = client.getAdminData("ProcessingUnits/Names/" + absolutePUName + "/Status");
//		String serviceStatus = (String)entriesJsonMap.get("DeclaringClass-Enumerator");
//
//		AssertUtils.assertTrue("service is not intact", serviceStatus.equalsIgnoreCase("INTACT"));	
//	}

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
}
