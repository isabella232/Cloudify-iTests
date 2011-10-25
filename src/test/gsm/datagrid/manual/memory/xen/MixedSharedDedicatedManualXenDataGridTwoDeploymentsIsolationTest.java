package test.gsm.datagrid.manual.memory.xen;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.admin.space.Space;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
/**
 * 
 *  Setup:
 *  myGrid: Shared deployment, total 512m, per container 256m
 *  myGrid2: Dedicated deployment, total 512m, per container 256m
 *  
 *  expecting each deployment to be on two different 2 machines.
 *  expecting 4 machines in total
 *
 *  There are two versions: one with DiscoveredMachineProvisioningConfig and the other with
 *  XenServerMachineProvisioningConfig. 
 *
 * @author dank
 */
public class MixedSharedDedicatedManualXenDataGridTwoDeploymentsIsolationTest extends AbstractXenGSMTest {

    private boolean discoveredMachineProvisioning = false;

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void mixedSharedDedicatedManualXenDataGridDeploymentTwoIndenpendentIsolationXenMachineProvisioningTest() {
        mixedSharedDedicatedManualXenDataGridDeploymentTwoIndenpendentIsolationTest(
                getMachineProvisioningConfig());
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void mixedSharedDedicatedManualXenDataGridDeploymentTwoIndenpendentIsolationDiscoveredsMachineProvisioningTest() {
        mixedSharedDedicatedManualXenDataGridDeploymentTwoIndenpendentIsolationTest(
                getDiscoveredMachineProvisioningConfig());
    }

    public void mixedSharedDedicatedManualXenDataGridDeploymentTwoIndenpendentIsolationTest(
            ElasticMachineProvisioningConfig machineProvisioningConfig) {

        if (machineProvisioningConfig instanceof DiscoveredMachineProvisioningConfig) {
            setupDiscovereMachineProvisioningEnvironment();
        } 
        
      	assertEquals(0, getNumberOfGSCsAdded());
        
        final ProcessingUnit pu = gsm.deploy(new ElasticSpaceDeployment("myGrid")
                .maxMemoryCapacity(512, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(512,MemoryUnit.MEGABYTES)
                       .create())
                .sharedMachineProvisioning("myGridShare", machineProvisioningConfig)
        );
        
        final ProcessingUnit pu2 = gsm.deploy(new ElasticSpaceDeployment("myGrid2")
                .maxMemoryCapacity(512, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(512,MemoryUnit.MEGABYTES)
                       .create())
                .dedicatedMachineProvisioning(machineProvisioningConfig)
        );
        
        Space space = pu.waitForSpace(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        Space space2 = pu2.waitForSpace(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        
        assertNotNull("Failed getting space instance", space);
        assertNotNull("Failed getting space2 instance", space2);
        
        assertTrue("Failed waiting for space instances", 
                space.waitFor(space.getTotalNumberOfInstances(), 
                		OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for space2 instances", 
                space2.waitFor(space2.getTotalNumberOfInstances(), 
                        OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        
        
        assertEquals("Number of GSAs added", 4, getNumberOfGSAsAdded());
        
        assertEquals("Number of GSCs added", 4, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
        

        assertTrue("Primary/Backup should be deployed on different machines", 
			!space.getInstances()[0].getMachine().equals(space.getInstances()[1].getMachine()));
			
		assertTrue("Primary/Backup should be deployed on different machines", 
			!space2.getInstances()[0].getMachine().equals(space2.getInstances()[1].getMachine()));
		
		
        Set<Machine> machines = new HashSet<Machine>();

		machines.add(space.getInstances()[0].getMachine());
		machines.add(space.getInstances()[1].getMachine());
		machines.add(space2.getInstances()[0].getMachine());
		machines.add(space2.getInstances()[1].getMachine());
		
		assertEquals("PUs should be on four different machines", 4, machines.size());
		
    }
	
    public void setupDiscovereMachineProvisioningEnvironment() {
        discoveredMachineProvisioning = true;
        startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public boolean isDiscoveredMachineProvisioning() {
        return discoveredMachineProvisioning;
    }
    
    public boolean isXenMachineProvisioning() {
        return !isDiscoveredMachineProvisioning();
    }
    
    @Override
    @AfterMethod
    public void afterTest() {
        discoveredMachineProvisioning = false;
        super.afterTest();
    }
    
}


