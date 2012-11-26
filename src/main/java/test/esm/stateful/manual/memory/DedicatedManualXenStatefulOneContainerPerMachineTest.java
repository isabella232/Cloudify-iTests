package test.esm.stateful.manual.memory;

import java.io.File;
import java.io.IOException;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.esm.AbstractFromXenToByonGSMTest;
import framework.utils.DeploymentUtils;
import framework.utils.GsmTestUtils;

public class DedicatedManualXenStatefulOneContainerPerMachineTest extends AbstractFromXenToByonGSMTest {

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitDeployment() throws IOException {
    
        repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
         
        DeploymentUtils.prepareApp("simpledata");
        File puDir = DeploymentUtils.getProcessingUnit("simpledata", "processor");
        
        final ProcessingUnit pu = super.deploy(
        		new ElasticStatefulProcessingUnitDeployment(puDir)
				.maxMemoryCapacity(256*2, MemoryUnit.MEGABYTES)
				.memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES)
				.dedicatedMachineProvisioning(getMachineProvisioningConfig())
				.singleMachineDeployment()
				.scale(
						new ManualCapacityScaleConfigurer()
						.memoryCapacity(512, MemoryUnit.MEGABYTES)
						.atMostOneContainerPerMachine()
						.create()));

		int expectedNumberOfContainers = 2;
        int expectedNumberOfMachines = 2;
		GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainers, expectedNumberOfMachines, OPERATION_TIMEOUT);

        repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
        
		for (GridServiceAgent gsa : admin.getGridServiceAgents()) {
			int containersOnMachine = gsa.getMachine().getGridServiceContainers().getSize();
			assertEquals(1,containersOnMachine);
		}

		assertUndeployAndWait(pu);
    } 
}
