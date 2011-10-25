package test.gsm.stateless.manual.memory.xen;

import java.io.File;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.utils.DeploymentUtils;

public class DedicatedStatelessManualXenDeployTest extends AbstractXenGSMTest {

	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void test() {
	    File archive = DeploymentUtils.getArchive("servlet.war");
	    
		final ProcessingUnit pu = gsm.deploy(
				new ElasticStatelessProcessingUnitDeployment(archive)
	            .memoryCapacityPerContainer(1, MemoryUnit.GIGABYTES)
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig())
	            .scale(new ManualCapacityScaleConfigurer()
	            	  .memoryCapacity(2, MemoryUnit.GIGABYTES)
                      .create())
	    );
	    
	    pu.waitFor(2);
	    	    
	    assertEquals("Number of GSCs added", 2, getNumberOfGSCsAdded());
	    assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
	    assertEquals("Number of GSAs added", 2, getNumberOfGSCsAdded());
	    assertEquals("Number of GSAs removed", 0, getNumberOfGSCsRemoved());
	}
}
