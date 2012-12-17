package test.esm.datagrid.manual.cpu;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.esm.AbstractFromXenToByonGSMTest;
import framework.utils.GsmTestUtils;

public class DedicatedManualXenCPUDeploymentTest extends AbstractFromXenToByonGSMTest {
    
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
    	repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
	            
	    final ProcessingUnit pu = super.deploy(new ElasticSpaceDeployment("mygrid")
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
	    
	    repetitiveAssertNumberOfGSCsAdded(numberOfCores/2, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(numberOfCores/2, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);   
    	
    	assertUndeployAndWait(pu);
    }
}


