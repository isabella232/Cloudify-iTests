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
		bootstrapper.secured(true).securityFilePath(getBuildSecurityFilePath());
		bootstrapper.keystoreFilePath(getDefaultKeystoreFilePath()).keystorePassword(getDefaultKeystorePassword());
		super.bootstrap(bootstrapper);
	}
	
	@AfterClass(alwaysRun = true)
	public void teardown() throws IOException, InterruptedException {
		if (bootstrapper != null) {
			super.teardown(bootstrapper);
		}
	}
}
