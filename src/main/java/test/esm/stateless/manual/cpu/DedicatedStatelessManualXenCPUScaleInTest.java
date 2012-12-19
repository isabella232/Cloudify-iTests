package test.gsm.stateless.manual.cpu.xen;

import java.io.File;
import java.io.IOException;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.gsm.AbstractXenGSMTest;

public class DedicatedStatelessManualXenCPUScaleInTest extends AbstractXenGSMTest {

    @Test(timeOut = DEFAULT_TEST_TIMEOUT*2, groups = "1")
    public void doTest() throws IOException {
    	         
	    // make sure no gscs yet created
	    repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
	    int numberOfCpuCores = 8;
	    
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
				return condition;
            }
	    },OPERATION_TIMEOUT);
	        
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSCsRemoved(expectedRemoved , OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsRemoved(expectedRemoved, OPERATION_TIMEOUT);
	    
	    assertUndeployAndWait(pu);
	}

}


