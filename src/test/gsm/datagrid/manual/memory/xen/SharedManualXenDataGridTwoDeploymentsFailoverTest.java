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
import test.gsm.GsmTestUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
/**
 *  Setup:
 *  myGrid: 512m total, 256m per container 
 *  myGrid2: 512m total, 256m per container
 *
 *  Test: killing 1 machine (the one without a GSM on it),
 *  check initial state was restored and that only 1 new machine is up
 *  
 *  There are four versions: one with DiscoveredMachineProvisioningConfig and the other with
 *  XenServerMachineProvisioningConfig those two are mixed with one with hard machine shutdown and another with
 *  soft machine shutdown 
 *
 * @author dank
 */
public class SharedManualXenDataGridTwoDeploymentsFailoverTest extends AbstractXenGSMTest {

    private static final String SHARING_ID = "sharedGroup";
    
    private boolean discoveredMachineProvisioning = false;

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
    public void manualXenDataGridDeploymentHardShutdownFailoverXenMachineProvisioningTest() {
        manualXenDataGridDeploymentTwoIndenpendentDeploymentsFailoverTest(true, getMachineProvisioningConfig());
    }
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
    public void manualXenDataGridDeploymentCleanShutdownFailoverXenMachineProvisioningTest() {
        manualXenDataGridDeploymentTwoIndenpendentDeploymentsFailoverTest(false, getMachineProvisioningConfig());
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void manualXenDataGridDeploymentCleanShutdownFailoverDiscoveredMachineProvisioningTest() {
        manualXenDataGridDeploymentTwoIndenpendentDeploymentsFailoverTest(false, getDiscoveredMachineProvisioningConfig());
    }

    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void manualXenDataGridDeploymentHardShutdownFailoverDiscoveredMachineProvisioningTest() {
        manualXenDataGridDeploymentTwoIndenpendentDeploymentsFailoverTest(true, getDiscoveredMachineProvisioningConfig());
    }
    
    private void manualXenDataGridDeploymentTwoIndenpendentDeploymentsFailoverTest(boolean hardShutdown, ElasticMachineProvisioningConfig machineProvisioningConfig) {
        
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
                .sharedMachineProvisioning(SHARING_ID,machineProvisioningConfig)
        );
        
        final ProcessingUnit pu2 = gsm.deploy(new ElasticSpaceDeployment("myGrid2")
                .maxMemoryCapacity(512, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(512,MemoryUnit.MEGABYTES)
                       .create())
                .sharedMachineProvisioning(SHARING_ID,machineProvisioningConfig)
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
        
        assertEquals("Number of GSCs added", 4, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
        

        assertTrue("Primary/Backup should be deployed on different machines", 
            !space.getInstances()[0].getMachine().equals(space.getInstances()[1].getMachine()));
            
        assertTrue("Primary/Backup should be deployed on different machines", 
            !space2.getInstances()[0].getMachine().equals(space2.getInstances()[1].getMachine()));
        
        assertTrue("Primary/Backup should be deployed on different containers", 
                !space.getInstances()[0].getVirtualMachine().getGridServiceContainer().equals(
                        space.getInstances()[1].getVirtualMachine().getGridServiceContainer()));

        assertTrue("Primary/Backup should be deployed on different containers", 
                !space2.getInstances()[0].getVirtualMachine().getGridServiceContainer().equals(
                        space2.getInstances()[1].getVirtualMachine().getGridServiceContainer()));
        
        Set<Machine> machines = new HashSet<Machine>();

        machines.add(space.getInstances()[0].getMachine());
        machines.add(space.getInstances()[1].getMachine());
        machines.add(space2.getInstances()[0].getMachine());
        machines.add(space2.getInstances()[1].getMachine());
        
        assertEquals("PUs should be shared on two machines", 2, machines.size());

        /* kill 1 machine (the one without the gsm) */
        Machine gsmMachine = admin.getGridServiceManagers().waitForAtLeastOne().getMachine();
        
        Machine machine = pu.getInstances()[0].getMachine().equals(gsmMachine) ?
                    pu.getInstances()[1].getMachine() : pu.getInstances()[0].getMachine();
        
        if (hardShutdown) {
            GsmTestUtils.hardShutdownMachine(machine, getMachineProvisioningConfig(), OPERATION_TIMEOUT);
        } else {
            GsmTestUtils.shutdownMachine(machine, getMachineProvisioningConfig(), OPERATION_TIMEOUT);
        }

        if (isDiscoveredMachineProvisioning()) {
            startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        }
        
        RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
            public boolean getCondition() {
                return getNumberOfGSCsAdded() == 6;
            }
        };
        
        repetitiveAssertTrue("Expecting machine provisioning failover", condition, OPERATION_TIMEOUT);
        
        /* assert proper failover */
        assertTrue("Failed waiting for space instances", 
                space.waitFor(space.getTotalNumberOfInstances(), 
                        OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for space2 instances", 
                space2.waitFor(space2.getTotalNumberOfInstances(), 
                        OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        
        assertEquals("Number of GSAs added", 3, getNumberOfGSAsAdded());
        
        assertEquals("Number of GSCs removed", 2, getNumberOfGSCsRemoved());

        assertTrue("Primary/Backup should be deployed on different machines", 
            !space.getInstances()[0].getMachine().equals(space.getInstances()[1].getMachine()));
            
        assertTrue("Primary/Backup should be deployed on different machines", 
            !space2.getInstances()[0].getMachine().equals(space2.getInstances()[1].getMachine()));
        
        assertTrue("Primary/Backup should be deployed on different containers", 
                !space.getInstances()[0].getVirtualMachine().getGridServiceContainer().equals(
                        space.getInstances()[1].getVirtualMachine().getGridServiceContainer()));

        assertTrue("Primary/Backup should be deployed on different containers", 
                !space2.getInstances()[0].getVirtualMachine().getGridServiceContainer().equals(
                        space2.getInstances()[1].getVirtualMachine().getGridServiceContainer()));
        
        
        machines = new HashSet<Machine>();

        machines.add(space.getInstances()[0].getMachine());
        machines.add(space.getInstances()[1].getMachine());
        machines.add(space2.getInstances()[0].getMachine());
        machines.add(space2.getInstances()[1].getMachine());
        
        assertEquals("PUs should be shared on two machines", 2, machines.size());
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


