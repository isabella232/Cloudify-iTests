package test.cli.cloudify.security;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.AbstractSecuredLocalCloudTest;
import test.cli.cloudify.CommandTestUtils.ProcessResult;
import framework.tools.SGTestHelper;
import framework.utils.LocalCloudBootstrapper;
import framework.utils.LogUtils;

public class LocalCloudSecurityKeystorePathFailTest extends AbstractSecuredLocalCloudTest {

	private static final String DEFAULT_SECURITY_FILE_PATH = SGTestHelper.getSGTestRootDir().replace('\\', '/') + "/src/main/config/security/spring-security.xml";
	private static final String DEFAULT_KEYSTORE_PASSWORD = "sgtest";
	private static final String FAIL_KEYSTORE_PATH_STRING = "Invalid keystore file";
	private static final String WRONG_KEYSTORE_PATH = SGTestHelper.getSGTestRootDir().replace('\\', '/') + "/src/main/config/";

	@Override
	@BeforeClass
	public void bootstrap() throws IOException, TimeoutException, InterruptedException {

	}



	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void wrongKeystorePathTest () {
		LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.setBootstrapExpectedToFail(true); 
		bootstrapper.secured(true).securityFilePath(DEFAULT_SECURITY_FILE_PATH);
		bootstrapper.keystoreFilePath(WRONG_KEYSTORE_PATH).keystorePassword(DEFAULT_KEYSTORE_PASSWORD);
		ProcessResult res = null ;
		try {
			res  = super.bootstrap(bootstrapper);
		} catch (Exception e) {
			LogUtils.log("Exception while bootstraping");
			AssertFail("bootstrap was failed NOT because of illegal keystore path");
		} 
		// The interesting case - bootstrap fails (because of the illegal keystore file path)
		Assert.assertNotNull(res);
		Assert.assertTrue(res.getOutput().contains(FAIL_KEYSTORE_PATH_STRING));
		LogUtils.log("wrong keystore path security test passed!");
	}
	

}
