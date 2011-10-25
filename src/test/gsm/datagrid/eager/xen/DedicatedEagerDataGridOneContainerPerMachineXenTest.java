package test.gsm.datagrid.eager.xen;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.internal.pu.InternalProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;

public class DedicatedEagerDataGridOneContainerPerMachineXenTest extends AbstractXenGSMTest {

	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="2", enabled = true)
	public void testElasticSpaceDeploymentOneContainerOnSingleMachine() {

		startNewVM( OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        
		final int containerCapacityInMB = 250;
		ProcessingUnit pu = gsm.deploy(
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
	}
}


