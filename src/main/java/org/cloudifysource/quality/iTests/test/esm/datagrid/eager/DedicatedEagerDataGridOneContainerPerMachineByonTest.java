package org.cloudifysource.quality.iTests.test.esm.datagrid.eager;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.internal.pu.InternalProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import iTests.framework.utils.GsmTestUtils;

public class DedicatedEagerDataGridOneContainerPerMachineByonTest extends AbstractFromXenToByonGSMTest {

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

	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="2", enabled = true)
	public void testElasticSpaceDeploymentOneContainerOnSingleMachine() throws Exception {

		startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);

		final int containerCapacityInMB = 250;
		ProcessingUnit pu = super.deploy(
				new ElasticSpaceDeployment("eagerspace")
				.maxMemoryCapacity(4*containerCapacityInMB, MemoryUnit.MEGABYTES)
				.memoryCapacityPerContainer(containerCapacityInMB, MemoryUnit.MEGABYTES)
				.dedicatedMachineProvisioning(getMachineProvisioningConfig())
				.scale(	new EagerScaleConfigurer()
				.atMostOneContainerPerMachine()
				.create())
				);

		GsmTestUtils.waitForScaleToComplete(pu, 2, 2, OPERATION_TIMEOUT);

		for (GridServiceAgent gsa : admin.getGridServiceAgents()) {
			int containersOnMachine = gsa.getMachine().getGridServiceContainers().getSize();
			assertEquals(1,containersOnMachine);
		}

		assertEquals(new EagerScaleConfigurer().atMostOneContainerPerMachine().create(),((InternalProcessingUnit)pu).getScaleStrategyConfig());
		assertUndeployAndWait(pu);
	}
}


