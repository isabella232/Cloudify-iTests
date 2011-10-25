package test.gsm.stateless.manual.cpu.xen;

import java.io.File;
import java.io.IOException;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.utils.DeploymentUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.LogUtils;

public class DedicatedStatelessManualXenCPUScaleInTest extends AbstractXenGSMTest {

    @Test(timeOut = DEFAULT_TEST_TIMEOUT*2, groups = "1")
    public void doTest() throws IOException {
    	         
	    // make sure no gscs yet created
	    assertEquals(0, getNumberOfGSCsAdded());
	    assertEquals(1, getNumberOfGSAsAdded());
	    int numberOfCpuCores = 8;
	    
	    File archive = DeploymentUtils.getArchive("servlet.war");
        
        final ProcessingUnit pu = gsm.deploy(
                new ElasticStatelessProcessingUnitDeployment(archive)
                .memoryCapacityPerContainer(1, MemoryUnit.GIGABYTES)
                .dedicatedMachineProvisioning(getMachineProvisioningConfig()).

                scale(new ManualCapacityScaleConfigurer()
                      .numberOfCpuCores(numberOfCpuCores)
                      .create())
        );
        
	    int expectedNumberOfContainers = (int) Math.ceil(numberOfCpuCores/super.getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine());
	    int expectedNumberOfMachines = expectedNumberOfContainers;
	    
	    pu.waitFor(expectedNumberOfContainers);
	    
	    assertEquals("Number of GSCs added", expectedNumberOfContainers, getNumberOfGSCsAdded());
	    assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
	    assertEquals("Number of GSCs added", expectedNumberOfMachines, getNumberOfGSAsAdded());
	    assertEquals("Number of GSCs removed", 0, getNumberOfGSAsRemoved());
	    	    
	    // compress 4 gscs into 2 machines	    
	    numberOfCpuCores = 4;
	    pu.scale(new ManualCapacityScaleConfigurer()
	    		 .numberOfCpuCores(numberOfCpuCores)
	    		 .create());
	    	    
	    final int expectedNumberOfContainersAfterScaleIn = (int) Math.ceil(numberOfCpuCores/super.getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine());
	    final int expectedRemoved = expectedNumberOfContainers - expectedNumberOfContainersAfterScaleIn;

	    repetitiveAssertTrue("Scaling in two instances",new RepetitiveConditionProvider() {

            public boolean getCondition() {
            	boolean condition = true;
            	int actualNumberOfInstances = pu.getInstances().length;
				if (actualNumberOfInstances != expectedNumberOfContainersAfterScaleIn) {
            		LogUtils.log("Waiting for " + expectedNumberOfContainersAfterScaleIn + " instances, currently " + actualNumberOfInstances + " instances.");
            		condition = false;
            	}

            	int actualContainersRemoved = getNumberOfGSCsRemoved();
            	if (actualContainersRemoved != expectedRemoved) {
            		LogUtils.log("Waiting for " + expectedRemoved + " removed containers, currently " + actualContainersRemoved + " removed containers.");
            		condition = false;
            	}
                
            	int actualAgentsRemoved = getNumberOfGSAsRemoved();
				if (actualAgentsRemoved != expectedRemoved) {
					LogUtils.log("Waiting for " + expectedRemoved + " removed agents, currently " + actualAgentsRemoved + " removed agents.");
					condition = false;
            	}
				return condition;
            }},
            OPERATION_TIMEOUT);
	    
	    assertEquals("Number of GSCs added", expectedNumberOfContainers, getNumberOfGSCsAdded());
	    assertEquals("Number of GSCs removed", expectedRemoved , getNumberOfGSCsRemoved());
	    assertEquals("Number of GSCs added", expectedNumberOfMachines, getNumberOfGSAsAdded());
	    assertEquals("Number of GSCs removed", expectedRemoved, getNumberOfGSAsRemoved());
	    
	}

}


