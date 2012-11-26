package test.gsm.stateful.manual.memory.xen;


import java.io.File;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import framework.utils.DeploymentUtils;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;

/**
 * @author giladh
 */
public class DedicatedManualXenStatefulFailoverTest extends AbstractXenGSMTest {

    enum FailureType {
        KILL_CONTAINER,
        SHUTDOWN_MACHINE,
        HARD_SHUTDOWN_MACHINE
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitDeploymentWithKillGSC() {       
        elasticStatefulProcessingUnitDeployment(FailureType.KILL_CONTAINER);  
    }
	
    @Test(timeOut = 2*DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitDeploymentWithShutdownMachine() {    	
        elasticStatefulProcessingUnitDeployment(FailureType.SHUTDOWN_MACHINE);  
    }
    
    @Test(timeOut = 2*DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitDeploymentWithHardShutdownMachine() {       
        elasticStatefulProcessingUnitDeployment(FailureType.HARD_SHUTDOWN_MACHINE);  
    }

    private void elasticStatefulProcessingUnitDeployment( FailureType failureType) {    	    	
    	// parent class is performing all of the necessary 
    	// preparations for test , including loading of lus gsm and asm

        // make sure no gscs yet created
        repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
        
        // deploy PU into ESM (Elastic...) 
        DeploymentUtils.prepareApp("simpledata");
        File puDir = DeploymentUtils.getProcessingUnit("simpledata", "processor");        
        final ProcessingUnit pu = super.deploy(
                new ElasticStatefulProcessingUnitDeployment(puDir)
                .maxMemoryCapacity(1024*4, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                      .memoryCapacity(1024, MemoryUnit.MEGABYTES)
                      .create()) 
                .dedicatedMachineProvisioning(getMachineProvisioningConfig())
        );
        
        int expectedNumberOfMachines = 2;
        int expectedNumberOfContainers = 4;
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu,expectedNumberOfContainers,expectedNumberOfMachines,OPERATION_TIMEOUT);
                
        repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);


        final int NUM_OF_POJOS = 1000;
        GsmTestUtils.writeData(pu, NUM_OF_POJOS);
                          
        int expectedNumberOfMachinesRemoved;
        int expectedNumberOfContainersRemoved;
        switch (failureType) {
            case HARD_SHUTDOWN_MACHINE:
                hardShutdownMachine(
                        findFirstNonManagementMachine(),
                        super.getMachineProvisioningConfig(),
                        OPERATION_TIMEOUT);
        
                expectedNumberOfMachinesRemoved = 1;
                expectedNumberOfContainersRemoved = 2;
                break;
                
            case SHUTDOWN_MACHINE:
        	    shutdownMachine(
    			        findFirstNonManagementMachine(),
    			        super.getMachineProvisioningConfig(),
    			        OPERATION_TIMEOUT);
    	
        	    expectedNumberOfMachinesRemoved = 1;
                expectedNumberOfContainersRemoved = 2;
                break;
            
            case KILL_CONTAINER:
            	GridServiceContainer container = admin.getGridServiceContainers().getContainers()[0];
                GsmTestUtils.killContainer(container);
                
                expectedNumberOfMachinesRemoved = 0;
                expectedNumberOfContainersRemoved = 1;
                break;
           default:
               throw new IllegalStateException("illegal failure type " + failureType);
        }

        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu,expectedNumberOfContainers,expectedNumberOfMachines,OPERATION_TIMEOUT);
        
        repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachinesRemoved+expectedNumberOfMachines, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSAsRemoved(expectedNumberOfMachinesRemoved, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainersRemoved+expectedNumberOfContainers, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(expectedNumberOfContainersRemoved, OPERATION_TIMEOUT);       
        
	    // make sure all pojos handled by PUs
        assertEquals("Number of Person Pojos in space", NUM_OF_POJOS, GsmTestUtils.countData(pu));

        assertUndeployAndWait(pu);
        
    }
    
    Machine findFirstNonManagementMachine() {
        GridServiceContainer[] containers = admin.getGridServiceContainers().getContainers();
        for (GridServiceContainer container : containers) {
            if (!container.getMachine().equals(super.getGridServiceManager().getMachine())) {
                
                return container.getMachine();
            }
        }
        return null;
    }
}
