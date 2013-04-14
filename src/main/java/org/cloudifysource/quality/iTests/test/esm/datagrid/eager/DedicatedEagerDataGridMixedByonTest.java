package org.cloudifysource.quality.iTests.test.esm.datagrid.eager;

import org.cloudifysource.quality.iTests.framework.utils.GsmTestUtils;
import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.*;
import org.openspaces.admin.gsc.GridServiceContainer;
import java.util.concurrent.TimeUnit;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;

public class DedicatedEagerDataGridMixedByonTest extends AbstractFromXenToByonGSMTest {

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

    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1", invocationCount = 3)
    public void doTest() throws Exception {

        GridServiceAgent[] agents = startNewByonMachines(getElasticMachineProvisioningCloudifyAdapter(),2,OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
        //gsa1 = management machine
        //gsa2 = agents[0];
        GridServiceAgent gsa3 = agents[1];

        final int containerCapacityInMB = 250;
        final int numberOfContainers = 6;
        ProcessingUnit pu = super.deploy(
                new ElasticSpaceDeployment("elasticspace")
                        .maxMemoryCapacity(numberOfContainers*containerCapacityInMB, MemoryUnit.MEGABYTES)
                        .memoryCapacityPerContainer(containerCapacityInMB, MemoryUnit.MEGABYTES)
                        .dedicatedMachineProvisioning(
                                new DiscoveredMachineProvisioningConfigurer()
                                        .reservedMemoryCapacityPerMachine(128, MemoryUnit.MEGABYTES)
                                        .create())
                        .scale(new EagerScaleConfig())
        );

        int numberOfMachines = 3;
        GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, numberOfMachines, OPERATION_TIMEOUT);

        final int numberOfObjects = 1000;
        GsmTestUtils.writeData(pu, numberOfObjects, numberOfObjects);


        GridServiceContainer[] containers = admin.getGridServiceContainers().getContainers();
        GsmTestUtils.killContainer(containers[0]);
        GsmTestUtils.writeData(pu, numberOfObjects, 2*numberOfObjects);
        GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, numberOfMachines, OPERATION_TIMEOUT);

        GridServiceAgent gsa4 = startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(),OPERATION_TIMEOUT, TimeUnit.MILLISECONDS); //5th machine
        numberOfMachines++;

        // wait for new container to start, otherwise next wait for scale may succeed before containers started moving
        gsa4.getMachine().getGridServiceContainers().waitFor(1);
        GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, numberOfMachines, OPERATION_TIMEOUT);

        // gsa3 was chosen since it does not hold any management process.
        stopByonMachine(getElasticMachineProvisioningCloudifyAdapter(),gsa3, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        numberOfMachines--;
        GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, numberOfMachines, OPERATION_TIMEOUT);

        GsmTestUtils.writeData(pu, numberOfObjects, 3*numberOfObjects);

        assertUndeployAndWait(pu);
    }
}