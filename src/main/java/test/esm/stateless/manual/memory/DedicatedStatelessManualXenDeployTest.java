package test.gsm.stateless.manual.memory.xen;

import java.io.File;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import framework.utils.DeploymentUtils;

import test.gsm.AbstractXenGSMTest;

public class DedicatedStatelessManualXenDeployTest extends AbstractXenGSMTest {

	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void test() {
	    File archive = DeploymentUtils.getArchive("servlet.war");
	    
		final ProcessingUnit pu = super.deploy(
				new ElasticStatelessProcessingUnitDeployment(archive)
	            .memoryCapacityPerContainer(1, MemoryUnit.GIGABYTES)
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig())
	            .scale(new ManualCapacityScaleConfigurer()
	            	  .memoryCapacity(2, MemoryUnit.GIGABYTES)
                      .create())
	    );
	    
	    pu.waitFor(2);
	    	    
	    repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
	    
	    assertUndeployAndWait(pu);
	}
}
