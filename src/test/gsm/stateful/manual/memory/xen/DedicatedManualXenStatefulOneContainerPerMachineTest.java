package test.gsm.stateful.manual.memory.xen;

import java.io.File;
import java.io.IOException;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;
import test.utils.DeploymentUtils;

public class DedicatedManualXenStatefulOneContainerPerMachineTest extends AbstractXenGSMTest {

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitDeployment() throws IOException {
    
        assertEquals(0, getNumberOfGSCsAdded());
        assertEquals(1, getNumberOfGSAsAdded());
         
        DeploymentUtils.prepareApp("simpledata");
        File puDir = DeploymentUtils.getProcessingUnit("simpledata", "processor");
        
        final ProcessingUnit pu = gsm.deploy(
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

        assertEquals("Number of GSAs added", expectedNumberOfMachines, getNumberOfGSAsAdded());
        assertEquals("Number of GSAs removed", 0, getNumberOfGSAsRemoved());
        assertEquals("Number of GSCs added", expectedNumberOfContainers, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
        
		for (GridServiceAgent gsa : admin.getGridServiceAgents()) {
			int containersOnMachine = gsa.getMachine().getGridServiceContainers().getSize();
			assertEquals(1,containersOnMachine);
		}

       
    } 
}
