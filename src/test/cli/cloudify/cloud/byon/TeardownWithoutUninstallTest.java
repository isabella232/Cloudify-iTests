package test.cli.cloudify.cloud.byon;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import framework.utils.AssertUtils;

import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.cloud.ApplicationInstaller;

public class TeardownWithoutUninstallTest extends AbstractByonCloudTest {
	
	private final static String SIMPLE_APP_FOLDER = CommandTestUtils.getPath("apps/USM/usm/applications/simple");
	
	@Override
	protected String getCloudName() {
		return "byon";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void testTeardownWithoutUnInstallApplication() throws IOException, InterruptedException {

		ApplicationInstaller applicationInstaller = new ApplicationInstaller(getRestUrl(), "simple");
		applicationInstaller.setRecipePath(SIMPLE_APP_FOLDER);
		applicationInstaller.install();
		
		super.teardown();
		
		AssertUtils.assertTrue("Application 'simple' should not have been discovered since a teardown was executed", 
				admin.getApplications().waitFor("simple", OPERATION_TIMEOUT, TimeUnit.MILLISECONDS) == null);
	}
	
	@Override
	protected void beforeTeardown() throws Exception {
		// override so as to not close the admin. we need it after teardown
	}

}
