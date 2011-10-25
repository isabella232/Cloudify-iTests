package test.gsm.datagrid.manual.cpu.xen;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;


public class DedicatedManualXenCPUMixedTest extends AbstractXenGSMTest {

	private static final int CONTAINER_CAPACITY = 256;
    private static final int HIGH_NUM_CPU = 8;
    private static final int LOW_NUM_CPU = 4;
    
    private static final int LOW_MEM_CAPACITY = 1024;
    private static final int HIGH_MEM_CAPACITY = 6000;
    
    private final int TEST_LEVEL = 3;
                     

    @Test(timeOut = 2*DEFAULT_TEST_TIMEOUT, groups = "5")
    public void doTest() {
    	         
	    // make sure no gscs yet created
	    assertEquals(0, getNumberOfGSCsAdded());
	    assertEquals(1, getNumberOfGSAsAdded());
	            
	    final ProcessingUnit pu = gsm.deploy(new ElasticSpaceDeployment("mygrid")
	            .maxMemoryCapacity(HIGH_MEM_CAPACITY, MemoryUnit.MEGABYTES)
	            .maxNumberOfCpuCores(HIGH_NUM_CPU)
	            .memoryCapacityPerContainer(CONTAINER_CAPACITY, MemoryUnit.MEGABYTES)
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig())
	            .scale((new ManualCapacityScaleConfigurer()
	            	   .numberOfCpuCores(LOW_NUM_CPU)
	            	   .memoryCapacity(LOW_MEM_CAPACITY,MemoryUnit.MEGABYTES)
	            	   .create()))
	    );

	    int expectedNumberOfMachines = (int)Math.ceil(LOW_NUM_CPU/super.getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine());
	    int expectedNumberOfContainers = (int) Math.max(
	    		Math.ceil(LOW_MEM_CAPACITY/(1.0*CONTAINER_CAPACITY)),
	    		expectedNumberOfMachines);
	    GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainers, expectedNumberOfMachines, 4*OPERATION_TIMEOUT);
	    
	    assertEquals("Number of GSCs added", expectedNumberOfContainers, getNumberOfGSCsAdded());
	    assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
	    assertEquals("Number of GSCs added", expectedNumberOfMachines, getNumberOfGSAsAdded());
	    assertEquals("Number of GSCs removed", 0, getNumberOfGSAsRemoved());

	    final int NUM_OF_POJOS = 1000;
	    GsmTestUtils.writeData(pu, NUM_OF_POJOS);	    
	    	    
	    // now spread 4 gscs on 4 machines	    
	    pu.scale(
	    		new ManualCapacityScaleConfigurer()
	    		.numberOfCpuCores(HIGH_NUM_CPU)
	    		.memoryCapacity(LOW_MEM_CAPACITY,MemoryUnit.MEGABYTES)
	    		.create());
	    	
	    int expectedNumberOfMachinesAfterScaleOut = (int)Math.ceil(HIGH_NUM_CPU/super.getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine());
	    GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainers, expectedNumberOfMachinesAfterScaleOut, 4*OPERATION_TIMEOUT);
	    
	    assertEquals("Number of GSCs added",   2+expectedNumberOfContainers, getNumberOfGSCsAdded());
	    assertEquals("Number of GSCs removed", 2, getNumberOfGSCsRemoved());	    	
	    assertEquals("Number of GSAs added",   expectedNumberOfMachinesAfterScaleOut, getNumberOfGSAsAdded());
	    assertEquals("Number of GSAs removed", 0, getNumberOfGSAsRemoved());
	    
	    // make sure all pojos handled by PUs
        assertEquals("Number of Person Pojos in space", NUM_OF_POJOS, GsmTestUtils.countData(pu));
	
	    if (TEST_LEVEL < 2) {
	    	return;
	    }
	    
	    // now shrink back to 2 machines	    
	    pu.scale(
	    		new ManualCapacityScaleConfigurer()
	    		.numberOfCpuCores(LOW_NUM_CPU)
	    		.memoryCapacity(LOW_MEM_CAPACITY,MemoryUnit.MEGABYTES)
	    		.create());
	    
	    GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainers, expectedNumberOfMachines, 4*OPERATION_TIMEOUT);
	    
	    assertEquals("Number of GSCs added",   4+expectedNumberOfContainers, getNumberOfGSCsAdded());
	    assertEquals("Number of GSCs removed", 4, getNumberOfGSCsRemoved());	    	
	    assertEquals("Number of GSAs added",   expectedNumberOfMachinesAfterScaleOut, getNumberOfGSAsAdded());
	    assertEquals("Number of GSAs removed", 2, getNumberOfGSAsRemoved());
	    	    
	    // make sure all pojos handled by PUs
        assertEquals("Number of Person Pojos in space", NUM_OF_POJOS, GsmTestUtils.countData(pu));
	
	    if (TEST_LEVEL < 3) {
	    	return;
	    }
        
	    // increase (by memory) to 4 machines   
	    pu.scale(
	    		new ManualCapacityScaleConfigurer()
	    		.memoryCapacity(HIGH_MEM_CAPACITY,MemoryUnit.MEGABYTES)
	    		.create());
	   
	    int expectedNumberOfContainersAfterScaleOut2 = (int) Math.ceil(HIGH_MEM_CAPACITY/(1.0*CONTAINER_CAPACITY));
	    int expectedNumberOfMachinesAfterScaleOut2 = 5;
	    GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainersAfterScaleOut2, expectedNumberOfMachinesAfterScaleOut2, 4*OPERATION_TIMEOUT);

	    // make sure all pojos handled by PUs
        assertEquals("Number of Person Pojos in space", NUM_OF_POJOS, GsmTestUtils.countData(pu));
    }
}