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
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.cloud.xenserver.XenServerMachineProvisioningConfig;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.utils.AssertUtils.RepetitiveConditionProvider;
/**
 *  Setup:
 *  myGrid: total 1024m, per container 256m, expecting 4 GSCs on 2 machines
 *  myGrid2: total 512m, per container 256m, expecting 2 GSCs on 2 machines
 *
 *  Scale In: myGrid 1024m --> 512m
 *  myGrid: total 1024m, per container 256m, expecting 2 GSCs on 2 machines
 *  myGrid2: total 512m, per container 256m, expecting 2 GSCs on 2 machines
 *  
 *  Scale Out: myGrid2 512m --> 1024m
 *  expecting same as setup only switching roles
 *  myGrid: total 1024m, per container 256m, expecting 2 GSCs on 2 machines
 *  myGrid2: total 512m, per container 256m, expecting 4 GSCs on 2 machines
 * 
 *  here are two versions: one with DiscoveredMachineProvisioningConfig and the other with
 *  XenServerMachineProvisioningConfig. 
 * 
 *  @author dank
 */
public class SharedManualXenDataGridTwoDeploymentsScaleInAndOutTest extends AbstractXenGSMTest {

	private static final String MY_GRID_A = "myGrid_A";
	private static final String MY_GRID_B = "myGrid_B";

	private static final String SHARING_ID = "sharedGroup";

    private boolean discoveredMachineProvisioning = false;
    
