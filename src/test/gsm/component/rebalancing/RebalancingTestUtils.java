package test.gsm.component.rebalancing;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.events.GridServiceContainerRemovedEventListener;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.internal.pu.elastic.ProcessingUnitSchemaConfig;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.grid.gsm.capacity.ClusterCapacityRequirements;
import org.openspaces.grid.gsm.capacity.MachineCapacityRequirements;
import org.openspaces.grid.gsm.rebalancing.FutureStatefulProcessingUnitInstance;
import org.openspaces.grid.gsm.rebalancing.RebalancingSlaEnforcement;
import org.openspaces.grid.gsm.rebalancing.RebalancingSlaEnforcementEndpoint;
import org.openspaces.grid.gsm.rebalancing.RebalancingSlaPolicy;
import org.openspaces.grid.gsm.rebalancing.RebalancingUtils;
import org.testng.Assert;

import test.data.Person;
import test.gsm.GsmTestUtils;
import test.gsm.component.SlaEnforcementTestUtils;
import test.utils.AdminUtils;
import test.utils.DeploymentUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;

public class RebalancingTestUtils {

    public static ProcessingUnit doNothingTest(RebalancingSlaEnforcement rebalancing,
            GridServiceAgent gsa, GridServiceManager gsm, String zone, 
            int numberOfObjects) throws InterruptedException {
        Admin admin = gsa.getAdmin();
        Machine[] machines = new Machine[] { gsa.getMachine()};
        //deploy pu partitioned(2,1) on 4 GSCs
        AdminUtils.loadGSCs(gsa, 4, zone);
        admin.getGridServiceContainers().waitFor(4);
        ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnSingleMachine(gsm, zone, 2,1);
        RebalancingTestUtils.insertPersonObjectsIntoSpace(pu, numberOfObjects);
        RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu);    
        RebalancingTestUtils.assertBalancedDeployment(pu, machines);
        GridServiceContainer[] containers = admin.getGridServiceContainers().getContainers();
        Assert.assertEquals(containers.length, 4);
        
