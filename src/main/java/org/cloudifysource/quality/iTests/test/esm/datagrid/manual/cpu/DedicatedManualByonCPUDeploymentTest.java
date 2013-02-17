package org.cloudifysource.quality.iTests.test.esm.datagrid.manual.cpu;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.framework.utils.GsmTestUtils;

import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;

public class DedicatedManualByonCPUDeploymentTest extends AbstractFromXenToByonGSMTest{
	
	private static final int MEMORY_CAPACITY_PER_CONTAINER = 256;
	private static final int MAX_NUMBER_OF_MACHINES = 4;
	
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
    public void doTest2()  {
		int numberOfMachines=2;
    	doTest(numberOfMachines *NUM_OF_CORES );
    }
    
	//TODO find a way to run few tests in one class - for now only one test works
	
    /*@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void doTest3()  {
		int numberOfMachines=3;
    	doTest(numberOfMachines *NUM_OF_CORES );
    }*/
    
    public void doTest(int numberOfCores)  {    	  
	    // make sure no gscs yet created
    	repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
	            
	    final ProcessingUnit pu = super.deploy(new ElasticSpaceDeployment("mygrid")
	    		.maxMemoryCapacity(MEMORY_CAPACITY_PER_CONTAINER * MAX_NUMBER_OF_MACHINES, MemoryUnit.MEGABYTES)
	            .maxNumberOfCpuCores(NUM_OF_CORES * MAX_NUMBER_OF_MACHINES)
	            .memoryCapacityPerContainer(MEMORY_CAPACITY_PER_CONTAINER, MemoryUnit.MEGABYTES)
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig())
	            .scale((new ManualCapacityScaleConfigurer()
	            		.numberOfCpuCores(numberOfCores)
	            		.create()))
	    );
	    
	    int expectedNumberOfPartitions = MAX_NUMBER_OF_MACHINES;
		assertEquals(expectedNumberOfPartitions,pu.getNumberOfInstances());
	    
	    int expectedNumberOfMachines = numberOfCores/NUM_OF_CORES;
	    int expectedNumberOfContainers = numberOfCores/NUM_OF_CORES;
		GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainers, expectedNumberOfMachines, OPERATION_TIMEOUT);
	    
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);   
    	
    	assertUndeployAndWait(pu);
    }

}
