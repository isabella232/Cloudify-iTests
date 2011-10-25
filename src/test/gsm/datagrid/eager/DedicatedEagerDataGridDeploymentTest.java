package test.gsm.datagrid.eager;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.GridServiceContainers;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.openspaces.grid.gsm.rebalancing.RebalancingUtils;
import org.testng.annotations.Test;

import test.gsm.AbstractGsmTest;
import test.gsm.GsmTestUtils;

public class DedicatedEagerDataGridDeploymentTest extends AbstractGsmTest {

	//unmark this test when running on laptop
	//@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1", enabled = true)
	public void testElasticSpaceDeploymentOneContainerOnSingleMachine() {
		doElasticSpaceDeployment(2,1);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="4", enabled = true)
	//assumes SGTest has at most 4 machines
	public void testElasticSpaceDeploymentOneContainerPerMachine() {
		doElasticSpaceDeployment(1,4);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="4", enabled = true)
	//assumes SGTest has at most 4 machines
	public void testElasticSpaceDeploymentTwoContainersPerMachine() {
		doElasticSpaceDeployment(2,4);
	}
	
	
	private void doElasticSpaceDeployment(int containersPerMachine,int numberOfMachines) {
	 
		admin.getGridServiceAgents().waitFor(numberOfMachines);
		
		final int numberOfContainers = containersPerMachine * numberOfMachines;
		final int containerCapacityInMB = 256;
		ElasticSpaceDeployment deployment = new ElasticSpaceDeployment("elasticspace")
		.maxMemoryCapacity(numberOfContainers*containerCapacityInMB, MemoryUnit.MEGABYTES)
		.memoryCapacityPerContainer(containerCapacityInMB, MemoryUnit.MEGABYTES);

		if (numberOfMachines ==1 ) {
			deployment.singleMachineDeployment();
		}
		
		ProcessingUnit pu = gsm.deploy(
				deployment
				.dedicatedMachineProvisioning(
						new DiscoveredMachineProvisioningConfigurer()
						.reservedMemoryCapacityPerMachine(128, MemoryUnit.MEGABYTES)
						.create())
				.scale(new EagerScaleConfig()));
		
		GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, OPERATION_TIMEOUT);
		
		Machine[] machines = RebalancingUtils.getMachinesHostingContainers(admin.getGridServiceContainers().getContainers());
        assertEquals(numberOfMachines,machines.length);
		
		for (Machine machine : machines) {
			GridServiceContainers containersOnMachine = machine.getGridServiceContainers();
			assertEquals(containersPerMachine,containersOnMachine.getSize());
			for (GridServiceContainer gsc : containersOnMachine) {
				assertTrue(gsc.getProcessingUnitInstances().length > 0);
			}
		}
	}
}
