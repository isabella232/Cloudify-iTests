package test.cli.cloudify;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.security.SecurityConstants;
import framework.tools.SGTestHelper;
import framework.utils.ApplicationInstaller;

public class LocalCloudSecurityTest extends AbstractSecuredLocalCloudTest{

	private static final String SIMPLE_APP_NAME = "simple";
	private static final String SIMPLE_APP_PATH = "\\src\\main\\resources\\apps\\USM\\usm\\applications\\" + SIMPLE_APP_NAME;
	private static final String SIMPLE_SERVICE_NAME = "simple";
	
	private static final String TRAVEL_APP_NAME = "travelExtended";
	private static final String TRAVEL_APP_PATH = "\\src\\main\\resources\\apps\\USM\\usm\\applications\\" + TRAVEL_APP_NAME;
	private static final String TOMCAT_SERVICE_NAME = "tomcat-extend";
	private static final String CASSANDRA_SERVICE_NAME = "cassandra-extend";
	
	private static final int TIMEOUT_IN_MINUTES = 30;

	@AfterMethod(alwaysRun = true)
	protected void uninstall() throws Exception {

		setUserAndPassword(SecurityConstants.ALL_ROLES_USER_PWD, SecurityConstants.ALL_ROLES_USER_PWD);
		uninstallApplicationIfFound(SIMPLE_APP_NAME);
		uninstallApplicationIfFound(TRAVEL_APP_NAME);	
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installAndUninstallWithCloudAdminTest() throws IOException, InterruptedException {

		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD, false, null);
		uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD, false, null);

	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installAndUninstallWithDifferentUsersTest() throws IOException, InterruptedException {
		
		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, false, null);

		String output = uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, true, null);
		assertTrue("install access granted to " + SecurityConstants.VIEWER_USER_PWD, output.contains("no_permission_access_is_denied"));

		uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD, false, null);
	
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installWithViewerTest() throws IOException, InterruptedException{

		String output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, true, null);

		assertTrue("install access granted to " + SecurityConstants.VIEWER_USER_PWD, output.contains("no_permission_access_is_denied"));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installWithUserWithNoRolesTest() throws IOException, InterruptedException{
		
		String output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.NO_ROLE_USER_PWD, SecurityConstants.NO_ROLE_USER_PWD, true, null);
		assertTrue("install access granted to " + SecurityConstants.NO_ROLE_USER_PWD, output.contains("no_permission_access_is_denied"));
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installWithoutCredentialsTest() throws IOException, InterruptedException{

		String output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, null, null, true, null);

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

		String output = "no output";
		
		output = login(SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, false);		
		assertTrue("login failed for user: " + SecurityConstants.VIEWER_USER_PWD, output.contains("Logged in successfully"));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void connectWithNonexistentUserTest() throws IOException, InterruptedException{

		String output = connect(SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", SecurityConstants.CLOUD_ADMIN_USER_PWD);		
		assertTrue("connect succeeded for user: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains("bad credentials"));			

	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void connectWithNoPasswordTest() throws IOException, InterruptedException{
		
		String output = connect(SecurityConstants.CLOUD_ADMIN_USER_PWD, null);		
		assertTrue("connect succeeded for user: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + " without providing a password", output.contains("password is required"));			
		
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void loginWithNonexistentUserTest() throws IOException, InterruptedException{

		String output = "no output";
		
		output = login(SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", SecurityConstants.CLOUD_ADMIN_USER_PWD, true);					

		assertTrue("login succeeded for user: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains("Bad credentials"));			
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void connectWithWrongPassword() {

		String output = connect(SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad");		
		assertTrue("connect succeeded for password: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains("Access denied"));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void loginWithWrongPassword() throws IOException, InterruptedException {

		String output = "no output";
		
		output = login(SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", true);
		
		assertTrue("login succeeded for password: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains("Bad credentials"));
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
	
	protected String installApplicationAndWait(String applicationPath, String applicationName, int timeout, final String cloudifyUsername,
			final String cloudifyPassword, boolean isExpectedToFail, final String authGroups) throws IOException, InterruptedException {

		ApplicationInstaller applicationInstaller = new ApplicationInstaller(securedRestUrl, applicationName);
		applicationInstaller.recipePath(applicationPath);
		applicationInstaller.waitForFinish(true);
		applicationInstaller.cloudifyUsername(cloudifyUsername);
		applicationInstaller.cloudifyPassword(cloudifyPassword);
		applicationInstaller.expectToFail(isExpectedToFail);
		if (StringUtils.isNotBlank(authGroups)) {
			applicationInstaller.authGroups(authGroups);
		}

		return applicationInstaller.install();
	}

	protected String uninstallApplicationAndWait(String applicationPath, String applicationName, int timeout, final String cloudifyUsername,
			final String cloudifyPassword, boolean isExpectedToFail, final String authGroups) throws IOException, InterruptedException {

		ApplicationInstaller applicationInstaller = new ApplicationInstaller(securedRestUrl, applicationName);
		applicationInstaller.recipePath(applicationPath);
		applicationInstaller.waitForFinish(true);
		applicationInstaller.cloudifyUsername(cloudifyUsername);
		applicationInstaller.cloudifyPassword(cloudifyPassword);
		applicationInstaller.expectToFail(isExpectedToFail);
		if (StringUtils.isNotBlank(authGroups)) {
			applicationInstaller.authGroups(authGroups);
		}

		return applicationInstaller.uninstall();
	}
}
