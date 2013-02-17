package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import java.io.IOException;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractSecurityCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudService;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudServiceManager;
import org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants;
import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;

public class Ec2CustomSecurityFileTest extends NewAbstractSecurityCloudTest{

	private static final String CLOUD_ADMIN_USER_AND_PASSWORD = "John"; 
	private static final String VIEWER_USER_AND_PASSWORD = "Amanda"; 
	private static final String VIEWER_DESCRIPTIN = VIEWER_USER_AND_PASSWORD + " (viewer)";
	private static final String APP_NAME = "simple";
	private static final String CUSTUM_SECURITY_FILE_PATH = CommandTestUtils.getPath("/src/main/config/security/custom-spring-security.xml");
	private static final String APP_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/applications/" + APP_NAME);
	
	private static final String ACCESS_DENIED_MESSAGE = "no_permission_access_is_denied";
	
	private static final int TIMEOUT_IN_MINUTES = 60;
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		CloudService service = CloudServiceManager.getInstance().getCloudService(getCloudName());
		CloudBootstrapper bootstrapper = new CloudBootstrapper();
		bootstrapper.secured(true).securityFilePath(CUSTUM_SECURITY_FILE_PATH);
		bootstrapper.keystoreFilePath(SecurityConstants.DEFAULT_KEYSTORE_FILE_PATH).keystorePassword(SecurityConstants.DEFAULT_KEYSTORE_PASSWORD);
		bootstrapper.user(SecurityConstants.USER_PWD_ALL_ROLES).password(SecurityConstants.USER_PWD_ALL_ROLES);
		service.setBootstrapper(bootstrapper);
		super.bootstrap(service);	
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	@AfterMethod(alwaysRun = true)
	protected void uninstall() throws Exception {

		uninstallApplicationIfFound(APP_NAME, SecurityConstants.USER_PWD_ALL_ROLES, SecurityConstants.USER_PWD_ALL_ROLES);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installWithcustomCloudAdminTest() throws IOException, InterruptedException {
		
		installApplicationAndWait(APP_PATH, APP_NAME, TIMEOUT_IN_MINUTES, CLOUD_ADMIN_USER_AND_PASSWORD, CLOUD_ADMIN_USER_AND_PASSWORD, false, null);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installWithCustomViewerTest() throws IOException, InterruptedException{
		
		String output = installApplicationAndWait(APP_PATH, APP_NAME, TIMEOUT_IN_MINUTES, VIEWER_USER_AND_PASSWORD, VIEWER_USER_AND_PASSWORD, true, null);
		
		assertTrue("install access granted to " + VIEWER_DESCRIPTIN, output.contains(ACCESS_DENIED_MESSAGE));
	}
	
	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

}
