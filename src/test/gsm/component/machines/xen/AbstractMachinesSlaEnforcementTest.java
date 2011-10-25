package test.gsm.component.machines.xen;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.internal.admin.InternalAdmin;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.cloud.xenserver.XenServerMachineProvisioning;
import org.openspaces.cloud.xenserver.XenServerMachineProvisioningConfig;
import org.openspaces.grid.gsm.capacity.CapacityRequirements;
import org.openspaces.grid.gsm.capacity.MemoryCapacityRequirement;
import org.openspaces.grid.gsm.machines.CapacityMachinesSlaPolicy;
import org.openspaces.grid.gsm.machines.MachinesSlaEnforcement;
import org.openspaces.grid.gsm.machines.MachinesSlaEnforcementEndpoint;
import org.openspaces.grid.gsm.machines.isolation.DedicatedMachineIsolation;
import org.openspaces.grid.gsm.machines.plugins.ElasticMachineProvisioning;
import org.openspaces.grid.gsm.machines.plugins.NonBlockingElasticMachineProvisioningAdapterFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import test.gsm.AbstractXenGSMTest;
import test.gsm.component.SlaEnforcementTestUtils;
import test.utils.LogUtils;

public abstract class AbstractMachinesSlaEnforcementTest extends AbstractXenGSMTest {

    protected final static long CONTAINER_MEGABYTES = 250;
	protected MachinesSlaEnforcement machinesSlaEnforcement;
    protected MachinesSlaEnforcementEndpoint endpoint;
    protected ProcessingUnit pu;
    protected XenServerMachineProvisioning machineProvisioning; 
    private static NonBlockingElasticMachineProvisioningAdapterFactory nonblockingAdapterFactory = new NonBlockingElasticMachineProvisioningAdapterFactory();
    protected static final String ZONE = "test_zone";
    protected static final String WRONG_ZONE = "wrong_test_zone";
    
    @BeforeMethod
    public void beforeTest() {
        super.beforeTest();
        
        updateMachineProvisioningConfig(getMachineProvisioningConfig());
        machinesSlaEnforcement = new MachinesSlaEnforcement(admin);
        pu = gsm.deploy(new SpaceDeployment("testspace").partitioned(10,1).addZone(ZONE));
    }

	protected void updateMachineProvisioningConfig(XenServerMachineProvisioningConfig config) {
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
			machineProvisioning = new XenServerMachineProvisioning();
	        machineProvisioning.setAdmin(admin);
	        machineProvisioning.setProperties(config.getProperties());
	        try {
	            machineProvisioning.afterPropertiesSet();
	        } catch (final Exception e) {
	            e.printStackTrace();
	            Assert.fail("Failed to initialize elastic scale handler",e);
	        }
		}
	}
    
    @AfterMethod
    public void afterTest() {
        machinesSlaEnforcement.destroyEndpoint(pu);
        pu.undeploy();
        
        try {
            machinesSlaEnforcement.destroy();
        } catch (Exception e) {
            Assert.fail("Failed to destroy machinesSlaEnforcement",e);
        }
        
        try {
            machineProvisioning.destroy();
        } catch (Exception e) {
            Assert.fail("Failed to destroy machineProvisioning",e);
        }
        super.afterTest();
    }
    

    protected void enforceNumberOfMachines(int numberOfMachines) throws InterruptedException {
    	XenServerMachineProvisioningConfig config = getMachineProvisioningConfig();
        config.setDedicatedManagementMachines(true);
		updateMachineProvisioningConfig(config);
		CapacityMachinesSlaPolicy sla = createSla(numberOfMachines);
		SlaEnforcementTestUtils.enforceSlaAndWait(admin, endpoint, sla, machineProvisioning);
    }

    protected CapacityMachinesSlaPolicy createSla(int numberOfMachines) {
        long minimumMemoryInMB = Math.min(numberOfMachines,2) * CONTAINER_MEGABYTES;
		long maximumMemoryInMB = pu.getTotalNumberOfInstances() * CONTAINER_MEGABYTES;
		long memoryCapacityPerMachineInMB = super.getMachineProvisioningConfig().getMemoryCapacityPerMachineInMB() - super.getMachineProvisioningConfig().getReservedMemoryCapacityPerMachineInMB();
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
        SlaEnforcementTestUtils.enforceSlaAndWait(admin, endpoint, sla, machineProvisioning);
    }
    
    protected CapacityMachinesSlaPolicy createSla(int numberOfMachines, ElasticMachineProvisioning machineProvisioning, long memoryCapacityInMB) {
        CapacityMachinesSlaPolicy sla = new CapacityMachinesSlaPolicy();
        sla.setCapacityRequirements(new CapacityRequirements(new MemoryCapacityRequirement(memoryCapacityInMB)));
        sla.setMinimumNumberOfMachines(Math.min(numberOfMachines,2));
        sla.setMaximumNumberOfMachines(pu.getTotalNumberOfInstances());
        sla.setMaximumNumberOfContainersPerMachine(pu.getTotalNumberOfInstances());
        sla.setContainerMemoryCapacityInMB(250);
        sla.setMachineProvisioning(nonblockingAdapterFactory.create(machineProvisioning));
        sla.setMachineIsolation(new DedicatedMachineIsolation(pu.getName()));
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
		return gsa.startGridServiceAndWait(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.zones="+containerZone));
	}

}
