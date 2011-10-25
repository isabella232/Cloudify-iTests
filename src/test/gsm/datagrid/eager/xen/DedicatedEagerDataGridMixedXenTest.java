package test.gsm.datagrid.eager.xen;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;

public class DedicatedEagerDataGridMixedXenTest extends AbstractXenGSMTest {

	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1")
	public void doTest() {
		
		startNewVM(OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
		GridServiceAgent gsa3 = startNewVM(OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
		
		final int containerCapacityInMB = 250;
		final int numberOfContainers = 6;
		ProcessingUnit pu = gsm.deploy(
				new ElasticSpaceDeployment("elasticspace")
				.maxMemoryCapacity(numberOfContainers*containerCapacityInMB, MemoryUnit.MEGABYTES)
				.memoryCapacityPerContainer(containerCapacityInMB, MemoryUnit.MEGABYTES)
				.dedicatedMachineProvisioning(
						new DiscoveredMachineProvisioningConfigurer()
						.reservedMemoryCapacityPerMachine(128, MemoryUnit.MEGABYTES)
						.create())
				.scale(new EagerScaleConfig())
		);
		
		int numberOfMachines = 3;
		GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, numberOfMachines, OPERATION_TIMEOUT);
		
		final int numberOfObjects = 1000;
		GsmTestUtils.writeData(pu, numberOfObjects, numberOfObjects);
		
		
		GridServiceContainer[] containers = admin.getGridServiceContainers().getContainers();
		GsmTestUtils.killContainer(containers[0]);		
		GsmTestUtils.writeData(pu, numberOfObjects, 2*numberOfObjects);
		GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, numberOfMachines, OPERATION_TIMEOUT);
				
		GridServiceAgent gsa4 = startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS); //5th machine
		numberOfMachines++;
		
		// wait for new container to start, otherwise next wait for scale may succeed before containers started moving
		gsa4.getMachine().getGridServiceContainers().waitFor(1); 
		GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, numberOfMachines, OPERATION_TIMEOUT);										
						
		// gsa3 was chosen since it does not hold any management process.
		GsmTestUtils.shutdownMachine(gsa3.getMachine(), super.getMachineProvisioningConfig(), OPERATION_TIMEOUT);
		numberOfMachines--;
		GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, numberOfMachines, OPERATION_TIMEOUT);
		
		GsmTestUtils.writeData(pu, numberOfObjects, 3*numberOfObjects);	
	}
}
