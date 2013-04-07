package org.cloudifysource.quality.iTests.test.esm.datagrid.manual.memory;


import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.admin.space.Space;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
/**
 *  Setup:
 *  myGrid: 512m total, 256m per container 
 *  myGrid2: 512m total, 256m per container
 *
 *  Test: killing 1 machine (the one without a GSM on it),
 *  check initial state was restored and that only 1 new machine is up
 *
 *  There are four versions: one with DiscoveredMachineProvisioningConfig and the other with
 *  XenServerMachineProvisioningConfig those two are mixed with one with hard machine shutdown and another with
 *  soft machine shutdown 
 *
 * @author dank
 */
public class SharedManualByonDataGridTwoDeploymentsFailoverTest extends AbstractFromXenToByonGSMTest {

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
        discoveredMachineProvisioning = false;
        super.afterTest();
    }

    @AfterClass(alwaysRun = true)
    protected void teardownAfterClass() throws Exception {
        super.teardownAfterClass();
    }

    private static final String SHARING_ID = "sharedGroup";
    private boolean discoveredMachineProvisioning = false;

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
    public void manualXenDataGridDeploymentHardShutdownFailoverXenMachineProvisioningTest() throws Exception {
        manualXenDataGridDeploymentTwoIndenpendentDeploymentsFailoverTest(true, getMachineProvisioningConfig());
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
    public void manualXenDataGridDeploymentCleanShutdownFailoverXenMachineProvisioningTest() throws Exception {
        manualXenDataGridDeploymentTwoIndenpendentDeploymentsFailoverTest(false, getMachineProvisioningConfig());
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void manualXenDataGridDeploymentCleanShutdownFailoverDiscoveredMachineProvisioningTest()throws Exception {
        setupDiscovereMachineProvisioningEnvironment();
        repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
        manualXenDataGridDeploymentTwoIndenpendentDeploymentsFailoverTest(false, getDiscoveredMachineProvisioningConfig());
    }

    /**
     * Hard shutdown isn't supported in byon , need to check if there is a possible way to simulate this test.
     * @throws Exception
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
    public void manualXenDataGridDeploymentHardShutdownFailoverDiscoveredMachineProvisioningTest()throws Exception {
        setupDiscovereMachineProvisioningEnvironment();
        repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
        manualXenDataGridDeploymentTwoIndenpendentDeploymentsFailoverTest(true, getDiscoveredMachineProvisioningConfig());
    }

    private void manualXenDataGridDeploymentTwoIndenpendentDeploymentsFailoverTest(boolean hardShutdown, ElasticMachineProvisioningConfig machineProvisioningConfig) throws Exception {

        repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
        final ProcessingUnit pu = super.deploy(new ElasticSpaceDeployment("myGrid")
                .maxMemoryCapacity(512, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                        .memoryCapacity(512,MemoryUnit.MEGABYTES)
                        .create())
                .sharedMachineProvisioning(SHARING_ID,machineProvisioningConfig)
        );

        final ProcessingUnit pu2 = super.deploy(new ElasticSpaceDeployment("myGrid2")
                .maxMemoryCapacity(512, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                        .memoryCapacity(512,MemoryUnit.MEGABYTES)
                        .create())
                .sharedMachineProvisioning(SHARING_ID,machineProvisioningConfig)
        );

        Space space = pu.waitForSpace(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        Space space2 = pu2.waitForSpace(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);

        assertNotNull("Failed getting space instance", space);
        assertNotNull("Failed getting space2 instance", space2);

        assertTrue("Failed waiting for space instances",
                space.waitFor(space.getTotalNumberOfInstances(),
                        OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for space2 instances",
                space2.waitFor(space2.getTotalNumberOfInstances(),
                        OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));

        repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);

        repetitiveAssertNumberOfGSCsAdded(4, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);


        assertTrue("Primary/Backup should be deployed on different machines",
                !space.getInstances()[0].getMachine().equals(space.getInstances()[1].getMachine()));

        assertTrue("Primary/Backup should be deployed on different machines",
                !space2.getInstances()[0].getMachine().equals(space2.getInstances()[1].getMachine()));

        assertTrue("Primary/Backup should be deployed on different containers",
                !space.getInstances()[0].getVirtualMachine().getGridServiceContainer().equals(
                        space.getInstances()[1].getVirtualMachine().getGridServiceContainer()));

        assertTrue("Primary/Backup should be deployed on different containers",
                !space2.getInstances()[0].getVirtualMachine().getGridServiceContainer().equals(
                        space2.getInstances()[1].getVirtualMachine().getGridServiceContainer()));

        Set<Machine> machines = new HashSet<Machine>();

        machines.add(space.getInstances()[0].getMachine());
        machines.add(space.getInstances()[1].getMachine());
        machines.add(space2.getInstances()[0].getMachine());
        machines.add(space2.getInstances()[1].getMachine());

        assertEquals("PUs should be shared on two machines", 2, machines.size());

        /* kill 1 machine (the one without the gsm) */
        Machine gsmMachine = admin.getGridServiceManagers().waitForAtLeastOne().getMachine();

        Machine machine = pu.getInstances()[0].getMachine().equals(gsmMachine) ?
                pu.getInstances()[1].getMachine() : pu.getInstances()[0].getMachine();
        
        if (hardShutdown) {
            //hardShutdownMachine(machine, getMachineProvisioningConfig(), OPERATION_TIMEOUT);
            AssertFail("Hard shutdown isn't supported in Byon");
        } else {
            stopByonMachine(getElasticMachineProvisioningCloudifyAdapter(),machine.getGridServiceAgent(),OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
        }

        if (isDiscoveredMachineProvisioning()) {
            startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        }
        repetitiveAssertNumberOfGSCsAdded(6, OPERATION_TIMEOUT);
        
        /* assert proper failover */
        assertTrue("Failed waiting for space instances",
                space.waitFor(space.getTotalNumberOfInstances(),
                        OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for space2 instances",
                space2.waitFor(space2.getTotalNumberOfInstances(),
                        OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));

        repetitiveAssertNumberOfGSAsAdded(3, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(2, OPERATION_TIMEOUT);

        assertTrue("Primary/Backup should be deployed on different machines",
                !space.getInstances()[0].getMachine().equals(space.getInstances()[1].getMachine()));

        assertTrue("Primary/Backup should be deployed on different machines",
                !space2.getInstances()[0].getMachine().equals(space2.getInstances()[1].getMachine()));

        assertTrue("Primary/Backup should be deployed on different containers",
                !space.getInstances()[0].getVirtualMachine().getGridServiceContainer().equals(
                        space.getInstances()[1].getVirtualMachine().getGridServiceContainer()));

        assertTrue("Primary/Backup should be deployed on different containers",
                !space2.getInstances()[0].getVirtualMachine().getGridServiceContainer().equals(
                        space2.getInstances()[1].getVirtualMachine().getGridServiceContainer()));


        machines = new HashSet<Machine>();

        machines.add(space.getInstances()[0].getMachine());
        machines.add(space.getInstances()[1].getMachine());
        machines.add(space2.getInstances()[0].getMachine());
        machines.add(space2.getInstances()[1].getMachine());

        assertEquals("PUs should be shared on two machines", 2, machines.size());

        assertUndeployAndWait(pu);
    }


    public void setupDiscovereMachineProvisioningEnvironment() throws Exception {
        discoveredMachineProvisioning = true;
        startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public boolean isDiscoveredMachineProvisioning() {
        return discoveredMachineProvisioning;
    }

}


