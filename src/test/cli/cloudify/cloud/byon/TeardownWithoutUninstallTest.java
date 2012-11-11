package test.cli.cloudify.cloud.byon;

import java.io.IOException;

import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import framework.utils.ApplicationInstaller;
import framework.utils.AssertUtils;
import framework.utils.ScriptUtils;

public class TeardownWithoutUninstallTest extends AbstractByonCloudTest {
	
	private final static String TRAVEL_PATH = ScriptUtils.getBuildPath() + "/recipes/apps/travel";
	private final static String PETCLINIC_SIMPLE_PATH = ScriptUtils.getBuildPath() + "/recipes/apps/petclinic-simple";
	
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

		ApplicationInstaller travelInstaller = new ApplicationInstaller(getRestUrl(), "travel");
		travelInstaller.setRecipePath(TRAVEL_PATH);
		
		ApplicationInstaller petclinicInstaller = new ApplicationInstaller(getRestUrl(), "petclinic");
		petclinicInstaller.setRecipePath(PETCLINIC_SIMPLE_PATH);

		travelInstaller.install();
		petclinicInstaller.install();
		
		super.teardown();
		
		AssertUtils.assertTrue("Application 'travel' should not have been discovered since a teardown was executed", 
				admin.getApplications().getApplication("travel") == null);
		
		AssertUtils.assertTrue("Application 'petclinic' should not have been discovered since a teardown was executed", 
				admin.getApplications().getApplication("petclinic") == null);

	}
	
	@Override
	protected void beforeTeardown() throws Exception {
		// override so as to not close the admin. we need it after teardown
	}

}
