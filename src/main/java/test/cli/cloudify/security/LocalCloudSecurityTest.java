package test.cli.cloudify.security;

import java.io.IOException;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.AbstractSecuredLocalCloudTest;
import framework.utils.LocalCloudBootstrapper;

public class LocalCloudSecurityTest extends AbstractSecuredLocalCloudTest{
	
	LocalCloudBootstrapper bootstrapper;

	@BeforeClass
	public void bootsrap() throws Exception {
		super.bootstrap();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installAndUninstallWithDifferentUsersTest() throws IOException, InterruptedException {
		
		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, false, null);

		String output = uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, true, null);
		assertTrue("uninstall access granted to " + SecurityConstants.VIEWER_DESCRIPTIN, output.contains(ACCESS_DENIED_MESSAGE));

		uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD, false, null);
	
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
	public void installWithWrongGroup() throws IOException, InterruptedException {
		
		String output = "no output";
		
		output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, true, SecurityConstants.CLOUDADMINS_GROUP);
		
		assertTrue("install succeeded with authGroup " + SecurityConstants.CLOUDADMINS_GROUP + " for: " + SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, output.contains(ACCESS_DENIED_MESSAGE));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installAndUninstallWithDifferentGroup() throws IOException, InterruptedException {
		
		String output = "no output";
		
		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, false, SecurityConstants.CLOUDADMINS_GROUP);
		output = uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, true, null);
		
		assertTrue("unseen application uninstall succeeded", output.contains(ACCESS_DENIED_MESSAGE));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void testInstallAndUninstall() throws IOException, InterruptedException {
		super.installAndUninstallTest();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void testInstallWithoutCredentials() throws IOException, InterruptedException{
		super.installWithoutCredentialsTest();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void loginTest() throws IOException, InterruptedException {
		super.testLogin();			
	}


	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void connectWithNonexistentUserTest() throws IOException, InterruptedException {
		super.testConnectWithNonExistingUser();			
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void connectWithNoPasswordTest() throws IOException, InterruptedException {
		super.testConnectWithNoPassword();			
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void loginWithNonexistentUserTest() throws IOException, InterruptedException {
		super.testLoginWithNonexistingUser();			
	}


	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void connectWithWrongPassword() throws IOException, InterruptedException {
		super.testConnectWithWrongPassword();			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void loginWithWrongPassword() throws IOException, InterruptedException {
		super.testLoginWithWrongPassword();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void securedUseApplicationTest() throws IOException, InterruptedException {
		super.testSecuredUseApplication();
	}

	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void tamperWithSecurityFileTest() throws Exception{
		super.testTamperWithSecurityFile();			
	}
	
	@AfterMethod(alwaysRun = true)
	protected void uninstall() throws Exception {
		uninstallApplicationIfFound(SIMPLE_APP_NAME, SecurityConstants.ALL_ROLES_USER_PWD, SecurityConstants.ALL_ROLES_USER_PWD);
		uninstallApplicationIfFound(GROOVY_APP_NAME, SecurityConstants.ALL_ROLES_USER_PWD, SecurityConstants.ALL_ROLES_USER_PWD);
	}
	
	@AfterClass(alwaysRun = true)
	public void teardown() throws IOException, InterruptedException {
		super.teardown();
	}
		
}
