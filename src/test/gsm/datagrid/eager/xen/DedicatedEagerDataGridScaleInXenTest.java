package test.gsm.datagrid.eager.xen;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;

public class DedicatedEagerDataGridScaleInXenTest extends AbstractXenGSMTest {

	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1")
	public void doTest() {
		
		startNewVM(OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
		GridServiceAgent gsa3 = startNewVM(OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
		GridServiceAgent gsa4 = startNewVM(OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
        
		final int containerCapacityInMB = 250;
		int numberOfContainers = 4;
		ProcessingUnit pu = gsm.deploy(
				new ElasticSpaceDeployment("eagerspace")
				.maxMemoryCapacity(numberOfContainers*containerCapacityInMB, MemoryUnit.MEGABYTES)
				.memoryCapacityPerContainer(containerCapacityInMB, MemoryUnit.MEGABYTES)
				.dedicatedMachineProvisioning(
						new DiscoveredMachineProvisioningConfigurer()
						.reservedMemoryCapacityPerMachine(128, MemoryUnit.MEGABYTES)
						.create())
				.scale(new EagerScaleConfig())
		);
		
		GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, 4, OPERATION_TIMEOUT);
		
		int numberOfObjects = 1000;
		GsmTestUtils.writeData(pu, numberOfObjects);
		
		GsmTestUtils.shutdownMachine(gsa3.getMachine(), super.getMachineProvisioningConfig(), OPERATION_TIMEOUT);
		GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, 3, OPERATION_TIMEOUT);
		
		GsmTestUtils.writeData(pu, numberOfObjects, 2*numberOfObjects);
		
		GsmTestUtils.shutdownMachine(gsa4.getMachine(), super.getMachineProvisioningConfig(), OPERATION_TIMEOUT);
		GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, 2, OPERATION_TIMEOUT);
		
		GsmTestUtils.writeData(pu, numberOfObjects, 3*numberOfObjects);
	}
}
