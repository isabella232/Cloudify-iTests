package org.cloudifysource.quality.iTests.test.esm.component.machines;

import org.cloudifysource.esc.driver.provisioning.CloudifyMachineProvisioningConfig;
import org.cloudifysource.esc.driver.provisioning.ElasticMachineProvisioningCloudifyAdapter;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.cloudifysource.quality.iTests.test.esm.component.SlaEnforcementTestUtils;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.internal.admin.InternalAdmin;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.zone.config.AnyZonesConfig;
import org.openspaces.grid.gsm.capacity.CapacityRequirements;
import org.openspaces.grid.gsm.capacity.MemoryCapacityRequirement;
import org.openspaces.grid.gsm.machines.CapacityMachinesSlaPolicy;
import org.openspaces.grid.gsm.machines.MachinesSlaEnforcement;
import org.openspaces.grid.gsm.machines.MachinesSlaEnforcementEndpoint;
import org.openspaces.grid.gsm.machines.isolation.DedicatedMachineIsolation;
import org.openspaces.grid.gsm.machines.isolation.ElasticProcessingUnitMachineIsolation;
import org.openspaces.grid.gsm.machines.plugins.ElasticMachineProvisioning;
import org.openspaces.grid.gsm.machines.plugins.NonBlockingElasticMachineProvisioningAdapterFactory;
import org.testng.Assert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractMachinesSlaEnforcementTest extends AbstractFromXenToByonGSMTest {

    protected static final String PU_NAME = "testspace";
	protected final static long CONTAINER_MEGABYTES = 250;
	protected MachinesSlaEnforcement machinesSlaEnforcement;
    protected MachinesSlaEnforcementEndpoint endpoint;
    protected ProcessingUnit pu;
    protected ElasticMachineProvisioningCloudifyAdapter machineProvisioning; 
    private static NonBlockingElasticMachineProvisioningAdapterFactory nonblockingAdapterFactory = new NonBlockingElasticMachineProvisioningAdapterFactory();
    protected static final String ZONE = "test_zone";
    protected static final String WRONG_ZONE = "wrong_test_zone";
    

	protected void updateMachineProvisioningConfig(CloudifyMachineProvisioningConfig config) {
		if (machineProvisioning != null) {
			LogUtils.log("machineProvisioning:"+machineProvisioning.getConfig().getProperties().toString());
		}
		LogUtils.log("config.machineProvisioning:"+config.getProperties().toString());
		if (machineProvisioning == null || 
		    !machineProvisioning.getProperties().equals(config.getProperties())) {
			
			if (machineProvisioning != null) {
				try {
					machineProvisioning.destroy();
					machineProvisioning=null;
				} catch (Exception e) {
		            Assert.fail("Failed to destroy machinesSlaEnforcement",e);
		        }
			}
			machineProvisioning = new ElasticMachineProvisioningCloudifyAdapter();
	        machineProvisioning.setAdmin(admin);
	        config.setCloudConfigurationDirectory(getService().getPathToCloudFolder());
	        machineProvisioning.setProperties(config.getProperties());
	        ElasticProcessingUnitMachineIsolation isolation = new DedicatedMachineIsolation(PU_NAME);
	        machineProvisioning.setElasticProcessingUnitMachineIsolation(isolation);

	        machineProvisioning.setElasticGridServiceAgentProvisioningProgressEventListener(agentEventListener);

	        machineProvisioning.setElasticMachineProvisioningProgressChangedEventListener(machineEventListener);
	        try {
	            machineProvisioning.afterPropertiesSet();
	        } catch (final Exception e) {
	            e.printStackTrace();
	            Assert.fail("Failed to initialize elastic scale handler",e);
	        }
		}
	}
    
    protected void enforceNumberOfMachines(int numberOfMachines) throws InterruptedException {
        CloudifyMachineProvisioningConfig config = getMachineProvisioningConfig();
        config.setDedicatedManagementMachines(true);
        updateMachineProvisioningConfig(config);
        CapacityMachinesSlaPolicy sla = createSla(numberOfMachines);
        SlaEnforcementTestUtils.enforceSlaAndWait(admin, endpoint, sla, machineProvisioning);
    }

    protected void enforceNumberOfMachinesOneContainerPerMachine(int numberOfMachines) throws InterruptedException {
        CloudifyMachineProvisioningConfig config = getMachineProvisioningConfig();
        config.setDedicatedManagementMachines(true);
        updateMachineProvisioningConfig(config);
        CapacityMachinesSlaPolicy sla = createSlaOneContainerPerMachine(numberOfMachines);
        SlaEnforcementTestUtils.enforceSlaAndWait(admin, endpoint, sla, machineProvisioning);
    }

    protected CapacityMachinesSlaPolicy createSlaOneContainerPerMachine(int numberOfMachines) {
        long MACHINE_MEMORY_CAPACITY_MB = (long)getFirstManagementMachinePhysicalMemoryInMB();
        long memoryCapacityPerMachineInMB = MACHINE_MEMORY_CAPACITY_MB - super.getMachineProvisioningConfig().getReservedMemoryCapacityPerMachineInMB();
        memoryCapacityPerMachineInMB -= memoryCapacityPerMachineInMB % CONTAINER_MEGABYTES;
        long memoryCapacityInMB = numberOfMachines * CONTAINER_MEGABYTES;

        return createSlaOneContainerPerMachine(numberOfMachines,machineProvisioning, memoryCapacityInMB);
    }

    protected CapacityMachinesSlaPolicy createSla(int numberOfMachines) {
        long MACHINE_MEMORY_CAPACITY_MB = (long)getFirstManagementMachinePhysicalMemoryInMB();
        long minimumMemoryInMB = Math.min(numberOfMachines,2) * CONTAINER_MEGABYTES;
		long maximumMemoryInMB = pu.getTotalNumberOfInstances() * CONTAINER_MEGABYTES;
		long memoryCapacityPerMachineInMB = MACHINE_MEMORY_CAPACITY_MB - super.getMachineProvisioningConfig().getReservedMemoryCapacityPerMachineInMB();
		memoryCapacityPerMachineInMB -= memoryCapacityPerMachineInMB % CONTAINER_MEGABYTES;
		long memoryCapacityInMB = numberOfMachines * memoryCapacityPerMachineInMB;
		if (memoryCapacityInMB < minimumMemoryInMB) {
			memoryCapacityInMB = minimumMemoryInMB;
		}
		if (memoryCapacityInMB > maximumMemoryInMB) {
			memoryCapacityInMB = maximumMemoryInMB;
		}
		return createSla(numberOfMachines,machineProvisioning, memoryCapacityInMB);
    }
    
    protected void enforceUndeploy() throws InterruptedException {
        CapacityMachinesSlaPolicy sla = new CapacityMachinesSlaPolicy();
        sla.setCapacityRequirements(new CapacityRequirements());
        sla.setMinimumNumberOfMachines(0);
        sla.setMaximumNumberOfMachines(pu.getTotalNumberOfInstances());
        sla.setMaximumNumberOfContainersPerMachine(pu.getTotalNumberOfInstances());
        sla.setContainerMemoryCapacityInMB(CONTAINER_MEGABYTES);
        sla.setMachineProvisioning(nonblockingAdapterFactory.create(machineProvisioning));
        sla.setMachineIsolation(new DedicatedMachineIsolation(pu.getName()));
        sla.setGridServiceAgentZones(new AnyZonesConfig());
        SlaEnforcementTestUtils.enforceSlaAndWait(admin, endpoint, sla, machineProvisioning);
    }
    
    protected CapacityMachinesSlaPolicy createSla(int numberOfMachines, ElasticMachineProvisioning machineProvisioning, long memoryCapacityInMB) {
        CapacityMachinesSlaPolicy sla = new CapacityMachinesSlaPolicy();
        sla.setCapacityRequirements(new CapacityRequirements(new MemoryCapacityRequirement(memoryCapacityInMB)));
        sla.setMinimumNumberOfMachines(Math.min(numberOfMachines,2));
        sla.setMaximumNumberOfMachines(pu.getTotalNumberOfInstances());
        sla.setMaximumNumberOfContainersPerMachine(pu.getTotalNumberOfInstances());
        sla.setContainerMemoryCapacityInMB(CONTAINER_MEGABYTES);
        sla.setMachineProvisioning(nonblockingAdapterFactory.create(machineProvisioning));
        sla.setMachineIsolation(new DedicatedMachineIsolation(pu.getName()));
        sla.setGridServiceAgentZones(new AnyZonesConfig());
        return sla;
    }

    protected CapacityMachinesSlaPolicy createSlaOneContainerPerMachine(int numberOfMachines, ElasticMachineProvisioning machineProvisioning, long memoryCapacityInMB) {
        CapacityMachinesSlaPolicy sla = new CapacityMachinesSlaPolicy();
        sla.setCapacityRequirements(new CapacityRequirements(new MemoryCapacityRequirement(memoryCapacityInMB)));
        sla.setMinimumNumberOfMachines(Math.min(numberOfMachines,2));
        sla.setMaximumNumberOfMachines(pu.getTotalNumberOfInstances());
        sla.setMaximumNumberOfContainersPerMachine(1);
        sla.setContainerMemoryCapacityInMB(CONTAINER_MEGABYTES);
        sla.setMachineProvisioning(nonblockingAdapterFactory.create(machineProvisioning));
        sla.setMachineIsolation(new DedicatedMachineIsolation(pu.getName()));
        sla.setGridServiceAgentZones(new AnyZonesConfig());
        return sla;
    }

    protected static MachinesSlaEnforcementEndpoint createEndpoint(final ProcessingUnit pu,
            final MachinesSlaEnforcement machinesSlaEnforcement) {
        final AtomicReference<MachinesSlaEnforcementEndpoint> endpointRef = new AtomicReference<MachinesSlaEnforcementEndpoint>();
        final CountDownLatch latch = new CountDownLatch(1);
        ((InternalAdmin) pu.getAdmin())
                .scheduleNonBlockingStateChange(new Runnable() {
                    public void run() {
                        MachinesSlaEnforcementEndpoint endpoint = machinesSlaEnforcement.createEndpoint(pu);
                        endpointRef.set(endpoint);
                        latch.countDown();
                    }
                });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Assert.fail("Interrupted", e);
        }
        return endpointRef.get();
    }

	protected GridServiceContainer startContainerOnAgent(GridServiceAgent gsa) {
		final String containerZone = pu.getRequiredZones()[0];
		return gsa.startGridServiceAndWait(new GridServiceContainerOptions()
                .vmInputArgument("-Dcom.gs.zones="+containerZone)
                .vmInputArgument("-Dcom.gs.transport_protocol.lrmi.bind-port="+LRMI_BIND_PORT_RANGE));
	}

}
