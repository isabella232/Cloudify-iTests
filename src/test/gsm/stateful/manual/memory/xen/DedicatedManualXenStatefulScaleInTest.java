package test.gsm.stateful.manual.memory.xen;

import java.io.File;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;
import test.utils.DeploymentUtils;

public class DedicatedManualXenStatefulScaleInTest extends AbstractXenGSMTest {
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void doTest() {
    	
        assertEquals(0, getNumberOfGSCsAdded());
        assertEquals(1, getNumberOfGSAsAdded());
                
        DeploymentUtils.prepareApp("simpledata");
        File puDir = DeploymentUtils.getProcessingUnit("simpledata", "processor");        
        final ProcessingUnit pu = gsm.deploy(new ElasticStatefulProcessingUnitDeployment(puDir).
                maxMemoryCapacity(1024, MemoryUnit.MEGABYTES).
                memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES).
                dedicatedMachineProvisioning(super.getMachineProvisioningConfig())
        );

        // we are doing here a variation where we scale after the deployment and not during the deployment
        pu.scale(
    		new ManualCapacityScaleConfigurer()
    		.memoryCapacity(1024, MemoryUnit.MEGABYTES)
    		.create());
        
	    int expectedNumberOfContainers = 4;
	    int expectedNumberOfMachines = 2;
 	    GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainers, expectedNumberOfMachines, OPERATION_TIMEOUT);
	    
	    assertEquals("Number of GSCs added", expectedNumberOfContainers, getNumberOfGSCsAdded());
	    assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
	    assertEquals("Number of GSCs added", expectedNumberOfMachines, getNumberOfGSAsAdded());
	    assertEquals("Number of GSCs removed", 0, getNumberOfGSAsRemoved());

	    final int NUM_OF_POJOS = 1000;
	    GsmTestUtils.writeData(pu, NUM_OF_POJOS);
	            
        pu.scale(
             new ManualCapacityScaleConfigurer()
             .memoryCapacity(512, MemoryUnit.MEGABYTES)
             .create());
        
	    int expectedNumberOfContainersAfterScaleIn = 2;
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainersAfterScaleIn, expectedNumberOfMachines, OPERATION_TIMEOUT);
                assertEquals("Number of GSCs added", 4, getNumberOfGSCsAdded());
        
	    assertEquals("Number of GSCs added", expectedNumberOfContainers, getNumberOfGSCsAdded());
	    assertEquals("Number of GSCs removed", expectedNumberOfContainers - expectedNumberOfContainersAfterScaleIn, getNumberOfGSCsRemoved());
	    assertEquals("Number of GSCs added", expectedNumberOfMachines, getNumberOfGSAsAdded());
	    assertEquals("Number of GSCs removed", 0, getNumberOfGSAsRemoved());
        
        // make sure all pojos handled by PUs
        assertEquals("Number of Person Pojos in space", NUM_OF_POJOS, GsmTestUtils.countData(pu));

    }
	
}


