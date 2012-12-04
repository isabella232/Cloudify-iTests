package test.cli.cloudify;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import framework.tools.SGTestHelper;
import framework.utils.LocalCloudBootstrapper;

/**
 * CLOUDIFY-1298
 * @author elip
 *
 */
public class DefaultSecurityFileLocationTest extends AbstractSecuredLocalCloudTest {
	
	@Override
	@BeforeClass
	public void bootstrap() throws IOException {
		
		// dont bootstrap, it is part of the test
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testDefaultSecurityFileLocation() throws IOException, TimeoutException, InterruptedException {
		LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.secured(true).securityFilePath(SGTestHelper.getBuildDir() + "/config/security/spring-security.xml");
		bootstrapper.keystoreFilePath(getDefaultKeystoreFilePath()).keystorePassword(getDefaultKeystorePassword());
		super.bootstrap(bootstrapper);
	}

}
