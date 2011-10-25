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

public class DedicatedStatelessManualXenCPUScaleOutTest extends AbstractXenGSMTest {

    @Test(timeOut = DEFAULT_TEST_TIMEOUT*2, groups = "1")
    public void doTest() throws IOException {
    	         
	    // make sure no gscs yet created
	    assertEquals(0, getNumberOfGSCsAdded());
	    assertEquals(1, getNumberOfGSAsAdded());
	            
	    int numberOfCpuCores = 4;
        
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
	    numberOfCpuCores = 8;
	    pu.scale(new ManualCapacityScaleConfigurer()
	    		 .numberOfCpuCores(numberOfCpuCores)
	    		 .create());
	    	    
	    int expectedNumberOfContainersAfterScaleOut = (int) Math.ceil(numberOfCpuCores/super.getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine());
	    int expectedNumberOfMachinesAfterScaleOut = expectedNumberOfContainersAfterScaleOut;
	    pu.waitFor(expectedNumberOfContainersAfterScaleOut);
	    
	    assertEquals("Number of GSCs added", expectedNumberOfContainersAfterScaleOut, getNumberOfGSCsAdded());
	    assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
	    assertEquals("Number of GSCs added", expectedNumberOfMachinesAfterScaleOut, getNumberOfGSAsAdded());
	    assertEquals("Number of GSCs removed", 0, getNumberOfGSAsRemoved());
	    
	}

}


