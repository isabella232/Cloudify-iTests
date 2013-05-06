package org.cloudifysource.quality.iTests.test.cli.cloudify.security;

import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.LocalCloudBootstrapper;
import iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractSecuredLocalCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils.ProcessResult;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * test doesn't execute teardown because no bootstrap was performed
 * @author elip
 *
 */
public class LocalCloudSecurityKeystorePathFailTest extends AbstractSecuredLocalCloudTest {

	private static final String FAIL_KEYSTORE_PATH_STRING = "Invalid keystore file";
	private static final String WRONG_KEYSTORE_PATH = SGTestHelper.getSGTestRootDir().replace('\\', '/') + "/src/main/config/";

	private LocalCloudBootstrapper bootstrapper;
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongKeystorePathTest () {
		bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.setBootstrapExpectedToFail(true); 
		bootstrapper.secured(true).securityFilePath(SecurityConstants.BUILD_SECURITY_FILE_PATH);
		bootstrapper.keystoreFilePath(WRONG_KEYSTORE_PATH).keystorePassword(SecurityConstants.DEFAULT_KEYSTORE_PASSWORD);
		ProcessResult res = null ;
		try {
			res = super.bootstrap(bootstrapper);
		} catch (Exception e) {
			AssertUtils.assertFail("bootstrap was failed NOT because of illegal keystore path", e);
		} 
		// The interesting case - bootstrap fails (because of the illegal keystore file path)
		Assert.assertNotNull(res);
		Assert.assertTrue(res.getOutput().contains(FAIL_KEYSTORE_PATH_STRING));
		LogUtils.log("wrong keystore path security test passed!");
	}
	
	@AfterClass
	public void teardown() {
		// test doesn't execute teardown because no bootstrap was performed
	}
}
