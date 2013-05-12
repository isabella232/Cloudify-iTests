package org.cloudifysource.quality.iTests.test.esm.stateless.manual.memory;


import iTests.framework.utils.DeploymentUtils;
import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.*;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
/**
 *
 *  Setup:
 *
 *
 *  A: Shared stateless deployment, total 4500m, per container 1500m - expecting 3 containers on a single machine
 *  B: Dedicated stateless deployment, total 3000m, per container 3000m - expecting 1 container on 1 machine (different
 *  than A's machine)
 *
 *  After Scale out:
 *  A: 4500m --> 6000m
 *  total 6000m, per container 1500m - expecting 4 containers on 2 machines
 *
 *  B: 3000m --> 6000m
 *  total 6000m, per container 3000m - expecting 2 containers on 2 machines (different than A's machins)
 *
 *
 * @author dank
 */
public class MixedSharedDedicatedManualByonStatelessTwoDeploymentsScaleOutTest extends AbstractFromXenToByonGSMTest {

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

    private boolean discoveredMachineProvisioning = false;


    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
    public void mixedSharedDedicatedManualXenStatelessDeploymentTwoIndenpendentIsolationXenMachineProvisioningTest() throws Exception {
        repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
        mixedSharedDedicatedManualXenStatelessDeploymentTwoIndenpendentIsolationTest(
                getMachineProvisioningConfig());
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void mixedSharedDedicatedManualXenStatelessDeploymentTwoIndenpendentIsolationDiscoveredsMachineProvisioningTest() throws Exception {
        setupDiscovereMachineProvisioningEnvironment();
        repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
        mixedSharedDedicatedManualXenStatelessDeploymentTwoIndenpendentIsolationTest(
                getDiscoveredMachineProvisioningConfig());
    }

    public void mixedSharedDedicatedManualXenStatelessDeploymentTwoIndenpendentIsolationTest(
            ElasticMachineProvisioningConfig machineProvisioningConfig) throws Exception {

        File archive = DeploymentUtils.getArchive("simpleStatelessPu.jar");
        repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
        //container sizes:
        int sizeContainerA = 1500;
        int sizeContainerB = 3000;

        final ProcessingUnit puA = super.deploy(new ElasticStatelessProcessingUnitDeployment(archive)
                .name("A")
                .memoryCapacityPerContainer(sizeContainerA,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                        .memoryCapacity(sizeContainerA * 3,MemoryUnit.MEGABYTES)
                        .create())
                .sharedMachineProvisioning("AShare", machineProvisioningConfig)
        );

        final ProcessingUnit puB = super.deploy(new ElasticStatelessProcessingUnitDeployment(archive)
                .name("B")
                .memoryCapacityPerContainer(sizeContainerB,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                        .memoryCapacity(sizeContainerB,MemoryUnit.MEGABYTES)
                        .create())
                .dedicatedMachineProvisioning(machineProvisioningConfig)
        );

        assertTrue("Failed waiting for pu instances", puA.waitFor(3, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for pu instances", puB.waitFor(1, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));

        repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);

        repetitiveAssertNumberOfGSCsAdded(4, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);

        assertEquals("Expecting deployment on 1 machine", 1, getNumberOfMachinesWithPUInstance(puA));
        assertEquals("Expecting deployment on 1 machine", 1, getNumberOfMachinesWithPUInstance(puB));

        assertEquals("Number of instances", 3, puA.getInstances().length);
        assertEquals("Number of instances", 1, puB.getInstances().length);

        assertDeploymentOnDifferentMachines(puA, puB);

        if (isDiscoveredMachineProvisioning()) {
            startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
            startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        }

        puA.scale(new ManualCapacityScaleConfigurer().memoryCapacity(4*sizeContainerA, MemoryUnit.MEGABYTES).create());
        puB.scale(new ManualCapacityScaleConfigurer().memoryCapacity(2*sizeContainerB, MemoryUnit.MEGABYTES).create());

        assertTrue("Failed waiting for pu instances", puA.waitFor(4, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for pu instances", puB.waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        repetitiveAssertNumberOfGSAsAdded(4, OPERATION_TIMEOUT);

        assertEquals("Expecting deployment on 2 machines", 2, getNumberOfMachinesWithPUInstance(puA));
        assertEquals("Expecting deployment on 2 machines", 2, getNumberOfMachinesWithPUInstance(puB));

        assertDeploymentOnDifferentMachines(puA, puB);

        assertUndeployAndWait(puA);
        assertUndeployAndWait(puB);

    }

    private int getNumberOfMachinesWithPUInstance(ProcessingUnit pu) {
        Set<Machine> puMachines = new HashSet<Machine>();
        for (ProcessingUnitInstance instance : pu.getInstances()) {
            puMachines.add(instance.getMachine());
        }

        return puMachines.size();
    }

    private void assertDeploymentOnDifferentMachines(ProcessingUnit puA, ProcessingUnit puB) {

        Set<Machine> puAMachines = new HashSet<Machine>();
        for (ProcessingUnitInstance instance : puA.getInstances()) {
            puAMachines.add(instance.getMachine());
        }

        Set<Machine> puBMachines = new HashSet<Machine>();
        for (ProcessingUnitInstance instance : puB.getInstances()) {
            puBMachines.add(instance.getMachine());
        }

        boolean isolated = true;

        for (Machine machine : puAMachines) {
            if (puBMachines.contains(machine)) {
                isolated = false;
            }
        }

        for (Machine machine : puBMachines) {
            if (puAMachines.contains(machine)) {
                isolated = false;
            }
        }

        assertTrue("Expecting isolation between two PUs", isolated);
    }

    public void setupDiscovereMachineProvisioningEnvironment() throws Exception {
        discoveredMachineProvisioning = true;
        startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public boolean isDiscoveredMachineProvisioning() {
        return discoveredMachineProvisioning;
    }

}


