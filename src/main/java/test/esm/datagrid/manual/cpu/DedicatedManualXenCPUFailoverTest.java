package test.gsm.datagrid.manual.cpu.xen;


import java.io.IOException;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfig;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;

public class DedicatedManualXenCPUFailoverTest extends AbstractXenGSMTest {
 
	private static final int CONTAINER_CAPACITY = 256;
    private static final int HIGH_NUM_CPU = 8;
    private static final int HIGH_MEM_CAPACITY = 2048;
        
    @Test(timeOut = DEFAULT_TEST_TIMEOUT*2, groups = "1")
    public void cpuThreeMachinesAndMemoryFailoverTest() {
    	doTest(1024,6);
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT*2, groups = "1")
    public void cpuThreeMachinesFailoverTest() {
    	doTest(0,6);
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void cpuAndMemoryFailoverTest() {
    	doTest(1024,4);
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void cpuFailoverTest() throws IOException {
    	doTest(0,4);
    }
    
    private void doTest(int memoryCapacity,int lowNumberOfCpus) {
    	          
	    // make sure no gscs yet created
    	repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);	    
	    
	    final int expectedNumberOfMachines = (int)  
    		Math.ceil(lowNumberOfCpus/super.getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine());

	    ManualCapacityScaleConfig scaleConfig = 
	    	new ManualCapacityScaleConfigurer()
	    	.numberOfCpuCores(lowNumberOfCpus)
			.create();
	    
	    int expectedNumberOfContainers; 
	    if (memoryCapacity == 0) {
	    	expectedNumberOfContainers = (int) Math.ceil(lowNumberOfCpus/super.getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine());
	    } else {
	    	scaleConfig.setMemoryCapacityInMB(memoryCapacity);
	    	expectedNumberOfContainers = (int) Math.ceil(memoryCapacity/CONTAINER_CAPACITY);
	    	if (expectedNumberOfContainers < expectedNumberOfMachines) {
	    		expectedNumberOfContainers = expectedNumberOfMachines;
	    	}
	    }
	    
		final ProcessingUnit pu = super.deploy(
				new ElasticSpaceDeployment("mygrid")
	            .maxMemoryCapacity(HIGH_MEM_CAPACITY, MemoryUnit.MEGABYTES)
	            .maxNumberOfCpuCores(HIGH_NUM_CPU)
	            .memoryCapacityPerContainer(CONTAINER_CAPACITY, MemoryUnit.MEGABYTES)
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig()).
	            // will allocate - as default - enough memory for 2 GSCs
	            scale(scaleConfig)
	    );
	    
	    
	    GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainers, expectedNumberOfMachines, OPERATION_TIMEOUT);
	    	    
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
	    
	    final int NUM_OF_POJOS = 1000;
	    
	    GsmTestUtils.writeData(pu, NUM_OF_POJOS);
                
        GridServiceContainer container = admin.getGridServiceContainers().getContainers()[0];
        GsmTestUtils.killContainer(container);
        
        GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainers, expectedNumberOfMachines, OPERATION_TIMEOUT);
     
        repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers+1, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(1, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
    		    
	    // make sure all pojos handled by PUs
        assertEquals("Number of Person Pojos in space", NUM_OF_POJOS, GsmTestUtils.countData(pu));   
        
        assertUndeployAndWait(pu);
	}
}