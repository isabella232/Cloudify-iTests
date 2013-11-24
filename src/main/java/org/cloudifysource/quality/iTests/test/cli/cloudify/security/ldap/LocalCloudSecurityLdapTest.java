package org.cloudifysource.quality.iTests.test.cli.cloudify.security.ldap;

import org.cloudifysource.quality.iTests.framework.utils.LocalCloudBootstrapper;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractSecuredLocalCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

public class LocalCloudSecurityLdapTest extends AbstractSecuredLocalCloudTest {

	private LocalCloudBootstrapper bootstrapper;
	
	@BeforeClass
	public void bootstrap() throws Exception {
		bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.secured(true).securityFilePath(SecurityConstants.LDAP_SECURITY_FILE_PATH);
		bootstrapper.keystoreFilePath(SecurityConstants.DEFAULT_KEYSTORE_FILE_PATH).keystorePassword(SecurityConstants.DEFAULT_KEYSTORE_PASSWORD);
		super.bootstrap(bootstrapper);	
	}
	

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installAndUninstallAppWithDifferentUsersTest() throws Exception {
		
		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, false, null);

		String output = uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, true, null);
		assertTrue("uninstall access granted to " + SecurityConstants.VIEWER_DESCRIPTIN, output.contains(SecurityConstants.ACCESS_DENIED_MESSAGE));

		uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, false, null);
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installAndUninstallServiceWithDifferentUsersTest() throws Exception {
		
		installServiceAndWait(SIMPLE_SERVICE_PATH, SIMPLE_SERVICE_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, false, null);

		String output = uninstallServiceAndWait(SIMPLE_SERVICE_PATH, SIMPLE_SERVICE_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, true, null);
		assertTrue("uninstall access granted to " + SecurityConstants.VIEWER_DESCRIPTIN, output.contains(SecurityConstants.ACCESS_DENIED_MESSAGE));

		uninstallServiceAndWait(SIMPLE_SERVICE_PATH, SIMPLE_SERVICE_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, false, null);
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void installingAndViewingAppTest() throws IOException, InterruptedException{

		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, false, null);
		installApplicationAndWait(GROOVY_APP_PATH, GROOVY_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, false, null);

		verifyApplicationVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, SIMPLE_APP_NAME, true);
		verifyApplicationVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, GROOVY_APP_NAME, true);
		
		verifyApplicationVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, SIMPLE_APP_NAME, false);
		verifyApplicationVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, GROOVY_APP_NAME, false);
		
		verifyApplicationVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.APP_MANAGER_DESCRIPTIN, SIMPLE_APP_NAME, true);
		verifyApplicationVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.APP_MANAGER_DESCRIPTIN, GROOVY_APP_NAME, false);
		
		verifyApplicationVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SIMPLE_APP_NAME, false);
		verifyApplicationVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, GROOVY_APP_NAME, true);
		
		verifyApplicationVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.VIEWER_DESCRIPTIN, SIMPLE_APP_NAME, true);
		verifyApplicationVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.VIEWER_DESCRIPTIN, GROOVY_APP_NAME, true);
		
		verifyApplicationVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.NO_ROLE_DESCRIPTIN, SIMPLE_APP_NAME, false);
		verifyApplicationVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.NO_ROLE_DESCRIPTIN, GROOVY_APP_NAME, false);
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installAppWithWrongGroup() throws IOException, InterruptedException {

		String output = "no output";

		output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, true, SecurityConstants.GE_GROUP);

		assertTrue("install-application succeeded with authGroup " + SecurityConstants.GE_GROUP + " for: " + SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, output.contains(SecurityConstants.ACCESS_DENIED_MESSAGE));
	}


    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, enabled = false)
    public void installingAndViewingServiceTest() throws Exception {
        // Dan (app manager) installs service simple without auth-groups (so the default is GE)
        installServiceAndWait(SIMPLE_SERVICE_PATH, SIMPLE_SERVICE_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, false, null);

        verifyServiceVisibleLists(SIMPLE_SERVICE_NAME, SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, true);
        verifyServiceVisibleLists(SIMPLE_SERVICE_NAME, SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, false);
        verifyServiceVisibleLists(SIMPLE_SERVICE_NAME, SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.APP_MANAGER_DESCRIPTIN, true);
        verifyServiceVisibleLists(SIMPLE_SERVICE_NAME, SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, false);
        verifyServiceVisibleLists(SIMPLE_SERVICE_NAME, SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.VIEWER_DESCRIPTIN, true);
        verifyServiceVisibleLists(SIMPLE_SERVICE_NAME, SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.NO_ROLE_DESCRIPTIN, false);

        uninstallServiceAndWait(SIMPLE_SERVICE_PATH, SIMPLE_SERVICE_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, false, null);

        // Don (app manager and viewer) installs service groovy without auth-groups (so the default is Cellcom)
        installServiceAndWait(GROOVY_SERVICE_PATH, GROOVY_SERVICE_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, false, null);

        verifyServiceVisibleLists(GROOVY_SERVICE_NAME, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, true);
        verifyServiceVisibleLists(GROOVY_SERVICE_NAME, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, false);
        verifyServiceVisibleLists(GROOVY_SERVICE_NAME, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.APP_MANAGER_DESCRIPTIN, false);
        verifyServiceVisibleLists(GROOVY_SERVICE_NAME, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, true);
        verifyServiceVisibleLists(GROOVY_SERVICE_NAME, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.VIEWER_DESCRIPTIN, true);
        verifyServiceVisibleLists(GROOVY_SERVICE_NAME, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.NO_ROLE_DESCRIPTIN, false);

        uninstallServiceAndWait(GROOVY_SERVICE_PATH, GROOVY_SERVICE_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, false, null);

        // Dana (cloud admin and app manager) installs service simple with auth-group = ROLE_APPMANAGERS
        installServiceAndWait(SIMPLE_SERVICE_PATH, SIMPLE_SERVICE_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, false, SecurityConstants.BEZEQ_GROUP);

        verifyServiceVisibleLists(SIMPLE_SERVICE_NAME, SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, true);
        verifyServiceVisibleLists(SIMPLE_SERVICE_NAME, SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, true);
        verifyServiceVisibleLists(SIMPLE_SERVICE_NAME, SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.APP_MANAGER_DESCRIPTIN, false);
        verifyServiceVisibleLists(SIMPLE_SERVICE_NAME, SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, false);
        verifyServiceVisibleLists(SIMPLE_SERVICE_NAME, SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.VIEWER_DESCRIPTIN, false);
        verifyServiceVisibleLists(SIMPLE_SERVICE_NAME, SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.NO_ROLE_DESCRIPTIN, false);

        uninstallServiceAndWait(SIMPLE_SERVICE_PATH, SIMPLE_SERVICE_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, false, null);
    }

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installServiceWithWrongGroup() throws IOException, InterruptedException {
		
		String output = "no output";
		
		output = installServiceAndWait(SIMPLE_SERVICE_PATH, SIMPLE_SERVICE_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, true, SecurityConstants.GE_GROUP);
		
		assertTrue("install-service succeeded with authGroup " + SecurityConstants.GE_GROUP + " for: " + SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, output.contains(SecurityConstants.ACCESS_DENIED_MESSAGE));
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installAndUninstallAppWithDifferentGroup() throws Exception {
		
		String output = "no output";
		
		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, false, SecurityConstants.GE_GROUP);
		output = uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, true, null);
		
		assertTrue("unseen application uninstall succeeded", output.contains(SecurityConstants.RESOURCE_NOT_FOUND));
	}
	
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installAndUninstallServiceWithDifferentGroup() throws Exception {
		
		String output = "no output";
		
		installServiceAndWait(SIMPLE_SERVICE_PATH, SIMPLE_SERVICE_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, false, SecurityConstants.GE_GROUP);
		output = uninstallServiceAndWait(SIMPLE_SERVICE_PATH, SIMPLE_SERVICE_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, true, null);
		
		AbstractTestSupport.assertTrue("unseen service uninstall succeeded", output.contains(SecurityConstants.MISSING_RESOURCE));
	}	
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void testInstallAndUninstall() throws Exception {
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
		uninstallApplicationIfFound(SIMPLE_APP_NAME, SecurityConstants.USER_PWD_ALL_ROLES, SecurityConstants.USER_PWD_ALL_ROLES);
		uninstallApplicationIfFound(GROOVY_APP_NAME, SecurityConstants.USER_PWD_ALL_ROLES, SecurityConstants.USER_PWD_ALL_ROLES);

		uninstallServiceIfFound(SIMPLE_SERVICE_NAME, SecurityConstants.USER_PWD_ALL_ROLES, SecurityConstants.USER_PWD_ALL_ROLES);
		uninstallServiceIfFound(GROOVY_SERVICE_NAME, SecurityConstants.USER_PWD_ALL_ROLES, SecurityConstants.USER_PWD_ALL_ROLES);
		uninstallServiceIfFound(GROOVY2_SERVICE_NAME, SecurityConstants.USER_PWD_ALL_ROLES, SecurityConstants.USER_PWD_ALL_ROLES);
	}
	
	@AfterClass(alwaysRun = true)
	public void teardown() throws IOException, InterruptedException {
		super.teardown();
	}
	
}
