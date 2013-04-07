package org.cloudifysource.quality.iTests.test.esm.datagrid.manual.memory;


import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.*;

import java.util.concurrent.TimeUnit;

public class SharedManualByonDataGridThreeDeploymentsTest extends AbstractFromXenToByonGSMTest {

    @BeforeMethod
    public void beforeTest() {
        super.beforeTestInit();
    }

    @BeforeClass
    protected void bootstrap() throws Exception {
        super.bootstrapBeforeClass();
    }

    @AfterMethod(alwaysRun = true)
    public void afterTest() {
        discoveredMachineProvisioning = true;
        super.afterTest();
    }

    @AfterClass(alwaysRun = true)
    protected void teardownAfterClass() throws Exception {
        super.teardownAfterClass();
    }

    private static final String SHARING_ID = "sharedGroup";
    private boolean discoveredMachineProvisioning = false;

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2", enabled = false)
    public void manualXenDataGridDeploymentTwoIndenpendentXenMachineProvisioningTest() {
        repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
        manualXenDataGridDeploymentTwoIndenpendentTest(getMachineProvisioningConfig());
    }


    // TODO: Finish writing test.. Committed to serve as reference for GS-8810
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2", enabled = false)
    public void manualXenDataGridDeploymentTwoIndenpendentDiscoveredsMachineProvisioningTest() {
        setupDiscovereMachineProvisioningEnvironment();
        repetitiveAssertNumberOfGSAsAdded(3, OPERATION_TIMEOUT);
        manualXenDataGridDeploymentTwoIndenpendentTest(getDiscoveredMachineProvisioningConfig());
    }

    public void manualXenDataGridDeploymentTwoIndenpendentTest(ElasticMachineProvisioningConfig machineProvisioningConfig) {

        repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
        final ProcessingUnit pu = super.deploy(new ElasticSpaceDeployment("myGrid")
                .maxMemoryCapacity(2744, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(343,MemoryUnit.MEGABYTES)
                .highlyAvailable(false)
                .scale(new ManualCapacityScaleConfigurer()
                        .memoryCapacity(1372,MemoryUnit.MEGABYTES)
                        .create())
                .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );

        final ProcessingUnit pu2 = super.deploy(new ElasticSpaceDeployment("myGrid2")
                .maxMemoryCapacity(2744, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(343,MemoryUnit.MEGABYTES)
                .highlyAvailable(false)
                .scale(new ManualCapacityScaleConfigurer()
                        .memoryCapacity(1372,MemoryUnit.MEGABYTES)
                        .create())
                .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );

        final ProcessingUnit pu3 = super.deploy(new ElasticSpaceDeployment("myGrid3")
                .maxMemoryCapacity(2744, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(343,MemoryUnit.MEGABYTES)
                .highlyAvailable(false)
                .scale(new ManualCapacityScaleConfigurer()
                        .memoryCapacity(1372,MemoryUnit.MEGABYTES)
                        .create())
                .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );

        // wait and asserts
        if (isDiscoveredMachineProvisioning()) {
            startNewByonMachines(getElasticMachineProvisioningCloudifyAdapter(), 3, OPERATION_TIMEOUT*2 ,TimeUnit.MILLISECONDS);
        }

        ///

        final ProcessingUnit pu4 = super.deploy(new ElasticSpaceDeployment("myGrid4")
                .maxMemoryCapacity(2744, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(343,MemoryUnit.MEGABYTES)
                .highlyAvailable(false)
                .scale(new ManualCapacityScaleConfigurer()
                        .memoryCapacity(2058,MemoryUnit.MEGABYTES)
                        .create())
                .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );

        pu.scale(new ManualCapacityScaleConfigurer().memoryCapacity(2058, MemoryUnit.MEGABYTES).create());
        pu2.scale(new ManualCapacityScaleConfigurer().memoryCapacity(2058, MemoryUnit.MEGABYTES).create());
        pu3.scale(new ManualCapacityScaleConfigurer().memoryCapacity(2058, MemoryUnit.MEGABYTES).create());

        // maybe wait some
        pu2.waitFor(1);
        pu2.getInstances()[0].restartAndWait();


        // some tests


        pu.scale(new ManualCapacityScaleConfigurer().memoryCapacity(1372, MemoryUnit.MEGABYTES).create());
        pu2.scale(new ManualCapacityScaleConfigurer().memoryCapacity(1372, MemoryUnit.MEGABYTES).create());
        pu3.scale(new ManualCapacityScaleConfigurer().memoryCapacity(1372, MemoryUnit.MEGABYTES).create());


        // wait


        pu.scale(new ManualCapacityScaleConfigurer().memoryCapacity(2744, MemoryUnit.MEGABYTES).create());
        pu2.scale(new ManualCapacityScaleConfigurer().memoryCapacity(2744, MemoryUnit.MEGABYTES).create());
        pu3.scale(new ManualCapacityScaleConfigurer().memoryCapacity(2744, MemoryUnit.MEGABYTES).create());

        assertUndeployAndWait(pu);
        assertUndeployAndWait(pu2);
        assertUndeployAndWait(pu3);
        assertUndeployAndWait(pu4);
    }

    public void setupDiscovereMachineProvisioningEnvironment() {
        discoveredMachineProvisioning = true;
        startNewByonMachines(getElasticMachineProvisioningCloudifyAdapter(), 2, OPERATION_TIMEOUT*2, TimeUnit.MILLISECONDS);
    }

    public boolean isDiscoveredMachineProvisioning() {
        return discoveredMachineProvisioning;
    }

}


