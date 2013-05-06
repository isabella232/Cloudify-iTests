package org.cloudifysource.quality.iTests.test.esm.stateless.manual.memory;


import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import org.cloudifysource.quality.iTests.framework.utils.DeploymentUtils;
import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.*;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
/**
 * Tests deployment of four different spaces with different memory sizes. 
 *  *
 *  Each machine has approx 5GB of memory
 * Setup: (on two machines)
 * A: 1200m total, 600m per container
 * B: 1200m total, 600m per container
 * C: 2000m total, 1000m per container
 * D: 4400m total, 2200m per container
 *
 * Total: 8800 MB
 *
 * After scale up: (third machine)
 * A: m total
 * D: m total
 *
 *
 * @author boris (dan's test)
 */
public class SharedStatelessManualByonFourMixedDeploymentsTest extends AbstractFromXenToByonGSMTest {

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
    public void manualXenStatelessDeploymentFourMixedDeploymentsXenMachineProvisioningTest() throws Exception {
        repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
        manualXenStatelessDeploymentFourMixedDeploymentsTest(getMachineProvisioningConfig());
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void manualXenStatelessDeploymentFourMixedDeploymentsDiscoveredsMachineProvisioningTest() throws Exception {
        setupDiscovereMachineProvisioningEnvironment();
        repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
        manualXenStatelessDeploymentFourMixedDeploymentsTest(getDiscoveredMachineProvisioningConfig());
    }

    public void manualXenStatelessDeploymentFourMixedDeploymentsTest(ElasticMachineProvisioningConfig machineProvisioningConfig) throws Exception {

        // make sure we have a clean environment
        repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
        File archive = DeploymentUtils.getArchive("simpleStatelessPu.jar");

        // deploy 4 stateless PUs according to setup described above
        int sizeContainerA = 600;
        int sizeContainerB =600;
        int sizeContainerC =1000;
        int sizeContainerD =2200;
        final ProcessingUnit puA = super.deploy(new ElasticStatelessProcessingUnitDeployment(archive)
                .name("A")
                .memoryCapacityPerContainer(sizeContainerA ,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                        .memoryCapacity(sizeContainerA*2,MemoryUnit.MEGABYTES)
                        .create())
                .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );
        final ProcessingUnit puB = super.deploy(new ElasticStatelessProcessingUnitDeployment(archive)
                .name("B")
                .memoryCapacityPerContainer(sizeContainerB ,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                        .memoryCapacity(sizeContainerB*2,MemoryUnit.MEGABYTES)
                        .create())
                .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );
        final ProcessingUnit puC = super.deploy(new ElasticStatelessProcessingUnitDeployment(archive)
                .name("C")
                .memoryCapacityPerContainer(sizeContainerC ,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                        .memoryCapacity(sizeContainerC*2,MemoryUnit.MEGABYTES)
                        .create())
                .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );
        final ProcessingUnit puD = super.deploy(new ElasticStatelessProcessingUnitDeployment(archive)
                .name("D")
                .memoryCapacityPerContainer(sizeContainerD ,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                        .memoryCapacity(sizeContainerD*2,MemoryUnit.MEGABYTES)
                        .create())
                .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );

        assertTrue("Failed waiting for pu instances", puA.waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for pu instances", puB.waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for pu instances", puC.waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for pu instances", puD.waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));

        repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsAdded(8, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);

        Set<Machine> machines = new HashSet<Machine>();

        // prepare machines set 
        machines.add(puA.getInstances()[0].getMachine());
        machines.add(puA.getInstances()[1].getMachine());
        machines.add(puB.getInstances()[0].getMachine());
        machines.add(puB.getInstances()[1].getMachine());
        machines.add(puC.getInstances()[0].getMachine());
        machines.add(puC.getInstances()[1].getMachine());
        machines.add(puD.getInstances()[0].getMachine());
        machines.add(puD.getInstances()[1].getMachine());

        //it doesn't split into 2 machines
        assertEquals("PUs should be shared on two machines", 2, machines.size());

        //scale up 'A' and 'D'

        if (isDiscoveredMachineProvisioning()) {
            startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        }


        // TODO: GS-8808 - verify with Itai.
        // If we request to scale A before we request to scale D, we might run into a situation that the rebalancing of A,
        // would cause all instances of A to occupy  a full machine. Thus leaving us with no more room for scaling D without requesting a new machine.


        //add 1 more container for D
        puD.scale(new ManualCapacityScaleConfigurer().memoryCapacity( 3 * sizeContainerD, MemoryUnit.MEGABYTES).create());

        repetitiveAssertNumberOfGSCsAdded(9, OPERATION_TIMEOUT);

        //add 4 more containers for A
        puA.scale(new ManualCapacityScaleConfigurer().memoryCapacity(6 * sizeContainerA, MemoryUnit.MEGABYTES).create());

        //total containers 8+4+1=13
        repetitiveAssertNumberOfGSCsAdded(13, OPERATION_TIMEOUT);

        assertTrue("Failed waiting for pu instances", puA.waitFor(6, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for pu instances", puB.waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for pu instances", puC.waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for pu instances", puD.waitFor(3, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));

        repetitiveAssertNumberOfGSAsAdded(3, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);

        //make sure only A and D grew 
        repetativeAssertPlannedNumberOfInstances(6, puA);
        repetativeAssertPlannedNumberOfInstances(3, puD);

        // this part fails - D isn't on a new Machine
        asserInstanceOnNewMachine(puA, machines);
        asserInstanceOnNewMachine(puD, machines);

        assertUndeployAndWait(puA);
        assertUndeployAndWait(puB);
        assertUndeployAndWait(puC);
        assertUndeployAndWait(puD);

    }

    private void repetativeAssertPlannedNumberOfInstances(final int expected, final ProcessingUnit pu) {

        super.repetitiveAssertTrue("Expected totalNumberOfInstances for pu " + pu + " to be " + expected,
                new RepetitiveConditionProvider() {

                    @Override
                    public boolean getCondition() {
                        return pu.getTotalNumberOfInstances() == expected;
                    }
                },
                OPERATION_TIMEOUT);
    }

    public void asserInstanceOnNewMachine(ProcessingUnit pu,  Set<Machine> previousMachines) {
        Set<Machine> machines = new HashSet<Machine>();
        for (ProcessingUnitInstance instance : pu.getInstances()) {
            machines.add(instance.getMachine());
        }

        machines.removeAll(previousMachines);
        assertEquals("Expecting an instance on the new machine", 1, machines.size());
    }

    public void setupDiscovereMachineProvisioningEnvironment() throws Exception {
        discoveredMachineProvisioning = true;
        startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public boolean isDiscoveredMachineProvisioning() {
        return discoveredMachineProvisioning;
    }

    @Override
    protected DiscoveredMachineProvisioningConfig getDiscoveredMachineProvisioningConfig() {
        DiscoveredMachineProvisioningConfig config = new DiscoveredMachineProvisioningConfig();
        config.setReservedMemoryCapacityPerMachineInMB(128);
        config.setDedicatedManagementMachines(false);
        return config;
    }

}


