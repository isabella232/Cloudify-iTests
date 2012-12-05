package test.cli.cloudify.security;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.AbstractSecuredLocalCloudTest;
import framework.tools.SGTestHelper;
import framework.utils.LogUtils;

public class LocalCloudSecurityTest extends AbstractSecuredLocalCloudTest{

	private String s = File.separator;

	protected static final String SGTEST_ROOT_DIR = SGTestHelper.getSGTestRootDir().replace('\\', '/');

	private static final String SIMPLE_APP_NAME = "simple";
	private static final String SIMPLE_APP_PATH = SGTEST_ROOT_DIR + "/src/main/resources/apps/USM/usm/applications/" + SIMPLE_APP_NAME;
	private static final String SIMPLE_SERVICE_NAME = "simple";
	private static final String SIMPLE_SERVICE_PATH = SGTEST_ROOT_DIR + "/src/main/resources/apps/USM/usm/" + SIMPLE_SERVICE_NAME;
	
	private static final String GROOVY_APP_NAME = "groovyApp";
	private static final String GROOVY_APP_PATH = SGTEST_ROOT_DIR + "/src/main/resources/apps/USM/usm/applications/" + GROOVY_APP_NAME;
	private static final String GROOVY_SERVICE_NAME = "groovy";
	private static final String GROOVY2_SERVICE_NAME = "groovy2";
	
	private static final String INSTANCE_VERIFICATION_STRING = "instance #1";
	private static final int TIMEOUT_IN_MINUTES = 30;
	private static final String ACCESS_DENIED_MESSAGE = "no_permission_access_is_denied";
	private static final String BAD_CREDENTIALS_MESSAGE = "Bad credentials";

	@AfterMethod(alwaysRun = true)
	protected void uninstall() throws Exception {

		uninstallApplicationIfFound(SIMPLE_APP_NAME, SecurityConstants.ALL_ROLES_USER_PWD, SecurityConstants.ALL_ROLES_USER_PWD);
		uninstallApplicationIfFound(GROOVY_APP_NAME, SecurityConstants.ALL_ROLES_USER_PWD, SecurityConstants.ALL_ROLES_USER_PWD);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installAndUninstallTest() throws IOException, InterruptedException {

		installAndUninstall(SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, false);
		installAndUninstall(SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD, false);
		installAndUninstall(SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, false);
		installAndUninstall(SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD, false);
		installAndUninstall(SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, true);
		installAndUninstall(SecurityConstants.NO_ROLE_USER_PWD, SecurityConstants.NO_ROLE_USER_PWD, true);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installAndUninstallWithDifferentUsersTest() throws IOException, InterruptedException {
		
		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, false, null);

		String output = uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, true, null);
		assertTrue("uninstall access granted to " + SecurityConstants.VIEWER_DESCRIPTIN, output.contains(ACCESS_DENIED_MESSAGE));

		uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD, false, null);
	
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installWithoutCredentialsTest() throws IOException, InterruptedException{

		String output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, null, null, true, null);