        return pu;
    }
    
    public static void killGscAndWait(final GridServiceContainer removedGSC)
            throws InterruptedException {
        
        Admin admin = removedGSC.getAdmin();
        
        final CountDownLatch gscRemovedLatch = new CountDownLatch(1);
        GridServiceContainerRemovedEventListener gscRemovedEventListener = new GridServiceContainerRemovedEventListener() {
            public void gridServiceContainerRemoved(
                    GridServiceContainer gridServiceContainer) {
                if (gridServiceContainer.equals(removedGSC)) {
                    gscRemovedLatch.countDown();
                }
            }
        };

        try {
            admin.getGridServiceContainers().getGridServiceContainerRemoved()
                    .add(gscRemovedEventListener);
            removedGSC.kill();
            gscRemovedLatch.await();
        } finally {
            admin.getGridServiceContainers().getGridServiceContainerRemoved()
                    .remove(gscRemovedEventListener);
        }
    }

    public static ProcessingUnit deployProcessingUnitOnTwoMachines(GridServiceManager gsm, String zone, int numberOfPartitions,
            int numberOfBackupsPerPartition) {
        return deployProcessingUnit(gsm, zone, numberOfPartitions, numberOfBackupsPerPartition, 2);
    }

    public static ProcessingUnit deployProcessingUnitOnSingleMachine(GridServiceManager gsm, String zone, int numberOfPartitions,
            int numberOfBackupsPerPartition) {
        return deployProcessingUnit(gsm, zone, numberOfPartitions, numberOfBackupsPerPartition, 1);
    }

    public static ProcessingUnit deployProcessingUnit(GridServiceManager gsm, String zone, int numberOfPartitions,
            int numberOfBackupsPerPartition, int numberOfMachines) {

        Admin admin = gsm.getAdmin();
        
        ProcessingUnit pu = gsm.deploy(new SpaceDeployment("myspace").partitioned(
                numberOfPartitions, numberOfBackupsPerPartition).addZone(zone)
                .maxInstancesPerVM(1).maxInstancesPerMachine(
                        numberOfMachines == 1 ? 0 : 1));

        admin.getProcessingUnits().getProcessingUnit("myspace").waitFor(
                numberOfPartitions * (1 + numberOfBackupsPerPartition));
        
        return pu;

    }

    public static void relocatePU(ProcessingUnit pu, int instanceId,
            SpaceMode spaceMode, GridServiceContainer target)
            throws InterruptedException {

        while (!RebalancingUtils.isProcessingUnitIntact(pu)) {
            Thread.sleep(1000);
        }

        ProcessingUnitInstance backupInstance = findProcessingUnitInstanceByInstanceIdAndByMode(
                pu, instanceId, SpaceMode.BACKUP);

        ProcessingUnitInstance primaryInstance = findProcessingUnitInstanceByInstanceIdAndByMode(
                pu, instanceId, SpaceMode.PRIMARY);

        if (!backupInstance.getGridServiceContainer().equals(target)
                && spaceMode == SpaceMode.BACKUP) {
            if (primaryInstance.getGridServiceContainer().equals(target)) {
                primaryInstance.restartAndWait();
            } else if (pu.getMaxInstancesPerMachine() == 1
                    && primaryInstance.getGridServiceContainer().getMachine()
                            .equals(target.getMachine())) {
                primaryInstance.relocateAndWait(target);
            } else {
                backupInstance.relocateAndWait(target);
            }
        } else if (!primaryInstance.getGridServiceContainer().equals(target)
                && spaceMode == SpaceMode.PRIMARY) {
            if (backupInstance.getGridServiceContainer().equals(target)) {
                primaryInstance.restartAndWait();
            } else if (pu.getMaxInstancesPerMachine() == 1
                    && backupInstance.getGridServiceContainer().getMachine()
                            .equals(target.getMachine())) {
                backupInstance.relocateAndWait(target);
                primaryInstance.restartAndWait();
            } else {
                primaryInstance.relocateAndWait(target);
                backupInstance.restartAndWait();
            }
        }

        while (!RebalancingUtils.isProcessingUnitIntact(pu)) {
            Thread.sleep(1000);
        }
    }

    public static ProcessingUnitInstance findProcessingUnitInstanceByInstanceIdAndByMode(
            ProcessingUnit pu, int instanceId, SpaceMode spaceMode) {

        Admin admin = pu.getAdmin();
        
        for (final GridServiceContainer container : admin
                .getGridServiceContainers()) {

            ProcessingUnitInstance puInstance = findProcessingUnitInstanceByInstanceIdAndByContainer(
                    pu, instanceId, container);
            if (puInstance != null && puInstance.getSpaceInstance() != null
                    && puInstance.getSpaceInstance().getMode() == spaceMode) {
                return puInstance;
            }
        }

        return null;
    }

    public static ProcessingUnitInstance findProcessingUnitInstanceByInstanceIdAndByContainer(
            ProcessingUnit pu, int instanceId,
            final GridServiceContainer container) {
        for (final ProcessingUnitInstance puInstance : container
                .getProcessingUnitInstances(pu.getName())) {
            if (puInstance.getInstanceId() == instanceId) {
                return puInstance;
            }
        }
        return null;
    }

    public static void assertBalancedDeployment(ProcessingUnit pu,
            GridServiceContainer[] gridServiceContainers, Machine[] machines) {
        assertTrue(RebalancingUtils.isProcessingUnitIntact(pu,
                gridServiceContainers));
        assertTrue(RebalancingUtils.isEvenlyDistributedAcrossContainers(pu,
                gridServiceContainers));
        assertTrue(GsmTestUtils.isEvenlyDistributedAcrossMachines(pu,machines));
    }

    public static void assertBalancedDeployment(ProcessingUnit pu,
            Machine[] machines) {
        Set<GridServiceContainer> containers = new HashSet<GridServiceContainer>();
        for (Machine machine : machines) {
            for (GridServiceContainer container : machine
                    .getGridServiceContainers()) {
                containers.add(container);
            }
        }

        assertBalancedDeployment(pu, containers
                .toArray(new GridServiceContainer[containers.size()]), machines);
    }

    public static void enforceSlaAndWait(RebalancingSlaEnforcement rebalancing, ProcessingUnit pu,
            GridServiceContainer[] containers, ProcessingUnitSchemaConfig schema) throws InterruptedException {
        RebalancingSlaEnforcementEndpoint endpoint = rebalancing
                .createEndpoint(pu);
        try {
            RebalancingSlaPolicy sla = new RebalancingSlaPolicy();
            sla.setContainers(containers);
            sla.setSchemaConfig(schema);
            sla.setMaximumNumberOfConcurrentRelocationsPerMachine(1000);
            ClusterCapacityRequirements allocatedCapacity = new ClusterCapacityRequirements();
            for (GridServiceContainer container : containers) {
	            String agentUid = container.getGridServiceAgent().getUid();
				if (!allocatedCapacity.getAgentUids().contains(agentUid)) {
					GridServiceAgent agent = container.getGridServiceAgent();
					allocatedCapacity = allocatedCapacity.add( 
				            agentUid, 
		                    new MachineCapacityRequirements(agent.getMachine()));
	            }
            }
            sla.setAllocatedCapacity(allocatedCapacity);
            SlaEnforcementTestUtils.enforceSlaAndWait(pu.getAdmin(), endpoint, sla);
        } finally {
            rebalancing.destroyEndpoint(pu);
        }
    }

    /**
     * Uses all available GSCs for created sla
     */
    public static void enforceSlaAndWait(RebalancingSlaEnforcement rebalancing, ProcessingUnit pu)
            throws InterruptedException {
        enforceSlaAndWait(rebalancing, pu, pu.getAdmin().getGridServiceContainers().getContainers());
    }

    public static void enforceSlaAndWait(RebalancingSlaEnforcement rebalancing, ProcessingUnit pu, GridServiceContainer[] containers)
        throws InterruptedException {
        ProcessingUnitSchemaConfig schema = new ProcessingUnitSchemaConfig(new HashMap<String,String>());
        schema.setPartitionedSync2BackupSchema();
        enforceSlaAndWait(rebalancing, pu, containers, schema);
    }
    

    public static void enforceStatelessSlaAndWait(
            RebalancingSlaEnforcement rebalancing, ProcessingUnit pu,
            GridServiceContainer[] containers) throws InterruptedException {
    
        ProcessingUnitSchemaConfig schema = new ProcessingUnitSchemaConfig(new HashMap<String,String>());
        schema.setDefaultSchema();
        enforceSlaAndWait(rebalancing, pu, containers, schema);
    }
    
    /**
     * Puts 4 Person object into the pu embedded space and asserts they are
     * there
     */
    public static void insertPersonObjectsIntoSpace(ProcessingUnit pu, int numberOfObjects) {
        Space space = pu.waitForSpace();
        GigaSpace gigaSpace = space.getGigaSpace();
        Person[] persons = new Person[numberOfObjects];
        for (int i = 0; i < numberOfObjects; i++) {
            persons[i] = new Person(new Long(i));
        }
        gigaSpace.writeMultiple(persons);
        Assert.assertEquals(numberOfObjects, gigaSpace.count(new Person()));
    }

    public static void assertPersonObjectsInSpaceAfterRebalancing(ProcessingUnit pu, int numberOfObjects) {
        Space space = pu.waitForSpace();
        GigaSpace gigaSpace = space.getGigaSpace();
        Assert.assertEquals(numberOfObjects, gigaSpace.count(new Person()));
    }

    public static void assertNumberOfRelocations(RebalancingSlaEnforcement rebalancing, int expectedNumberOfRelocation) {
        List<FutureStatefulProcessingUnitInstance> relocations = rebalancing.getTrace();

        int actualNumberOfRelocations = 0;

        for (FutureStatefulProcessingUnitInstance future : relocations) {
            if (future.getException() == null && !future.isTimedOut()) {
                actualNumberOfRelocations += 1;
            }
        }

        assertEquals("Number of rebalaning relocations",
                expectedNumberOfRelocation, actualNumberOfRelocations);
    }

    public static ProcessingUnit deployStatelessProcessingUnitOnSingleMachine(
            GridServiceManager gsm, String zone, int numberOfInstances) {
        
//        File archive = DeploymentUtils.getArchive("servlet.war");
        File archive = DeploymentUtils.getArchive("simpleStatelessPu.jar");
        final ProcessingUnit pu = gsm.deploy(
                new ProcessingUnitDeployment(archive)
                .numberOfInstances(numberOfInstances)
                .maxInstancesPerVM(1)
                .addZone(zone));
        
        pu.waitFor(numberOfInstances);
        return pu;
    }

    public static void assertStatelessDeployment(ProcessingUnit pu,
            GridServiceContainer[] containers) {
        
        assertEquals(DeploymentStatus.INTACT,pu.getStatus());
        
        Set<GridServiceContainer> approvedContainers = new HashSet<GridServiceContainer>(Arrays.asList(containers));
        
        for (GridServiceContainer container : pu.getAdmin().getGridServiceContainers()) {
            if (approvedContainers.contains(container)) {
                assertEquals("One instance per container " + RebalancingUtils.gscToString(container),1, container.getProcessingUnitInstances(pu.getName()).length);
            }
            else {
                assertEquals("No instance per container " + RebalancingUtils.gscToString(container),0, container.getProcessingUnitInstances(pu.getName()).length);
            }
        }
        
    }
}
