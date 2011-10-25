package test.gsm.datagrid.manual.cpu.xen;

import java.io.IOException;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;

public class DedicatedManualXenCPUScaleInTest extends AbstractXenGSMTest {

    private static final int HIGH_NUM_CPU = 8;
    private static final int LOW_NUM_CPU = 4;
    
    private static final int HIGH_MEM_CAPACITY = 1024;
                         

    @Test(timeOut = DEFAULT_TEST_TIMEOUT*2, groups = "1")
    public void doTest() throws IOException {
    	         
	    // make sure no gscs yet created
	    assertEquals(0, getNumberOfGSCsAdded());
	    assertEquals(1, getNumberOfGSAsAdded());
	            
	    final ProcessingUnit pu = gsm.deploy(
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

	    int expectedNumberOfContainers = (int) Math.ceil(HIGH_NUM_CPU/super.getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine());
	    int expectedNumberOfMachines = expectedNumberOfContainers;
	    
	    GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainers, expectedNumberOfMachines, OPERATION_TIMEOUT*2);
	    
	    assertEquals("Number of GSCs added", expectedNumberOfContainers, getNumberOfGSCsAdded());
	    assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
	    assertEquals("Number of GSCs added", expectedNumberOfMachines, getNumberOfGSAsAdded());
	    assertEquals("Number of GSCs removed", 0, getNumberOfGSAsRemoved());

	    final int NUM_OF_POJOS = 1000;
	    GsmTestUtils.writeData(pu, NUM_OF_POJOS);
	    	    
	    // compress 4 gscs into 2 machines	    
	    pu.scale(new ManualCapacityScaleConfigurer()
	    		 .numberOfCpuCores(LOW_NUM_CPU)
	    		 .create());
	    	    
	    int expectedNumberOfContainersAfterScaleIn = (int) Math.ceil(LOW_NUM_CPU/super.getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine());
	    int expectedNumberOfMachinesAfterScaleIn = expectedNumberOfContainersAfterScaleIn;
	    int expectedRemoved = expectedNumberOfContainers - expectedNumberOfContainersAfterScaleIn;
	    GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainersAfterScaleIn, expectedNumberOfMachinesAfterScaleIn, OPERATION_TIMEOUT);
	    
	    assertEquals("Number of GSCs added", expectedNumberOfContainers, getNumberOfGSCsAdded());
	    assertEquals("Number of GSCs removed", expectedRemoved , getNumberOfGSCsRemoved());
	    assertEquals("Number of GSCs added", expectedNumberOfMachines, getNumberOfGSAsAdded());
	    assertEquals("Number of GSCs removed", expectedRemoved, getNumberOfGSAsRemoved());
	    
	    // make sure all pojos handled by PUs
        assertEquals("Number of Person Pojos in space", NUM_OF_POJOS, GsmTestUtils.countData(pu));
	}

}


