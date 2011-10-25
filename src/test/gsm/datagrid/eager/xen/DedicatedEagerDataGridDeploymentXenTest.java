package test.gsm.datagrid.eager.xen;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.internal.pu.InternalProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;

public class DedicatedEagerDataGridDeploymentXenTest extends AbstractXenGSMTest {

	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1")
	public void testElasticSpaceDeployment() {
		
		startNewVM( 2, 0 , OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        
		runTest();
	}

	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1")
	public void testElasticSpaceDeploymentUnbalnacedCPU() {
		
		startNewVM( 8, 0,OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        
		runTest();
	}
	
	private void runTest() {
		final int containerCapacityInMB = 250;
		EagerScaleConfig eagerScaleConfig = new EagerScaleConfig();
		ProcessingUnit pu = gsm.deploy(
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
	}
}
