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
import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import org.cloudifysource.quality.iTests.framework.utils.GsmTestUtils;
import iTests.framework.utils.LogUtils;

public class DedicatedEagerDataGridScaleOutByonTest extends AbstractFromXenToByonGSMTest {

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

    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1" , enabled = true)
    public void testElasticDataGridGracefulScaleOut() throws Exception {

        startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);

        final int containerCapacityInMB = 256;
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

        GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, 2, OPERATION_TIMEOUT);
        waitUntilAtLeastOneContainerPerMachine();

        int numberOfObjects = 1000;
        GsmTestUtils.writeData(pu, numberOfObjects);

        // start first VM and wait
        startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        repetitiveAssertNumberOfGSCsRemoved(1, OPERATION_TIMEOUT);
        GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, 3, OPERATION_TIMEOUT);
        waitUntilAtLeastOneContainerPerMachine();
        GsmTestUtils.writeData(pu, numberOfObjects, numberOfObjects*2);

        // start second VM
        startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        repetitiveAssertNumberOfGSCsRemoved(2, OPERATION_TIMEOUT);
        GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, 4, OPERATION_TIMEOUT);

        waitUntilAtLeastOneContainerPerMachine();
        GsmTestUtils.writeData(pu, numberOfObjects, numberOfObjects*3);
        assertUndeployAndWait(pu);
    }

    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1", enabled = true )
    public void testElasticDataGridFastScaleOut() throws Exception {

        startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);

        final int containerCapacityInMB = 256;
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

        GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, 2, OPERATION_TIMEOUT);
        waitUntilAtLeastOneContainerPerMachine();

        int numberOfObjects = 1000;
        GsmTestUtils.writeData(pu, numberOfObjects);

        //start 2 machines concurrently

        startNewByonMachines(getElasticMachineProvisioningCloudifyAdapter(), 2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);

        GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, 4, OPERATION_TIMEOUT);
        waitUntilAtLeastOneContainerPerMachine();
        assertEquals(numberOfObjects,GsmTestUtils.countData(pu));
        assertUndeployAndWait(pu);
    }

    private void waitUntilAtLeastOneContainerPerMachine() {

        final RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
            String lastMessage = "";
            public boolean getCondition() {

                boolean oneContainerPerMachine = true;
                String message = "";

                for (GridServiceAgent gsa : admin.getGridServiceAgents()) {
                    int containersOnMachine = gsa.getMachine().getGridServiceContainers().getSize();
                    if(containersOnMachine==0) {
                        message = message + " " + gsa.getMachine().getHostName();
                        oneContainerPerMachine = false;
                    }
                }
                if (!oneContainerPerMachine) {
                    message = "Waiting until the following machines have at least one container: " + message;

                    if (!lastMessage.equals(message)) {
                        LogUtils.log(message);
                        lastMessage = message;
                    }
                }
                return oneContainerPerMachine;

            }
        };

        AssertUtils.repetitiveAssertTrue("Waiting for container deployment to complete",
                condition, OPERATION_TIMEOUT);
    }


}
