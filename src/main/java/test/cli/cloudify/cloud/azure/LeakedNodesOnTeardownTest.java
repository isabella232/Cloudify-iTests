package test.cli.cloudify.cloud.azure;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.NewAbstractCloudTest;
import framework.utils.ServiceInstaller;

/**
 * CLOUDIFY-1273
 * @author elip
 *
 */
public class LeakedNodesOnTeardownTest extends NewAbstractCloudTest {
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 5, enabled = true)
	public void testTeardownWithoutUninstall() throws Exception {
		
		ServiceInstaller tomcatInstaller = new ServiceInstaller(getRestUrl(), "tomcat");
		tomcatInstaller.recipePath("tomcat");
		
		tomcatInstaller.install();
		
		// this will fail if leaked nodes are found after the teardown.
		super.teardown();
	}

	@Override
	protected String getCloudName() {
		return "azure";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

}