		assertTrue("install access granted to an Anonymous user" , output.contains(BAD_CREDENTIALS_MESSAGE));
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installingAndViewingTest() throws IOException, InterruptedException{

		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD, false, null);
		installApplicationAndWait(GROOVY_APP_PATH, GROOVY_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, false, null);

		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, SIMPLE_APP_NAME, false);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, GROOVY_APP_NAME, false);
		
		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, SIMPLE_APP_NAME, true);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, GROOVY_APP_NAME, true);
		
		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_DESCRIPTIN, SIMPLE_APP_NAME, true);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_DESCRIPTIN, GROOVY_APP_NAME, true);
		
		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SIMPLE_APP_NAME, true);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, GROOVY_APP_NAME, true);
		
		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_DESCRIPTIN, SIMPLE_APP_NAME, false);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_DESCRIPTIN, GROOVY_APP_NAME, true);
		
		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.NO_ROLE_USER_PWD, SecurityConstants.NO_ROLE_USER_PWD, SecurityConstants.NO_ROLE_DESCRIPTIN, SIMPLE_APP_NAME, false);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.NO_ROLE_USER_PWD, SecurityConstants.NO_ROLE_USER_PWD, SecurityConstants.NO_ROLE_DESCRIPTIN, GROOVY_APP_NAME, false);

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void loginTest() {

		String output = "no output";
		
		output = login(SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, false);		
		assertTrue("login failed for: " + SecurityConstants.VIEWER_DESCRIPTIN, output.contains("Logged in successfully"));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void connectWithNonexistentUserTest() {

		String output = connect(SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", SecurityConstants.CLOUD_ADMIN_USER_PWD, true);		
		assertTrue("connect succeeded for user: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains(BAD_CREDENTIALS_MESSAGE));			

	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void connectWithNoPasswordTest() {
		
		String output = connect(SecurityConstants.CLOUD_ADMIN_USER_PWD, null, true);		
		assertTrue("connect succeeded for: " + SecurityConstants.CLOUD_ADMIN_DESCRIPTIN + " without providing a password", output.contains(BAD_CREDENTIALS_MESSAGE));			
		
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void loginWithNonexistentUserTest() {

		String output = "no output";
		
		output = login(SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", SecurityConstants.CLOUD_ADMIN_USER_PWD, true);					

		assertTrue("login succeeded for user: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains(BAD_CREDENTIALS_MESSAGE));			
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void connectWithWrongPassword() {

		String output = connect(SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", true);		
		assertTrue("connect succeeded for password: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains(BAD_CREDENTIALS_MESSAGE));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void loginWithWrongPassword() {

		String output = "no output";
		
		output = login(SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", true);
		
		assertTrue("login succeeded for password: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains(BAD_CREDENTIALS_MESSAGE));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installWithWrongGroup() throws IOException, InterruptedException {
		
		String output = "no output";
		
		output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, true, "ROLE_CLOUDADMINS");
		
		assertTrue("install succeeded with authGroup ROLE_CLOUDADMINS for: " + SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, output.contains(ACCESS_DENIED_MESSAGE));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installAndUninstallWithDifferentGroup() throws IOException, InterruptedException {
		
		String output = "no output";
		
		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, false, "ROLE_CLOUDADMINS");
		output = uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, true, null);
		
		assertTrue("unseen application uninstall succeeded", output.contains("cant_find_service_for_app"));
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void securityKitchenSinkTest() throws IOException, InterruptedException {
		String output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, user, password, false, null);
		output += runCommand("connect -user " + user + " -password " + password + " " + securedRestUrl + "; use-application simple");
		
		assertTrue("Failed to change application context. Use-application command failed. output was: " + output, output.contains("Using application simple"));
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void tamperWithSecurityFileTest() throws Exception{

		String fakeCloudAdminUserAndPassword = "John";

		String originalFilePath = getBuildSecurityFilePath();
		String backupFilePath = originalFilePath + ".tempBackup";
		String fakeFilePath = SGTEST_ROOT_DIR + s + "src" + s + "main" + s + "config" + s + "security" + s + "fake-spring-security.xml";
		File originalFile = new File(originalFilePath);
		File backupFile = new File(backupFilePath);
		File fakeFile = new File(fakeFilePath);
		String output = "no output";

		LogUtils.log("moving " + originalFilePath + " to " + backupFilePath);
		FileUtils.moveFile(originalFile, backupFile);
		
		try {
			LogUtils.log("copying " + fakeFilePath + " to " + originalFilePath);
			FileUtils.copyFile(fakeFile, originalFile);
			
			output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, fakeCloudAdminUserAndPassword, fakeCloudAdminUserAndPassword, true, null);

		} 
		finally {			
			LogUtils.log("deleting " + originalFilePath);
			try{
				FileUtils.deleteQuietly(originalFile);
			}
			catch(Exception e) {
				LogUtils.log("deletion of " + originalFilePath + " failed", e);
			}
			
			LogUtils.log("moving " + backupFilePath + " to " + originalFilePath);
			try{
				FileUtils.moveFile(backupFile, originalFile);
			}
			catch(Exception e) {
				LogUtils.log("moving of " + backupFilePath + " failed", e);
			}
		}
				
		assertTrue("install access granted to viewer " + fakeCloudAdminUserAndPassword, output.contains(ACCESS_DENIED_MESSAGE));			
	}

	private void verifyVisibleLists(String installer, String viewerName, String viewerPassword, String viewerDescription, String appName, boolean isVisible) {
		
		String output = "no output";
		
		output = listApplications(viewerName, viewerPassword);
		
		if(isVisible){
			assertTrue(viewerDescription + " doesn't see the application of " + installer, output.contains(appName));
		}
		else{			
			assertTrue(viewerDescription + " sees the application of " + installer, !output.contains(appName));
		}
		
		output = listServices(viewerName, viewerPassword);
		
		if(isVisible){
			if(appName.equalsIgnoreCase(SIMPLE_APP_NAME)){					
				assertTrue(viewerDescription + " doesn't see the services of " + installer, output.contains(SIMPLE_SERVICE_NAME));
			}
			else{
				assertTrue(viewerDescription + " doesn't see the services of " + installer, output.contains(GROOVY_SERVICE_NAME) && output.contains(GROOVY2_SERVICE_NAME));
				
			}
				
		}
		else{	
			if(appName.equalsIgnoreCase(SIMPLE_APP_NAME)){	
				assertTrue(viewerDescription + " sees the services of " + installer, !output.contains(SIMPLE_SERVICE_NAME));			
			}
			else{
				assertTrue(viewerDescription + " sees the services of " + installer, !(output.contains(GROOVY_SERVICE_NAME) || output.contains(GROOVY2_SERVICE_NAME)));							
			}
		}
		
		
		if(appName.equalsIgnoreCase(SIMPLE_APP_NAME)){
			
			output = listInstances(viewerName, viewerPassword, SIMPLE_APP_NAME + "." + SIMPLE_SERVICE_NAME);
			
			if(isVisible){	
				
				assertTrue(viewerDescription + " doesn't see the instances of " + installer, output.contains(INSTANCE_VERIFICATION_STRING));
			}
			else{
				assertTrue(viewerDescription + " sees the instances of " + installer, !output.contains(INSTANCE_VERIFICATION_STRING));
				
			}
			
		}
		else{	
			
			output = listInstances(viewerName, viewerPassword, GROOVY_APP_NAME + "." + GROOVY_SERVICE_NAME);

			if(isVisible){	
				assertTrue(viewerDescription + " doesn't see the instances of " + installer, output.contains(INSTANCE_VERIFICATION_STRING));			
			}
			else{
				assertTrue(viewerDescription + " sees the instances of " + installer, !output.contains(INSTANCE_VERIFICATION_STRING));							
			}
		}
	}
	
	public void installAndUninstall(String user, String password, boolean isInstallExpectedToFail) throws IOException, InterruptedException{
		
		String output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, user, password, isInstallExpectedToFail, null);
		
		if(isInstallExpectedToFail){
			assertTrue("application installation access granted to " + user, output.contains(ACCESS_DENIED_MESSAGE));
		}
		
		if(output.contains("Application " + SIMPLE_APP_NAME + " installed successfully")){			
			uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, user, password, isInstallExpectedToFail, null);
		}
		
		listApplications(user, password);
		
		output = installServiceAndWait(SIMPLE_SERVICE_PATH, SIMPLE_SERVICE_NAME, TIMEOUT_IN_MINUTES, user, password, isInstallExpectedToFail, null);
		
		if(isInstallExpectedToFail){
			assertTrue("service installation access granted to " + user, output.contains(ACCESS_DENIED_MESSAGE));
		}
		
		if(output.contains("Service \"" + SIMPLE_SERVICE_NAME + "\" successfully installed")){			
			uninstallServiceAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, user, password, isInstallExpectedToFail, null);
		}

	}
	

}
