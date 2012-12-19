package test.gsm.stateless.manual.cpu.xen;


import java.io.File;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import framework.utils.DeploymentUtils;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;

public class DedicatedStatelessManualXenCPUFailoverTest extends AbstractXenGSMTest {

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void doTest() {
    	                  
        int numberOfCpuCores = 6;
        
	    // make sure no gscs yet created
	    repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);	    
	    
	    final int expectedNumberOfMachines = (int)  
    		Math.ceil(numberOfCpuCores/super.getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine());

	    int expectedNumberOfContainers = (int) Math.ceil(numberOfCpuCores/super.getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine());
	    
	    File archive = DeploymentUtils.getArchive("servlet.war");
	    
		final ProcessingUnit pu = super.deploy(
				new ElasticStatelessProcessingUnitDeployment(archive)
	            .memoryCapacityPerContainer(1, MemoryUnit.GIGABYTES)
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig()).

	            scale(new ManualCapacityScaleConfigurer()
                      .numberOfCpuCores(numberOfCpuCores)
                      .create())
	    );
	    
	    pu.waitFor(expectedNumberOfContainers);
	    	    
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
	    
               
        GridServiceContainer container = admin.getGridServiceContainers().getContainers()[0];
        GsmTestUtils.killContainer(container);
        
        pu.waitFor(expectedNumberOfContainers);
        
 
	    repetitiveAssertNumberOfGSCsRemoved(1, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers+1, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
	    
	    assertUndeployAndWait(pu);
	    
	}
}