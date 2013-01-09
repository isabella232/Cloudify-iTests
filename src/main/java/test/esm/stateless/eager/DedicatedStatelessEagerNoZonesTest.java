package test.gsm.stateless.eager.xen;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import framework.utils.DeploymentUtils;

/**
 * This test tries to deploy an Elastic PU with a zone, on a GSA that has no zones.
 * Starting from XAP 9.1.0 this scenario is not supported and the deployment is expected to fail.
 * @see GS-9721
 * @since 9.1.0
 * @author Itai Frenkel
 *
 */
public class DedicatedStatelessEagerNoZonesTest extends AbstractXenGSMTest {

    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1")
    public void doTest() {
        
        startNewVM(OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
        
        File archive = DeploymentUtils.getArchive("servlet.war");
        
        final ProcessingUnit pu = super.deploy(
                new ElasticStatelessProcessingUnitDeployment(archive)
                .memoryCapacityPerContainer(1, MemoryUnit.GIGABYTES)
 
                .dedicatedMachineProvisioning(
						new DiscoveredMachineProvisioningConfigurer()
						.reservedMemoryCapacityPerMachine(128, MemoryUnit.MEGABYTES)
						.addGridServiceAgentZone("myzone")
						.create())
				.scale(new EagerScaleConfig())
        );

        repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGridServiceContainersHolds(0,0, 30, TimeUnit.SECONDS);
        
        assertUndeployAndWait(pu);
    }
}
