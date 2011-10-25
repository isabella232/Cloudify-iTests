package test.gsm.stateful.manual.memory;

import java.io.File;
import java.io.IOException;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractGsmTest;
import test.gsm.GsmTestUtils;
import test.utils.DeploymentUtils;


public class DedicatedManualStatefulScaleOutTest extends AbstractGsmTest {
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "3")
    public void testElasticStatefulProcessingUnitDeployment() throws IOException {
    	
    	admin.getGridServiceAgents().waitFor(3);

    	assertEquals("Number of GSCs added", 0, getNumberOfGSCsAdded());
    	assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
        
        DeploymentUtils.prepareApp("simpledata");
        File puDir = DeploymentUtils.getProcessingUnit("simpledata", "processor");        
                
        final ProcessingUnit pu = gsm.deploy(new ElasticStatefulProcessingUnitDeployment(puDir).
                maxMemoryCapacity(1, MemoryUnit.GIGABYTES).
                memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES));       
        
        pu.scale(new ManualCapacityScaleConfigurer().memoryCapacity(512, MemoryUnit.MEGABYTES).create());
       
        int expectedNumberOfContainers = 2;
		GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainers, OPERATION_TIMEOUT);
        
        assertEquals("Number of GSCs added", expectedNumberOfContainers, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());

        int numberOfPojos = 1000;
        GsmTestUtils.writeData(pu, numberOfPojos);
             
        pu.scale((new ManualCapacityScaleConfigurer().
                memoryCapacity(1, MemoryUnit.GIGABYTES).create()));
        
        int expectedNumberOfContainersAfterScaleOut = 4;
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainersAfterScaleOut, OPERATION_TIMEOUT);
        
        assertEquals("Number of GSCs added", expectedNumberOfContainersAfterScaleOut, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
        
        // make sure primary and backup not killed both
        assertEquals("Number of Person Pojos in space", numberOfPojos, GsmTestUtils.countData(pu));        
    }        
	
}


