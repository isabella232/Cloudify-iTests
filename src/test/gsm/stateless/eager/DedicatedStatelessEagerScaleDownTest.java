package test.gsm.stateless.eager;

import java.io.File;

import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractGsmTest;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.DeploymentUtils;

public class DedicatedStatelessEagerScaleDownTest extends AbstractGsmTest {
    
	/**
	 * Tests scale down by applying atMostOneContainerPerMachine to eager scale
	 * See GS-9241
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	void testScaleDown() {
		super.assertEquals( 0, admin.getGridServiceContainers().getSize() ); 
		
	    File archive = DeploymentUtils.getArchive("servlet.war");
	    
		final ProcessingUnit pu = gsm.deploy(
	            new ElasticStatelessProcessingUnitDeployment(archive)
	            .memoryCapacityPerContainer(512, MemoryUnit.MEGABYTES)
	
	            .dedicatedMachineProvisioning(
						new DiscoveredMachineProvisioningConfigurer()
						.reservedMemoryCapacityPerMachine(128, MemoryUnit.MEGABYTES)
						.create())
				.scale(new EagerScaleConfig())
	    );
	    
		RepetitiveConditionProvider atLeastOneMachineWithMoreThanOneContainer = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				for (Machine m : admin.getMachines()) {
					if (m.getGridServiceContainers().getSize() > 1) {
						return true;
					}
				}
				return false;
			}
		};
		
		RepetitiveConditionProvider atMostOneContainerPerMachine = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				for (Machine m : admin.getMachines()) {
					if (m.getGridServiceContainers().getSize() > 1) {
						return false;
					}
				}
				return true;
			}
		};
		super.repetitiveAssertTrue("timed out waiting for eager at least one machine with more than one container", atLeastOneMachineWithMoreThanOneContainer, OPERATION_TIMEOUT);
	    pu.scale(new EagerScaleConfigurer().atMostOneContainerPerMachine().create());
	    
	    super.repetitiveAssertTrue("timed out waiting for eager at most one container per machine", atMostOneContainerPerMachine, OPERATION_TIMEOUT);
	}
    
}
