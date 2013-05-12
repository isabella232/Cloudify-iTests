package org.cloudifysource.quality.iTests.test.esm.datagrid.manual.cpu;

import java.io.IOException;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import iTests.framework.utils.GsmTestUtils;

public class DedicatedManualByonCPUScaleInTest extends AbstractFromXenToByonGSMTest {
	private static final int HIGH_NUM_CPU = 8;
    private static final int LOW_NUM_CPU = 4;
    private static final int HIGH_MEM_CAPACITY = 1024;
                         
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
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT*2, groups = "1")
    public void doTest() throws IOException {
    	         
	    // make sure no gscs yet created
    	repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
	            
	    final ProcessingUnit pu = super.deploy(
	    		new ElasticSpaceDeployment("mygrid")
	    		.maxMemoryCapacity(HIGH_MEM_CAPACITY, MemoryUnit.MEGABYTES)
	            .maxNumberOfCpuCores(HIGH_NUM_CPU)
	            .commandLineArgument("-Xmx256m")
	            .commandLineArgument("-Xms256m")
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig())
	            .scale(new ManualCapacityScaleConfigurer()
	            		 .numberOfCpuCores(HIGH_NUM_CPU)
	            		 .create())
	    );

	    int expectedNumberOfContainers = (int) Math.ceil(HIGH_NUM_CPU/super.getMachineProvisioningConfig().getMinimumNumberOfCpuCoresPerMachine());
	    int expectedNumberOfMachines = expectedNumberOfContainers;
	    
	    GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainers, expectedNumberOfMachines, OPERATION_TIMEOUT*2);
	    
        repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
    	
	    final int NUM_OF_POJOS = 1000;
	    GsmTestUtils.writeData(pu, NUM_OF_POJOS);
	    	    
	    // compress 4 gscs into 2 machines	    
	    pu.scale(new ManualCapacityScaleConfigurer()
	    		 .numberOfCpuCores(LOW_NUM_CPU)
	    		 .create());
	    	    
	    int expectedNumberOfContainersAfterScaleIn = (int) Math.ceil(LOW_NUM_CPU/super.getMachineProvisioningConfig().getMinimumNumberOfCpuCoresPerMachine());
	    int expectedNumberOfMachinesAfterScaleIn = expectedNumberOfContainersAfterScaleIn;
	    int expectedRemoved = expectedNumberOfContainers - expectedNumberOfContainersAfterScaleIn;
	    GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainersAfterScaleIn, expectedNumberOfMachinesAfterScaleIn, OPERATION_TIMEOUT);
	    
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(expectedRemoved, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsRemoved(expectedRemoved, OPERATION_TIMEOUT);
    		    
	    // make sure all pojos handled by PUs
        assertEquals("Number of Person Pojos in space", NUM_OF_POJOS, GsmTestUtils.countData(pu));
        
        assertUndeployAndWait(pu);
	}
}