    @Override
    protected void overrideXenServerProperties(XenServerMachineProvisioningConfig machineProvisioningConfig) {
    	super.overrideXenServerProperties(machineProvisioningConfig);
        int reservedMemory = (int)getMachineProvisioningConfig().getReservedMemoryCapacityPerMachineInMB();
        machineProvisioningConfig.setMemoryCapacityPerMachineInMB(reservedMemory + 256 * 3);
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2", enabled = false)
    public void manualXenDataGridDeploymentTwoIndenpendentDeploymentsScaleInScaleOutXenMachineProvisioningTest() {
        manualXenDataGridDeploymentTwoIndenpendentDeploymentsScaleInScaleOutTest(getMachineProvisioningConfig());
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2", enabled = true)
    public void manualXenDataGridDeploymentTwoIndenpendentDeploymentsScaleInScaleOutDiscoveredsMachineProvisioningTest() {
        manualXenDataGridDeploymentTwoIndenpendentDeploymentsScaleInScaleOutTest(getDiscoveredMachineProvisioningConfig());
    }
    
    public void manualXenDataGridDeploymentTwoIndenpendentDeploymentsScaleInScaleOutTest(ElasticMachineProvisioningConfig machineProvisioningConfig) {
   	    
        if (machineProvisioningConfig instanceof DiscoveredMachineProvisioningConfig) {
            setupDiscovereMachineProvisioningEnvironment();
        } 
        
    	assertEquals(0, getNumberOfGSCsAdded());
        
        final ProcessingUnit pu = gsm.deploy(new ElasticSpaceDeployment(MY_GRID_A)
                .maxMemoryCapacity(1024, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(256, MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(1024,MemoryUnit.MEGABYTES)
                       .create())
                .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );
        
        final ProcessingUnit pu2 = gsm.deploy(new ElasticSpaceDeployment(MY_GRID_B)
                .maxMemoryCapacity(1024, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(512,MemoryUnit.MEGABYTES)
                       .create())
                .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
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
        
        assertEquals("Number of GSAs added", 2, getNumberOfGSAsAdded());
        
        assertEquals("Number of GSCs added", 6, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs in zone", 4, admin.getZones().getByName(MY_GRID_A).getGridServiceContainers().getSize());
        assertEquals("Number of GSCs in zone", 2, admin.getZones().getByName(MY_GRID_B).getGridServiceContainers().getSize());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());

        assertEquals("Expected Number of space instances", 4, space.getInstances().length);
        assertEquals("Expected Number of space instances", 4, space2.getInstances().length);
        

        assertEquals("Expecting deployment on two machines", 2, getNumberOfMachinesWithSpaceInstance(space));
        assertEquals("Expecting deployment on two machines", 2, getNumberOfMachinesWithSpaceInstance(space2));
        
        assertDeploymentOnSharedMachines(space, space2);
        
		/* scale in myGrid */
	    pu.scale(new ManualCapacityScaleConfigurer().memoryCapacity(512, MemoryUnit.MEGABYTES).create());
	    
        RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
            public boolean getCondition() {
                return getNumberOfGSCsRemoved() == 2;
            }
        };
	    
	    repetitiveAssertTrue("Expeting two GSCs to be removed after scale in", condition, OPERATION_TIMEOUT);
        
	    assertEquals("Number of GSAs added", 2, getNumberOfGSAsAdded());
        
	    assertEquals("Number of GSCs in zone", 2, admin.getZones().getByName(MY_GRID_A).getGridServiceContainers().getSize());
        assertEquals("Number of GSCs in zone", 2, admin.getZones().getByName(MY_GRID_B).getGridServiceContainers().getSize());
        
        assertTrue("Failed waiting for space instances", 
                space.waitFor(space.getTotalNumberOfInstances(), 
                		OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for space2 instances", 
                space2.waitFor(space2.getTotalNumberOfInstances(), 
                        OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        
        assertEquals("Expected Number of space instances", 4, space.getInstances().length);
        assertEquals("Expected Number of space instances", 4, space2.getInstances().length);
        
        assertEquals("Expecting deployment on two machines", 2, getNumberOfMachinesWithSpaceInstance(space));
        assertEquals("Expecting deployment on two machines", 2, getNumberOfMachinesWithSpaceInstance(space2));
        
        assertDeploymentOnSharedMachines(space, space2);
        
	    /* scale out myGrid2 */
	    pu2.scale(new ManualCapacityScaleConfigurer().memoryCapacity(1024, MemoryUnit.MEGABYTES).create());
		
	    condition = new RepetitiveConditionProvider() {
            public boolean getCondition() {
                return getNumberOfGSCsAdded() == 8;
            }
        };
        repetitiveAssertTrue("Expeting two GSCs to be added after scale out", condition, OPERATION_TIMEOUT);
        
        assertEquals("Number of GSAs added", 2, getNumberOfGSAsAdded());
        
        assertEquals("Number of GSCs in zone", 2, admin.getZones().getByName(MY_GRID_A).getGridServiceContainers().getSize());
        assertEquals("Number of GSCs in zone", 4, admin.getZones().getByName(MY_GRID_B).getGridServiceContainers().getSize());
        
        assertTrue("Failed waiting for space instances", 
                space.waitFor(space.getTotalNumberOfInstances(), 
                		OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for space2 instances", 
                space2.waitFor(space2.getTotalNumberOfInstances(), 
                        OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        
        assertEquals("Expected Number of space instances", 4, space.getInstances().length);
        assertEquals("Expected Number of space instances", 4, space2.getInstances().length);
        
        assertEquals("Expecting deployment on two machines", 2, getNumberOfMachinesWithSpaceInstance(space));
        assertEquals("Expecting deployment on two machines", 2, getNumberOfMachinesWithSpaceInstance(space2));
        
        assertDeploymentOnSharedMachines(space, space2);
        
    }
	
    private int getNumberOfMachinesWithSpaceInstance(Space space) {
        Set<Machine> spaceMachines = new HashSet<Machine>();
        for (SpaceInstance instance : space.getInstances()) {
            spaceMachines.add(instance.getMachine());
        }
        
        return spaceMachines.size();
    }
    
    private void assertDeploymentOnSharedMachines(Space space1, Space space2) {
        
        Set<Machine> spaceMachines = new HashSet<Machine>();
        for (SpaceInstance instance : space1.getInstances()) {
            spaceMachines.add(instance.getMachine());
        }
        
        
        for (SpaceInstance instance : space2.getInstances()) {
            spaceMachines.add(instance.getMachine());
        }
        
        assertEquals("Expecting deployment of same two machines", 2, spaceMachines.size());
    }
    
    public void setupDiscovereMachineProvisioningEnvironment() {
        discoveredMachineProvisioning = true;
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


