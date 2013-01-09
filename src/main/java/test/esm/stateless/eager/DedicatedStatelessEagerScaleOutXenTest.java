package test.gsm.stateless.eager.xen;

import java.io.File;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.core.util.MemoryUnit;
import org.openspaces.grid.gsm.rebalancing.RebalancingUtils;
import org.testng.annotations.Test;

import framework.utils.DeploymentUtils;

import test.gsm.AbstractXenGSMTest;

public class DedicatedStatelessEagerScaleOutXenTest extends AbstractXenGSMTest {

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
						.create())
				.scale(new EagerScaleConfig())
        );
        
        pu.waitFor(getNumberOfMachines());
        Assert.assertEquals(2,getNumberOfMachinesWithContainers());
        
        startNewVM(OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
        pu.waitFor(getNumberOfMachines());
        Assert.assertEquals(3,getNumberOfMachinesWithContainers());
        
        startNewVM(OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
        pu.waitFor(getNumberOfMachines());
        Assert.assertEquals(4,getNumberOfMachinesWithContainers());
        
        assertUndeployAndWait(pu);
    }

    private int getNumberOfMachines() {
        return admin.getGridServiceAgents().getSize();
    }

    private int getNumberOfMachinesWithContainers() {
        return RebalancingUtils.getMachinesHostingContainers(admin.getGridServiceContainers().getContainers()).length;
    }

}