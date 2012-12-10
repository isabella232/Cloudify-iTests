package test.cli.cloudify.security;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.AbstractSecuredLocalCloudTest;

import framework.tools.SGTestHelper;
import framework.utils.LocalCloudBootstrapper;
import framework.utils.LogUtils;

public class LocalCloudSecurityPasswordFailTest extends AbstractSecuredLocalCloudTest {

	private static final String DEFAULT_SECURITY_FILE_PATH = SGTestHelper.getSGTestRootDir().replace('\\', '/') + "/src/main/config/security/spring-security.xml";
	private static final String FAKE_PASSWORD = "sgtes";
	private static final String FAIL_PASSWORD_STRING = "Invalid keystore file: Keystore was tampered with, or password was incorrect: Operation failed. CLIStatusException, reason code: invalid_keystore_file, message arguments: Keystore was tampered with, or password was incorrect";

	@Override
	@BeforeClass
	public void bootstrap() throws IOException, TimeoutException, InterruptedException {

	}



	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongPasswordTest () {
		LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.setBootstrapExpectedToFail(true); // new flag which says bootstrapper is about to fail
		bootstrapper.secured(true).securityFilePath(DEFAULT_SECURITY_FILE_PATH);
		bootstrapper.keystoreFilePath(getDefaultKeystoreFilePath()).keystorePassword(FAKE_PASSWORD);
		boolean assertionFail = false;

		try {
			super.bootstrap(bootstrapper);
		} catch (Exception e) {
			LogUtils.log("IOException while bootstraping");
		} 
		// The interesting case - bootstrap fails when the assert fails (because of the illegal password)
		catch (AssertionError e) {
			LogUtils.log(e.getMessage());
			Assert.assertTrue(e.getMessage().contains(FAIL_PASSWORD_STRING));
			assertionFail = true;
		} 

		Assert.assertNotEquals(assertionFail, false);
		LogUtils.log("wrongPasswordTest security test passed!");
	}
}

