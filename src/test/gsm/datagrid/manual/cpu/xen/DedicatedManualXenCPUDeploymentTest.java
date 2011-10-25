package test.gsm.datagrid.manual.cpu.xen;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;

public class DedicatedManualXenCPUDeploymentTest extends AbstractXenGSMTest {
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void doTest4()  {
    	doTest(4);
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void doTest6()  {
    	doTest(6);
    }
    
    public void doTest(int numberOfCores)  {    	  
	    // make sure no gscs yet created
	    assertEquals(0, getNumberOfGSCsAdded());
	    assertEquals(1, getNumberOfGSAsAdded());  
	            
	    final ProcessingUnit pu = gsm.deploy(new ElasticSpaceDeployment("mygrid")
	    		.maxMemoryCapacity(1024, MemoryUnit.MEGABYTES)
	            .maxNumberOfCpuCores(8)
	            .memoryCapacityPerContainer(256, MemoryUnit.MEGABYTES)
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig())
	            .scale((new ManualCapacityScaleConfigurer()
	            		.numberOfCpuCores(numberOfCores)
	            		.create()))
	    );
	   
	    assertEquals(4,pu.getNumberOfInstances());
	    
	    GsmTestUtils.waitForScaleToComplete(pu, numberOfCores/2, numberOfCores/2, OPERATION_TIMEOUT);
	    
	    assertEquals("Number of GSCs added",   numberOfCores/2, getNumberOfGSCsAdded());
	    assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
	   
	    assertEquals("Number of GSAs added",   numberOfCores/2, getNumberOfGSAsAdded());
	    assertEquals("Number of GSAs removed", 0, getNumberOfGSAsRemoved());
	}
    
}


