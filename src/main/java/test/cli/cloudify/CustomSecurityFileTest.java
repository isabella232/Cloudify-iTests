package test.cli.cloudify;

import java.io.IOException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import framework.tools.SGTestHelper;
import framework.utils.ApplicationInstaller;
import framework.utils.LocalCloudBootstrapper;

public class CustomSecurityFileTest extends AbstractSecuredLocalCloudTest{

	private static final String SGTEST_ROOT_DIR = SGTestHelper.getSGTestRootDir().replace('\\', '/');
	private static final String CLOUD_ADMIN_USER_AND_PASSWORD = "John"; 
	private static final String VIEWER_USER_AND_PASSWORD = "Amanda"; 
	private static final String APP_NAME = "simple";
	private static final String CUSTUM_SECURITY_FILE_PATH = SGTEST_ROOT_DIR + "/src/main/config/security/spring-security.xml";
	private static final String APP_PATH = SGTEST_ROOT_DIR + "/src/main/resources/apps/USM/usm/applications/" + APP_NAME;
	
	@Override
	@BeforeClass
	public void bootstrap() throws IOException {
		LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.secured(true).securityFilePath(CUSTUM_SECURITY_FILE_PATH);
		bootstrapper.keystoreFilePath(getDefaultKeystoreFilePath()).keystorePassword(getDefaultKeystorePassword());
		super.bootstrap(bootstrapper);		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installWithCustomCloudAdminTest() throws IOException, InterruptedException {
		
		ApplicationInstaller appInstaller = new ApplicationInstaller(restUrl, APP_NAME);
		String output = appInstaller.cloudifyUsername(CLOUD_ADMIN_USER_AND_PASSWORD).cloudifyPassword(CLOUD_ADMIN_USER_AND_PASSWORD).recipePath(APP_PATH).install();
				
		appInstaller.assertInstall(output);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installWithCustomViewerTest() throws IOException, InterruptedException{
		
		ApplicationInstaller appInstaller = new ApplicationInstaller(restUrl, APP_NAME);
		String output = appInstaller.cloudifyUsername(VIEWER_USER_AND_PASSWORD).cloudifyPassword(VIEWER_USER_AND_PASSWORD).recipePath(APP_PATH).expectToFail(true).install();

		assertTrue("install access granted to " + VIEWER_USER_AND_PASSWORD, output.contains("no_permission_access_is_denied"));
		appInstaller.assertInstall(output);
	}
}
