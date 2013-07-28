package org.cloudifysource.quality.iTests.test.esm.stateful.manual.memory;

import iTests.framework.utils.DeploymentUtils;
import iTests.framework.utils.GsmTestUtils;
import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.*;

import java.io.IOException;

public class DedicatedManualByonStatefulScaleOutTest extends AbstractFromXenToByonGSMTest {
	
	static final int CONTAINER_CAPACITY = 256;
	
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
    public void doTest() throws IOException {
    
    	repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);

        final ProcessingUnit pu = super.deploy(
        		new ElasticStatefulProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("simpledata", "processor"))
        		.maxMemoryCapacity(1024, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(CONTAINER_CAPACITY,MemoryUnit.MEGABYTES)
                .dedicatedMachineProvisioning(getMachineProvisioningConfig())
        );

        // we are doing here a variation where we scale right after the deployment
        pu.scale(
    		new ManualCapacityScaleConfigurer()
    		.memoryCapacity(512, MemoryUnit.MEGABYTES)
    		.create());
        
	    int expectedNumberOfContainers = 2;
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
            .memoryCapacity(1024, MemoryUnit.MEGABYTES)
            .create());
        
	    int expectedNumberOfContainersAfterScaleOut = 4;
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainersAfterScaleOut, expectedNumberOfMachines, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsAdded(4, OPERATION_TIMEOUT);
        
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainersAfterScaleOut, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
        
        // make sure all pojos handled by PUs
        assertEquals("Number of Person Pojos in space", NUM_OF_POJOS, GsmTestUtils.countData(pu));
        
        assertUndeployAndWait(pu);
    }
	
}


