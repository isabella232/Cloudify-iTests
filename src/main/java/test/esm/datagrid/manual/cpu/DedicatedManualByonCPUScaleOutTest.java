package test.esm.datagrid.manual.cpu;

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

import test.esm.AbstractFromXenToByonGSMTest;
import framework.utils.GsmTestUtils;

public class DedicatedManualByonCPUScaleOutTest extends AbstractFromXenToByonGSMTest{
	// Assuming the machines have 2 cores
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
	            
	    final ProcessingUnit pu = super.deploy(new ElasticSpaceDeployment("mygrid")
	            .maxMemoryCapacity(HIGH_MEM_CAPACITY, MemoryUnit.MEGABYTES)
	            .maxNumberOfCpuCores(HIGH_NUM_CPU)
	            .commandLineArgument("-Xmx256m")
	            .commandLineArgument("-Xms256m")
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig())
	            .scale(new ManualCapacityScaleConfigurer()
	            	   .numberOfCpuCores(LOW_NUM_CPU)
	            	   .create())
	    );
	    int expectedNumberOfContainers = (int) Math.ceil(LOW_NUM_CPU/super.getMachineProvisioningConfig().getMinimumNumberOfCpuCoresPerMachine());
	    int expectedNumberOfMachines = expectedNumberOfContainers;
	    
	    GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainers, expectedNumberOfMachines, OPERATION_TIMEOUT*2);

	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
    	
	    final int NUM_OF_POJOS = 1000;
	    GsmTestUtils.writeData(pu, NUM_OF_POJOS);
	    	    
	    // scale out from 2 gscs into 4 gscs	    
	    pu.scale(new ManualCapacityScaleConfigurer()
	    		 .numberOfCpuCores(HIGH_NUM_CPU)
	    		 .create());
		    
	    int expectedNumberOfContainersAfterScaleOut = (int) Math.ceil(HIGH_NUM_CPU/super.getMachineProvisioningConfig().getMinimumNumberOfCpuCoresPerMachine());
	    int expectedNumberOfMachinesAfterScaleOut = expectedNumberOfContainersAfterScaleOut;
	    GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainersAfterScaleOut, expectedNumberOfMachinesAfterScaleOut, OPERATION_TIMEOUT);
	    
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainersAfterScaleOut, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachinesAfterScaleOut, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);

	    // make sure all pojos handled by PUs
        assertEquals("Number of Person Pojos in space", NUM_OF_POJOS, GsmTestUtils.countData(pu));
        
        assertUndeployAndWait(pu);
	}
}
