package test.gsm.datagrid.eager.xen;

import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.cloud.xenserver.XenServerMachineProvisioningConfig;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;


public class DedicatedEagerDataGridTwoIndependentDeploymentsXenTest extends AbstractXenGSMTest {

	private static final String ZONE1 = "zone1";
	private static final String ZONE2 = "zone2";

	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1")
	public void testElasticSpaceDeployment() {
		
		XenServerMachineProvisioningConfig machineProvisioningConfig1 = super.getMachineProvisioningConfigWithMachineZone(new String[] { ZONE1});
		GridServiceAgent gsa1 = startNewVM( 0, 0, machineProvisioningConfig1, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		GridServiceAgent gsa2 = startNewVM( 0, 0, machineProvisioningConfig1, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        
		XenServerMachineProvisioningConfig machineProvisioningConfig2 = super.getMachineProvisioningConfigWithMachineZone(new String[] { ZONE2});
		GridServiceAgent gsa3 = startNewVM( 0, 0, machineProvisioningConfig2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		GridServiceAgent gsa4 = startNewVM( 0, 0, machineProvisioningConfig2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		
		final int containerCapacityInMB = 250;
		ProcessingUnit pu1 = gsm.deploy(
				new ElasticSpaceDeployment("eagerspace1")
				.maxMemoryCapacity(2*containerCapacityInMB, MemoryUnit.MEGABYTES)
				.memoryCapacityPerContainer(containerCapacityInMB, MemoryUnit.MEGABYTES)
				.dedicatedMachineProvisioning(
						new DiscoveredMachineProvisioningConfigurer()
						.addGridServiceAgentZone(ZONE1)
						.dedicatedManagementMachines()
						.create())
				
				.scale(new EagerScaleConfigurer()
				       .create())
		);
		
		GsmTestUtils.waitForScaleToComplete(pu1, 2, 5, OPERATION_TIMEOUT);
		
		Assert.assertEquals(1, countGridServiceContainersOnMachine(gsa1));
		Assert.assertEquals(1, countGridServiceContainersOnMachine(gsa2));
		Assert.assertEquals(0, countGridServiceContainersOnMachine(gsa3));
		Assert.assertEquals(0, countGridServiceContainersOnMachine(gsa4));
		Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(gsa1, pu1));
		Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(gsa2, pu1));
		Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(gsa3, pu1));
		Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(gsa4, pu1));
		
		ProcessingUnit pu2 = gsm.deploy(
				new ElasticSpaceDeployment("eagerspace2")
				.maxMemoryCapacity(2*containerCapacityInMB, MemoryUnit.MEGABYTES)
				.memoryCapacityPerContainer(containerCapacityInMB, MemoryUnit.MEGABYTES)
				.dedicatedMachineProvisioning(
						new DiscoveredMachineProvisioningConfigurer()
						.dedicatedManagementMachines()
					    .addGridServiceAgentZone(ZONE2)
					    .create())
				.scale(new EagerScaleConfig())
		);
		
		GsmTestUtils.waitForScaleToComplete(pu2, 4, 5, OPERATION_TIMEOUT);
		
		Assert.assertEquals(1, countGridServiceContainersOnMachine(gsa1));
		Assert.assertEquals(1, countGridServiceContainersOnMachine(gsa2));
		Assert.assertEquals(1, countGridServiceContainersOnMachine(gsa3));
		Assert.assertEquals(1, countGridServiceContainersOnMachine(gsa4));
		Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(gsa1, pu1));
		Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(gsa2, pu1));
		Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(gsa3, pu1));
		Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(gsa4, pu1));
		Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(gsa1, pu2));
		Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(gsa2, pu2));
		Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(gsa3, pu2));
		Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(gsa4, pu2));
	}
	
	int countProcessingUnitInstancesOnMachine(GridServiceAgent agent, ProcessingUnit pu) {
		return agent.getMachine().getProcessingUnitInstances(pu.getName()).length;
	}
	
	int countGridServiceContainersOnMachine(GridServiceAgent agent) {
	    return agent.getMachine().getGridServiceContainers().getSize();
	}
}