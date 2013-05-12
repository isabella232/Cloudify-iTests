package org.cloudifysource.quality.iTests.test.esm.stateless.eager;

import java.io.File;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import iTests.framework.utils.DeploymentUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.openspaces.grid.gsm.rebalancing.RebalancingUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DedicatedStatelessEagerScaleOutByonTest extends AbstractFromXenToByonGSMTest {
	
	@BeforeMethod
    public void beforeTest() {
		super.beforeTestInit();
	}
	
	@BeforeClass
	protected void bootstrap() throws Exception {
		super.bootstrapBeforeClass();
	}
	
	@AfterMethod
    public void afterTest() {
		super.afterTest();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardownAfterClass() throws Exception {
		super.teardownAfterClass();
	}
	
    @Test(timeOut= AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups="1")
    public void doTest() throws Exception {
        
    	repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
        startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
        File archive = DeploymentUtils.getArchive("servlet.war");
        
        final ProcessingUnit pu = super.deploy(
                new ElasticStatelessProcessingUnitDeployment(archive)
                .memoryCapacityPerContainer(1, MemoryUnit.GIGABYTES)
 
                .dedicatedMachineProvisioning(
						new DiscoveredMachineProvisioningConfigurer()
						.reservedMemoryCapacityPerMachine(128, MemoryUnit.MEGABYTES)
						.create())
				.scale(new EagerScaleConfigurer().atMostOneContainerPerMachine().create())
        );
        
        pu.waitFor(getNumberOfMachines());
        Assert.assertEquals(2,getNumberOfMachinesWithContainers());
        
        startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        repetitiveAssertNumberOfGSAsAdded(3, OPERATION_TIMEOUT);
        
        pu.waitFor(getNumberOfMachines());
        Assert.assertEquals(3,getNumberOfMachinesWithContainers());
        
        startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        repetitiveAssertNumberOfGSAsAdded(4, OPERATION_TIMEOUT);
        
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