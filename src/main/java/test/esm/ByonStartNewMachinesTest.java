package test.esm;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AssertUtils;
import framework.utils.LogUtils;

public class ByonStartNewMachinesTest extends AbstractFromXenToByonGSMTest{
	private static final int NUM_OF_MACHINES = 2;

	@BeforeMethod
	public void beforeTest() {
		super.beforeTestInit();
	}

	@BeforeClass
	protected void bootstrap() throws Exception {
		super.bootstrapBeforeClass();
	}

	@AfterMethod
	public void afterTest() {
		super.afterTest();
	}

	@AfterClass(alwaysRun = true)
	protected void teardownAfterClass() throws Exception {
		super.teardownAfterClass();
	}
	
	//This test is covered in other tests
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void startMachineBasicTest() throws Exception {
		LogUtils.log("Start new machines test");
		AssertUtils.assertEquals(0, admin.getGridServiceContainers().getSize());	
		admin.getGridServiceAgents().waitFor(1,OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
		AssertUtils.assertEquals(1, admin.getGridServiceAgents().getSize());
		GridServiceAgent[] agents = startNewByonMachines(getElasticMachineProvisioningCloudifyAdapter(), NUM_OF_MACHINES, OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
		AssertUtils.assertNotNull(agents);
		repetitiveAssertNumberOfGSAsAdded(1+NUM_OF_MACHINES, OPERATION_TIMEOUT);
		LogUtils.log("Start new machines test passed");
	}
}

