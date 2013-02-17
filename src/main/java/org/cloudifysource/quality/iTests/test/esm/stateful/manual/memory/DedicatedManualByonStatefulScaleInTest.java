package org.cloudifysource.quality.iTests.test.esm.stateful.manual.memory;

import java.io.File;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.cloudifysource.quality.iTests.framework.utils.DeploymentUtils;
import org.cloudifysource.quality.iTests.framework.utils.GsmTestUtils;

public class DedicatedManualByonStatefulScaleInTest extends AbstractFromXenToByonGSMTest {
	
	@BeforeMethod
    public void beforeTest() {
		super.beforeTestInit();
	}
	
	@BeforeClass
	protected void bootstrap() throws Exception {
		super.bootstrapBeforeClass();
	}
	
	@AfterMethod
    public void afterTest() {
		super.afterTest();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardownAfterClass() throws Exception {
		super.teardownAfterClass();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void doTest() {
    	
        repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
                
        File puDir = DeploymentUtils.getArchive("processorPU.jar");
        
        final ProcessingUnit pu = super.deploy(new ElasticStatefulProcessingUnitDeployment(puDir).
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
	    
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);

	    final int NUM_OF_POJOS = 1000;
	    GsmTestUtils.writeData(pu, NUM_OF_POJOS);
	            
        pu.scale(
             new ManualCapacityScaleConfigurer()
             .memoryCapacity(512, MemoryUnit.MEGABYTES)
             .create());
        
	    int expectedNumberOfContainersAfterScaleIn = 2;
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainersAfterScaleIn, expectedNumberOfMachines, OPERATION_TIMEOUT);
                repetitiveAssertNumberOfGSCsAdded(4, OPERATION_TIMEOUT);
        
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSCsRemoved(expectedNumberOfContainers - expectedNumberOfContainersAfterScaleIn, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
        
        // make sure all pojos handled by PUs
        assertEquals("Number of Person Pojos in space", NUM_OF_POJOS, GsmTestUtils.countData(pu));
        
        assertUndeployAndWait(pu);

    }
	
}


