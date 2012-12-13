package test.cli.cloudify.security;

import java.io.IOException;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import test.cli.cloudify.AbstractSecuredLocalCloudTest;
import framework.utils.LocalCloudBootstrapper;

/**
 * CLOUDIFY-1298
 * @author elip
 *
 */
public class DefaultSecurityFileLocationTest extends AbstractSecuredLocalCloudTest {
	
	private LocalCloudBootstrapper bootstrapper;
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testDefaultSecurityFileLocation() throws Exception {
		bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.secured(true).securityFilePath(SecurityConstants.BUILD_SECURITY_FILE_PATH);
		bootstrapper.keystoreFilePath(SecurityConstants.DEFAULT_KEYSTORE_FILE_PATH).keystorePassword(SecurityConstants.DEFAULT_KEYSTORE_PASSWORD);
		super.bootstrap(bootstrapper);
	}
	
	@AfterClass(alwaysRun = true)
	public void teardown() throws IOException, InterruptedException {
		super.teardown();
	}
}
