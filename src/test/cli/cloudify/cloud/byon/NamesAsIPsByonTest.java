package test.cli.cloudify.cloud.byon;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import framework.utils.ScriptUtils;


/**
 * This test installs petclinic-simple on BYON after changing the byon-cloud.groovy file to contain machine names instead of IPs.
 * <p>It checks whether the bootstrap and install have succeeded.
 * <p>Note: this test uses 3 fixed machines - 192.168.9.115, 192.168.9.116, 192.168.9.120.
 */
public class NamesAsIPsByonTest extends AbstractByonCloudTest {

	private String namesList = "pc-lab95,pc-lab96,pc-lab100";

	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testPetclinic() throws IOException, InterruptedException{

		installApplicationAndWait(ScriptUtils.getBuildPath() + "/recipes/apps/petclinic-simple", "petclinic");
		assertTrue("petclinic.mongod is not up - install failed", admin.getProcessingUnits().waitFor("petclinic.mongod", OPERATION_TIMEOUT, TimeUnit.MILLISECONDS) != null);
		assertTrue("petclinic.tomcat is not up - install failed", admin.getProcessingUnits().waitFor("petclinic.tomcat", OPERATION_TIMEOUT, TimeUnit.MILLISECONDS) != null);
		uninstallApplicationAndWait("petclinic");

	}

	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}

	@Override
	protected void customizeCloud() throws Exception {
		super.customizeCloud();
		getService().setIpList(namesList);
	}
}