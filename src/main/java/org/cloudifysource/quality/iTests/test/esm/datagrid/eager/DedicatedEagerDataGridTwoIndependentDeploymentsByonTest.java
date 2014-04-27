package org.cloudifysource.quality.iTests.test.esm.datagrid.eager;

import iTests.framework.utils.GsmTestUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.cloudifysource.esc.driver.provisioning.byon.ByonProvisioningDriver;
import org.cloudifysource.quality.iTests.test.cli.cloudify.util.CloudTestUtils;
import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DedicatedEagerDataGridTwoIndependentDeploymentsByonTest extends AbstractFromXenToByonGSMTest {

    @BeforeMethod
    public void beforeTest() {
        super.beforeTestInit();
    }

    @BeforeClass
    protected void bootstrap() throws Exception {
        super.bootstrap();
    }

    @AfterMethod
    public void afterTest() {
        super.afterTest();
    }

    @AfterClass(alwaysRun = true)
    protected void teardownAfterClass() throws Exception {
        super.teardownAfterClass();
    }
    private static final String CLOUD_PREFIX = "__cloud.zone.";
    private static final String ZONE1 = "zone1";
    private static final String ZONE2 = "zone2";

    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1")
    public void testElasticSpaceDeployment() throws Exception {

        String[] zones = {CLOUD_PREFIX+ZONE1,CLOUD_PREFIX+ZONE1,CLOUD_PREFIX+ZONE2,CLOUD_PREFIX+ZONE2};
        GridServiceAgent[] agents = startNewByonMachinesWithZones(getElasticMachineProvisioningCloudifyAdapter(), 4, zones, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);

        final int containerCapacityInMB = 250;
        ProcessingUnit pu1 = super.deploy(
                new ElasticSpaceDeployment("eagerspace1")
                        .maxMemoryCapacity(2*containerCapacityInMB, MemoryUnit.MEGABYTES)
                        .memoryCapacityPerContainer(containerCapacityInMB, MemoryUnit.MEGABYTES)
                        .dedicatedMachineProvisioning(
                                new DiscoveredMachineProvisioningConfigurer()
                                        .addGridServiceAgentZone(CLOUD_PREFIX+ZONE1)
                                        .dedicatedManagementMachines()
                                        .create())

                        .scale(new EagerScaleConfigurer()
                                .create())
        );

        GsmTestUtils.waitForScaleToComplete(pu1, 2, 2, OPERATION_TIMEOUT);

        assertNumberOfContainersOnMachine(agents[0],1);
        assertNumberOfContainersOnMachine(agents[1],1);
        assertNumberOfContainersOnMachine(agents[2],0);
        assertNumberOfContainersOnMachine(agents[3],0);
        Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(agents[0], pu1));
        Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(agents[1], pu1));
        Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(agents[2], pu1));
        Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(agents[3], pu1));


        ProcessingUnit pu2 = super.deploy(
                new ElasticSpaceDeployment("eagerspace2")
                        .maxMemoryCapacity(2*containerCapacityInMB, MemoryUnit.MEGABYTES)
                        .memoryCapacityPerContainer(containerCapacityInMB, MemoryUnit.MEGABYTES)
                        .dedicatedMachineProvisioning(
                                new DiscoveredMachineProvisioningConfigurer()
                                        .dedicatedManagementMachines()
                                        .addGridServiceAgentZone(CLOUD_PREFIX+ZONE2)
                                        .create())
                        .scale(new EagerScaleConfig())
        );

        GsmTestUtils.waitForScaleToComplete(pu2, 2, 2, OPERATION_TIMEOUT);

        assertNumberOfContainersOnMachine(agents[0],1);
        assertNumberOfContainersOnMachine(agents[1],1);
        assertNumberOfContainersOnMachine(agents[2],1);
        assertNumberOfContainersOnMachine(agents[3],1);
        Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(agents[0], pu1));
        Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(agents[1], pu1));
        Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(agents[2], pu1));
        Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(agents[3], pu1));
        Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(agents[0], pu2));
        Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(agents[1], pu2));
        Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(agents[2], pu2));
        Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(agents[3], pu2));

        assertUndeployAndWait(pu1);
        assertUndeployAndWait(pu2);
    }

    private void assertNumberOfContainersOnMachine(GridServiceAgent gsa, int expected) {
        int actual = countGridServiceContainersOnMachine(gsa);
        String message =
                "Wrong number of containers on machine " + gsa.getMachine().getHostAddress() + " " +
                        "expected "+ expected + " "+
                        "actual " + actual;
        Assert.assertEquals(message, expected, actual);
    }

    int countProcessingUnitInstancesOnMachine(GridServiceAgent agent, ProcessingUnit pu) {
        return agent.getMachine().getProcessingUnitInstances(pu.getName()).length;
    }

    int countGridServiceContainersOnMachine(GridServiceAgent agent) {
        return agent.getMachine().getGridServiceContainers().getSize();
    }

    @Override
    public void beforeBootstrap() throws IOException {
    	CloudTestUtils.replaceCloudDriverImplementation(
    			getService(),
    			ByonProvisioningDriver.class.getName(), //old class
    			"org.cloudifysource.quality.iTests.BasicLocationAwareByonProvisioningDriver", //new class
    			"location-aware-provisioning-byon", "2.3-SNAPSHOT"); //jar
    }

    public String toClassName(String className) {
        return "className \""+className+"\"";
    }
}


