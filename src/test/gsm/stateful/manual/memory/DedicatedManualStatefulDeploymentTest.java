package test.gsm.stateful.manual.memory;


import java.io.File;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.gsm.AbstractGsmTest;
import test.gsm.GsmTestUtils;
import test.utils.AssertUtils;
import test.utils.DeploymentUtils;

public class DedicatedManualStatefulDeploymentTest extends AbstractGsmTest {

	private static final int CONTAINER_CAPACITY = 256;
	private static final int MAX_MEMORY_CAPACITY = 512;
	File puDir ;
	
	@Override 
    @BeforeMethod
    public void beforeTest() {
    	super.beforeTest();

        DeploymentUtils.prepareApp("simpledata");
        puDir = DeploymentUtils.getProcessingUnit("simpledata", "processor");
    }
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
    public void testElasticStatefulProcessingUnitDeployment() {

    	admin.getGridServiceAgents().waitFor(2); 
        assertEquals(0, getNumberOfGSCsAdded());
                
        final ProcessingUnit pu = gsm.deploy(
                new ElasticStatefulProcessingUnitDeployment(puDir)
                .maxMemoryCapacity(MAX_MEMORY_CAPACITY, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(CONTAINER_CAPACITY,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(512,MemoryUnit.MEGABYTES)
                       .create()));
        
        completeDeploymentMemoryTest(pu);

    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitDeploymentOnSingleMachine() {

    	admin.getGridServiceAgents().waitFor(1); 
    	
        assertEquals(0, getNumberOfGSCsAdded());
        
        final ProcessingUnit pu = gsm.deploy(
                new ElasticStatefulProcessingUnitDeployment(puDir)
                .maxMemoryCapacity(MAX_MEMORY_CAPACITY, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(CONTAINER_CAPACITY,MemoryUnit.MEGABYTES)
                .singleMachineDeployment()
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(512,MemoryUnit.MEGABYTES)
                       .create())
        );
        
        completeDeploymentMemoryTest(pu);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitDeploymentOnSingleMachineNoMemoryCapacity() {

    	admin.getGridServiceAgents().waitFor(1); 
    	
        assertEquals(0, getNumberOfGSCsAdded());
        
        final ProcessingUnit pu = gsm.deploy(
                new ElasticStatefulProcessingUnitDeployment(puDir)
                .maxMemoryCapacity(512, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfig())
                .singleMachineDeployment()
        );
        
        completeDeploymentMemoryTest(pu);

    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitDeploymentOnSingleMachineNoScale() {

    	admin.getGridServiceAgents().waitFor(1); 
    	
        assertEquals(0, getNumberOfGSCsAdded());
        
        final ProcessingUnit pu = gsm.deploy(new ElasticStatefulProcessingUnitDeployment(puDir).
                maxMemoryCapacity(MAX_MEMORY_CAPACITY, MemoryUnit.MEGABYTES).
                memoryCapacityPerContainer(CONTAINER_CAPACITY,MemoryUnit.MEGABYTES).
                singleMachineDeployment()
        );
        
        completeDeploymentMemoryTest(pu);

    }


    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitDeploymentOnSingleMachineCpuScale() {

    	admin.getGridServiceAgents().waitFor(1); 
    	
        assertEquals(0, getNumberOfGSCsAdded());
        int cores = 1;
        final ProcessingUnit pu = gsm.deploy(
                new ElasticStatefulProcessingUnitDeployment(puDir)
                .maxMemoryCapacity(MAX_MEMORY_CAPACITY, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(CONTAINER_CAPACITY,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .numberOfCpuCores(cores)
                       .create())
                .singleMachineDeployment()
        );
        
        completeDeploymentCpuTest(pu);

    }
    private void completeDeploymentMemoryTest(final ProcessingUnit pu) {
    	int expectedNumberOfContainers = 2;
		GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainers, OPERATION_TIMEOUT);
    	completeDeploymentTest(pu);
    }
    
    private void completeDeploymentCpuTest(final ProcessingUnit pu) {
    	int expectedNumberOfContainers = 2;
		GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainers, OPERATION_TIMEOUT);
    	completeDeploymentTest(pu);
    }
    private void completeDeploymentTest(final ProcessingUnit pu) {
    	
		assertEquals("Number of GSCs added", 2, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());

        // undeploy
        pu.undeployAndWait(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        
        assertEquals("Number of GSCs discovered", 0, admin.getGridServiceContainers().getSize());
        assertEquals("PU undeployed", 0, pu.getInstances().length);
        AssertUtils.assertNull("PU undiscovered", admin.getProcessingUnits().getProcessingUnit(pu.getName()));
        assertEquals("Number of GSCs added", 2, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 2, getNumberOfGSCsRemoved());
    }
    
}
