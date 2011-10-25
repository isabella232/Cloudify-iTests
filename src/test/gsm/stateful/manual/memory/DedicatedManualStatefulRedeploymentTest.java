package test.gsm.stateful.manual.memory;

import java.io.File;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.gsm.AbstractGsmTest;
import test.gsm.GsmTestUtils;
import test.utils.AssertUtils;
import test.utils.DeploymentUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;

public class DedicatedManualStatefulRedeploymentTest extends AbstractGsmTest {

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
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitReDeploymentOnSingleMachine() {
        assertEquals(0, getNumberOfGSCsAdded());
        {   
        final ProcessingUnit pu = gsm.deploy(new ElasticStatefulProcessingUnitDeployment(puDir).
                maxMemoryCapacity(MAX_MEMORY_CAPACITY, MemoryUnit.MEGABYTES).
                memoryCapacityPerContainer(CONTAINER_CAPACITY,MemoryUnit.MEGABYTES).
                scale(new ManualCapacityScaleConfigurer().memoryCapacity(512,MemoryUnit.MEGABYTES).create())
                .singleMachineDeployment()
        );
        
        int expectedNumberOfContainers = 2;
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainers, OPERATION_TIMEOUT);
        assertEquals("Number of GSCs added", 2, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
        
        // undeploy
        pu.undeploy();
        final RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
            public boolean getCondition() {
                return admin.getGridServiceContainers().isEmpty();
            }
        };
        AssertUtils.repetitiveAssertTrue("Waiting for undeploy to complete", condition, OPERATION_TIMEOUT);
        
        assertEquals("Number of GSCs added", 2, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 2, getNumberOfGSCsRemoved());
        }
        
        //redeploy
        final ProcessingUnit pu = gsm.deploy(new ElasticStatefulProcessingUnitDeployment(puDir).
                maxMemoryCapacity(MAX_MEMORY_CAPACITY, MemoryUnit.MEGABYTES).
                memoryCapacityPerContainer(CONTAINER_CAPACITY,MemoryUnit.MEGABYTES).
                scale(new ManualCapacityScaleConfigurer().memoryCapacity(512,MemoryUnit.MEGABYTES).create())
                .singleMachineDeployment()
        );
        
        int expectedNumberOfContainers = 2;
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainers, OPERATION_TIMEOUT);
        
        assertEquals("Number of GSCs added", 4, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 2, getNumberOfGSCsRemoved());
        
        // undeploy
        pu.undeploy();
        final RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
            public boolean getCondition() {
                return admin.getGridServiceContainers().isEmpty();
            }
        };
        AssertUtils.repetitiveAssertTrue("Waiting for undeploy to complete", condition, OPERATION_TIMEOUT);
        
        assertEquals("Number of GSCs added", 4, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 4, getNumberOfGSCsRemoved());
    }
}
