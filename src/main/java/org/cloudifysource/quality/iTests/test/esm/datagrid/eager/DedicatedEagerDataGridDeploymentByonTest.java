package org.cloudifysource.quality.iTests.test.esm.datagrid.eager;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.internal.pu.InternalProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.cloudifysource.quality.iTests.framework.utils.GsmTestUtils;

public class DedicatedEagerDataGridDeploymentByonTest extends AbstractFromXenToByonGSMTest {

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

	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1")
	public void testElasticSpaceDeployment() throws Exception {
		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSCsAdded(0,OPERATION_TIMEOUT);
		//startNewVM( 2, 0 , OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
		runTest();
	}

    // Can't start a machine with number of cores = 8 in byon

	/*@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1")
	public void testElasticSpaceDeploymentUnbalnacedCPU() {

		//startNewVM( 8, 0,OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);

		runTest();
	}*/

	private void runTest() {
		final int containerCapacityInMB = 250;
		EagerScaleConfig eagerScaleConfig = new EagerScaleConfig();
		ProcessingUnit pu = super.deploy(
				new ElasticSpaceDeployment("eagerspace")
				.maxMemoryCapacity(4*containerCapacityInMB, MemoryUnit.MEGABYTES)
				.memoryCapacityPerContainer(containerCapacityInMB, MemoryUnit.MEGABYTES)
				.dedicatedMachineProvisioning(getMachineProvisioningConfig())
				.scale(eagerScaleConfig)
				);

		GsmTestUtils.waitForScaleToComplete(pu, 4, 2, OPERATION_TIMEOUT);

		for (GridServiceAgent gsa : admin.getGridServiceAgents()) {
			int containersOnMachine = gsa.getMachine().getGridServiceContainers().getSize();
			assertEquals(2,containersOnMachine);
		}

		assertEquals(eagerScaleConfig,((InternalProcessingUnit)pu).getScaleStrategyConfig());
		assertUndeployAndWait(pu);
	}
}
