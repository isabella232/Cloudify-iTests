package test.cli.cloudify.cloud.byon;

import java.io.IOException;

import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.services.byon.ByonCloudService;
import framework.utils.AssertUtils;
import framework.utils.ScriptUtils;


/**
 * This test installs petclinic-simple on byon after changing the byon-cloud.groovy file to contain machine names instead of IPs.
 * <p>It checks whether the bootstrap and instal have succeeded.
 * <p>Note: this test uses 3 fixed machines - 192.168.9.115, 192.168.9.116, 192.168.9.120.
 */
public class NamesAsIPsByonTest extends AbstractByonCloudTest {

	private String namesList = "pc-lab95,pc-lab96,pc-lab100";

	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void testPetclinic() throws IOException, InterruptedException{

		installApplicationAndWait(ScriptUtils.getBuildPath() + "/recipes/apps/petclinic-simple", "petclinic");

		//TODO : edit this, so if it fails it won't be on NPE!
		AssertUtils.assertTrue("petclinic.mongod is not up - install failed", admin.getProcessingUnits().getProcessingUnit("petclinic.mongod") != null);		
		AssertUtils.assertTrue("petclinic.tomcat is not up - install failed", admin.getProcessingUnits().getProcessingUnit("petclinic.tomcat") != null);

	}

	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}

	@Override
	protected void customizeCloud() throws Exception {
		super.customizeCloud();
		getService().setIpList(System.getProperty(ByonCloudService.IP_LIST_PROPERTY, namesList));
	}

	@Override
	protected void beforeTeardown() throws Exception {
		// clean
		if (admin != null) {
			admin.close();
			admin = null;
		}

		try{
			uninstallApplicationAndWait("petclinic");
		} finally {
			System.clearProperty(ByonCloudService.IP_LIST_PROPERTY);
		}
	}
}