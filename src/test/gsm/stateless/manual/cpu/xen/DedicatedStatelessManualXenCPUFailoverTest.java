package test.gsm.stateless.manual.cpu.xen;


import java.io.File;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;
import test.utils.DeploymentUtils;

public class DedicatedStatelessManualXenCPUFailoverTest extends AbstractXenGSMTest {

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void doTest() {
    	                  
        int numberOfCpuCores = 6;
        
	    // make sure no gscs yet created
	    assertEquals(0, getNumberOfGSCsAdded());
	    assertEquals(1, getNumberOfGSAsAdded());	    
	    
	    final int expectedNumberOfMachines = (int)  
    		Math.ceil(numberOfCpuCores/super.getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine());

	    int expectedNumberOfContainers = (int) Math.ceil(numberOfCpuCores/super.getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine());
	    
	    File archive = DeploymentUtils.getArchive("servlet.war");
	    
		final ProcessingUnit pu = gsm.deploy(
				new ElasticStatelessProcessingUnitDeployment(archive)
	            .memoryCapacityPerContainer(1, MemoryUnit.GIGABYTES)
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig()).

	            scale(new ManualCapacityScaleConfigurer()
                      .numberOfCpuCores(numberOfCpuCores)
                      .create())
	    );
	    
	    pu.waitFor(expectedNumberOfContainers);
	    	    
	    assertEquals("Number of GSCs added", expectedNumberOfContainers, getNumberOfGSCsAdded());
	    assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
	    
               
        GridServiceContainer container = admin.getGridServiceContainers().getContainers()[0];
        GsmTestUtils.killContainer(container);
        
        pu.waitFor(expectedNumberOfContainers);
        
 
	    assertEquals("Number of GSCs removed", 1, getNumberOfGSCsRemoved());	    	
	    assertEquals("Number of GSCs added",   expectedNumberOfContainers+1, getNumberOfGSCsAdded());
	    assertEquals("Number of GSAs added",   expectedNumberOfMachines, getNumberOfGSAsAdded());
	    assertEquals("Number of GSAs removed", 0, getNumberOfGSAsRemoved());
	    
	}
}