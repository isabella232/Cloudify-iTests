package test.gsm.stateful.manual.memory;


import java.io.File;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractGsmTest;
import test.gsm.GsmTestUtils;
import test.utils.DeploymentUtils;

//zzzz gilad new 
public class DedicatedManualStatefulFailoverTest extends AbstractGsmTest {
	
	// gilad - newly written test
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "3")
    public void testElasticStatefulProcessingUnitDeployment() {

    	admin.getGridServiceAgents().waitFor(3);

    	assertEquals(0, getNumberOfGSCsAdded());
        assertEquals(0, getNumberOfGSCsRemoved());
        
        DeploymentUtils.prepareApp("simpledata");
        File puDir = DeploymentUtils.getProcessingUnit("simpledata", "processor");        
                
        final ProcessingUnit pu = gsm.deploy(new ElasticStatefulProcessingUnitDeployment(puDir).
                maxMemoryCapacity(1, MemoryUnit.GIGABYTES).
                memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES));       
        
        pu.scale(new ManualCapacityScaleConfigurer().memoryCapacity(1, MemoryUnit.GIGABYTES).create());
       
        int expectedNumberOfContainers = 4;
		GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainers, OPERATION_TIMEOUT);
        
        assertEquals("Number of GSCs added", expectedNumberOfContainers, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());

        int numberOfPojos = 1000;
        GsmTestUtils.writeData(pu, numberOfPojos);

        GridServiceContainer[] containers = admin.getGridServiceContainers().getContainers();
        GsmTestUtils.killContainer(containers[0]);
        
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainers, OPERATION_TIMEOUT);
        
        assertEquals("Number of GSCs added + 1 failover recovery", expectedNumberOfContainers+1, getNumberOfGSCsAdded());
        assertEquals("Only one new container is brought up", 1, getNumberOfGSCsRemoved());
        
        // make sure primary and backup not killed both
        assertEquals("Number of Person Pojos in space", numberOfPojos, GsmTestUtils.countData(pu));
   }    
}


