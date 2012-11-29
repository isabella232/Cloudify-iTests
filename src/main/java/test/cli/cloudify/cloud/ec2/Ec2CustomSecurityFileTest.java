package test.cli.cloudify.cloud.ec2;

import java.io.IOException;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.NewAbstractSecurityCloudTest;
import test.cli.cloudify.cloud.services.ec2.SecuredEc2CloudService;
import test.cli.cloudify.security.SecurityConstants;
import framework.tools.SGTestHelper;
import framework.utils.CloudBootstrapper;

public class Ec2CustomSecurityFileTest extends NewAbstractSecurityCloudTest{

	private static final String SGTEST_ROOT_DIR = SGTestHelper.getSGTestRootDir().replace('\\', '/');
	private static final String CLOUD_ADMIN_USER_AND_PASSWORD = "John"; 
	private static final String VIEWER_USER_AND_PASSWORD = "Amanda"; 
	private static final String APP_NAME = "simple";
	private static final String CUSTUM_SECURITY_FILE_PATH = SGTEST_ROOT_DIR + "/src/main/config/security/spring-security.xml";
	private static final String APP_PATH = SGTEST_ROOT_DIR + "/src/main/resources/apps/USM/usm/applications/" + APP_NAME;
	
	private static final int TIMEOUT_IN_MINUTES = 60;
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		CloudBootstrapper bootstrapper = new CloudBootstrapper();
		bootstrapper.securityFilePath(CUSTUM_SECURITY_FILE_PATH);
		bootstrapper.keystoreFilePath(getDefaultKeystoreFilePath()).keystorePassword(getDefaultKeystorePassword());

		cloudService = new SecuredEc2CloudService();
		cloudService.setBootstrapper(bootstrapper);
		super.bootstrap(cloudService);	
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	@AfterMethod(alwaysRun = true)
	protected void uninstall() throws Exception {

		uninstallApplicationIfFound(APP_NAME, SecurityConstants.ALL_ROLES_USER_PWD, SecurityConstants.ALL_ROLES_USER_PWD);
		uninstallApplicationIfFound(APP_NAME, SecurityConstants.ALL_ROLES_USER_PWD, SecurityConstants.ALL_ROLES_USER_PWD);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installWithcustomCloudAdminTest() throws IOException, InterruptedException {
		
		installApplicationAndWait(APP_PATH, APP_NAME, TIMEOUT_IN_MINUTES, CLOUD_ADMIN_USER_AND_PASSWORD, CLOUD_ADMIN_USER_AND_PASSWORD, false, null);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installWithCustomViewerTest() throws IOException, InterruptedException{
		
		String output = installApplicationAndWait(APP_PATH, APP_NAME, TIMEOUT_IN_MINUTES, VIEWER_USER_AND_PASSWORD, VIEWER_USER_AND_PASSWORD, true, null);
		
		assertTrue("install access granted to " + VIEWER_USER_AND_PASSWORD, output.contains("no_permission_access_is_denied"));
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
