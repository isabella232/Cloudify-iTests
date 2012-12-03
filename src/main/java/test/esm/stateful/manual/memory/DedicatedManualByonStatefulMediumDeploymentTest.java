package test.esm.stateful.manual.memory;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.esm.AbstractFromXenToByonGSMTest;
import framework.utils.GsmTestUtils;

public class DedicatedManualByonStatefulMediumDeploymentTest extends AbstractFromXenToByonGSMTest {

	private static final int CONTAINER_CAPACITY = 256;
    private static final int HIGH_NUM_CPU = 8;
    private static final int HIGH_MEM_CAPACITY = 6000;

    @Test(timeOut=DEFAULT_TEST_TIMEOUT*2 , groups = "1" )
	public void largeDeploymentTest() {
	
	    // make sure no gscs yet created
	    repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
	            
	    final ProcessingUnit pu = super.deploy(
	            new ElasticSpaceDeployment("mygrid")
	            .maxMemoryCapacity(HIGH_MEM_CAPACITY, MemoryUnit.MEGABYTES)
	            .maxNumberOfCpuCores(HIGH_NUM_CPU)
	            .memoryCapacityPerContainer(CONTAINER_CAPACITY, MemoryUnit.MEGABYTES)
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig())
	            .scale(new ManualCapacityScaleConfigurer()
	            	  .memoryCapacity(HIGH_MEM_CAPACITY,MemoryUnit.MEGABYTES)
	            	  .create())
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig())
	    );
	    
	    int expectedNumberOfContainers = (int) Math.ceil(HIGH_MEM_CAPACITY/(1.0*CONTAINER_CAPACITY));
		int expectedNumberOfMachines = 5;
	    GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainers, expectedNumberOfMachines, 2*DEFAULT_TEST_TIMEOUT);

	    assertUndeployAndWait(pu);
   }
}
