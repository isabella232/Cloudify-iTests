package org.cloudifysource.quality.iTests.test.esm.datagrid.eager;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.cloudifysource.quality.iTests.framework.utils.GsmTestUtils;

public class DedicatedEagerDataGridScaleInByonTest extends AbstractFromXenToByonGSMTest {

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

    // this test was invoked 3 times before, removed it for tests
    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1", invocationCount = 3)
    public void doTest() throws Exception {

        GridServiceAgent[] agents = startNewByonMachines(getElasticMachineProvisioningCloudifyAdapter(), 3, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        //gsa1 is the management machine
        //gsa2 = agents[0];
        GridServiceAgent gsa3 = agents[1];
        GridServiceAgent gsa4 = agents[2];

        final int containerCapacityInMB = 250;
        int numberOfContainers = 4;
        ProcessingUnit pu = super.deploy(
                new ElasticSpaceDeployment("eagerspace")
                        .maxMemoryCapacity(numberOfContainers*containerCapacityInMB, MemoryUnit.MEGABYTES)
                        .memoryCapacityPerContainer(containerCapacityInMB, MemoryUnit.MEGABYTES)
                        .dedicatedMachineProvisioning(
                                new DiscoveredMachineProvisioningConfigurer()
                                        .reservedMemoryCapacityPerMachine(128, MemoryUnit.MEGABYTES)
                                        .create())
                        .scale(new EagerScaleConfig())
        );

        GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, 4, OPERATION_TIMEOUT);

        int numberOfObjects = 1000;
        GsmTestUtils.writeData(pu, numberOfObjects);

        stopByonMachine(getElasticMachineProvisioningCloudifyAdapter(), gsa3, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, 3, OPERATION_TIMEOUT);

        GsmTestUtils.writeData(pu, numberOfObjects, 2*numberOfObjects);

        stopByonMachine(getElasticMachineProvisioningCloudifyAdapter(), gsa4, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, 2, OPERATION_TIMEOUT);

        GsmTestUtils.writeData(pu, numberOfObjects, 3*numberOfObjects);

        assertUndeployAndWait(pu);
    }
}
