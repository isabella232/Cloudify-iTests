package test.gsm.stateless.manual.cpu.xen;

import java.io.File;
import java.io.IOException;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import framework.utils.DeploymentUtils;

import test.gsm.AbstractXenGSMTest;

public class DedicatedStatelessManualXenCPUScaleOutTest extends AbstractXenGSMTest {

    @Test(timeOut = DEFAULT_TEST_TIMEOUT*2, groups = "1")
    public void doTest() throws IOException {
    	         
	    // make sure no gscs yet created
	    repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
	            
	    int numberOfCpuCores = 4;
        
        File archive = DeploymentUtils.getArchive("servlet.war");
        
        final ProcessingUnit pu = super.deploy(
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
	    
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);

	    	    
	    // compress 4 gscs into 2 machines
	    numberOfCpuCores = 8;
	    pu.scale(new ManualCapacityScaleConfigurer()
	    		 .numberOfCpuCores(numberOfCpuCores)
	    		 .create());
	    	    
	    int expectedNumberOfContainersAfterScaleOut = (int) Math.ceil(numberOfCpuCores/super.getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine());
	    int expectedNumberOfMachinesAfterScaleOut = expectedNumberOfContainersAfterScaleOut;
	    pu.waitFor(expectedNumberOfContainersAfterScaleOut);
	    
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainersAfterScaleOut, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachinesAfterScaleOut, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
	    
	    assertUndeployAndWait(pu);
	}

}


