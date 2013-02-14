package org.cloudifysource.quality.iTests.test.esm.datagrid.manual.cpu;

import org.cloudifysource.quality.iTests.framework.utils.GsmTestUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DedicatedManualByonCPUMixedTest extends AbstractFromXenToByonGSMTest {
	
	private static final int CONTAINER_CAPACITY = 256;
    private static final int HIGH_NUM_CPU = 8;
    private static final int LOW_NUM_CPU = 4;
    
    private static final int LOW_MEM_CAPACITY = 1024;
    private static final int HIGH_MEM_CAPACITY = 6000;
                         
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
	
    @Test(timeOut = 2* AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1")
    public void doTest() {
    	         
	    // make sure no gscs yet created
    	repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
	            
	    final ProcessingUnit pu = super.deploy(new ElasticSpaceDeployment("mygrid")
	            .maxMemoryCapacity(HIGH_MEM_CAPACITY, MemoryUnit.MEGABYTES)
	            .maxNumberOfCpuCores(HIGH_NUM_CPU)
	            .memoryCapacityPerContainer(CONTAINER_CAPACITY, MemoryUnit.MEGABYTES)
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig())
	            .scale((new ManualCapacityScaleConfigurer()
	            	   .numberOfCpuCores(LOW_NUM_CPU)
	            	   .memoryCapacity(LOW_MEM_CAPACITY,MemoryUnit.MEGABYTES)
	            	   .create()))
	    );

	    int expectedNumberOfMachines = (int)Math.ceil(LOW_NUM_CPU/super.getMachineProvisioningConfig().getMinimumNumberOfCpuCoresPerMachine());
	    int expectedNumberOfContainers = (int) Math.max(
	    		Math.ceil(LOW_MEM_CAPACITY/(1.0*CONTAINER_CAPACITY)),
	    		expectedNumberOfMachines);
	    GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainers, expectedNumberOfMachines, 4*OPERATION_TIMEOUT);

	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
    	
	    final int NUM_OF_POJOS = 1000;
	    GsmTestUtils.writeData(pu, NUM_OF_POJOS);	    
	    	    
	    // now spread 4 gscs on 4 machines	    
	    pu.scale(
	    		new ManualCapacityScaleConfigurer()
	    		.numberOfCpuCores(HIGH_NUM_CPU)
	    		.memoryCapacity(LOW_MEM_CAPACITY,MemoryUnit.MEGABYTES)
	    		.create());
	    	
	    int expectedNumberOfMachinesAfterScaleOut = (int)Math.ceil(HIGH_NUM_CPU/super.getMachineProvisioningConfig().getMinimumNumberOfCpuCoresPerMachine());
	    GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainers, expectedNumberOfMachinesAfterScaleOut, 4*OPERATION_TIMEOUT);
	    
	    repetitiveAssertNumberOfGSCsAdded(2+expectedNumberOfContainers, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(2, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachinesAfterScaleOut, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
    	
	    // make sure all pojos handled by PUs
        AbstractTestSupport.assertEquals("Number of Person Pojos in space", NUM_OF_POJOS, GsmTestUtils.countData(pu));
	   
	    // now shrink back to 2 machines	    
	    pu.scale(
	    		new ManualCapacityScaleConfigurer()
	    		.numberOfCpuCores(LOW_NUM_CPU)
	    		.memoryCapacity(LOW_MEM_CAPACITY,MemoryUnit.MEGABYTES)
	    		.create());
	    
	    GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainers, expectedNumberOfMachines, 4*OPERATION_TIMEOUT);
	    
	    repetitiveAssertNumberOfGSCsAdded(4+expectedNumberOfContainers, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(4, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachinesAfterScaleOut, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsRemoved(2, OPERATION_TIMEOUT);
    	
	    // make sure all pojos handled by PUs
        AbstractTestSupport.assertEquals("Number of Person Pojos in space", NUM_OF_POJOS, GsmTestUtils.countData(pu));
	
	    /*// increase (by memory) to 3 machines   
	    pu.scale(
	    		new ManualCapacityScaleConfigurer()
	    		.memoryCapacity(HIGH_MEM_CAPACITY*2,MemoryUnit.MEGABYTES)
	    		.create());
	   
	    int expectedNumberOfContainersAfterScaleOut2 = (int) Math.ceil(HIGH_MEM_CAPACITY/(1.0*CONTAINER_CAPACITY));
	    int expectedNumberOfMachinesAfterScaleOut2 = 3;
	    GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainersAfterScaleOut2, expectedNumberOfMachinesAfterScaleOut2, 4*OPERATION_TIMEOUT);
*/
	    // make sure all pojos handled by PUs
        
        assertUndeployAndWait(pu);
    }

}
