package test.servicegrid.sla.maxPerMachine;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.SLAUtils.assertMaxPerMachine;
import static test.utils.ToStringUtils.gscToString;
import static test.utils.ToStringUtils.puInstanceToString;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

/**
 * Topology: 1 machine, 1 GSM, 2 GSCs per machine
 * Tests that an instance being relocated to a machine with an instance
 * of the same partition fails, and is re-instantiated at origin.
 * 
 * 1. deploy partition 1,1 max-per-machine = 1
 * 2. verify 1 instance per machine
 * 3. relocate instance from one machine to the other - relocation fails due to max-per-machine
 * 4. instance is re-instantiated at origin
 * 
 * @author Moran Avigdor
 */
public class RelocateFailureMaxPerMachineSlaTest extends AbstractTest {
	private Machine machineA, machineB;
	private GridServiceManager gsm;
	
	@BeforeMethod
	public void setup() {
        assertTrue(admin.getMachines().waitFor(2));
        assertTrue(admin.getGridServiceAgents().waitFor(2));
        
        GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
        GridServiceAgent gsaA = agents[0];
        GridServiceAgent gsaB = agents[1];
        
        machineA = gsaA.getMachine();
        machineB = gsaB.getMachine();
		
		log("loading 1 GSM, 2 GSC on " + machineA.getHostName());
		gsm = loadGSM(machineA);
		loadGSCs(machineA,2);
		
		log("loading, 2 GSC on " + machineB.getHostName());
		loadGSCs(machineB, 2);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws InterruptedException {
		
		log("Deploying cluster A partitioned 1,1 max-per-machine=1");
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("A").partitioned(1, 1).maxInstancesPerMachine(1));
		pu.waitFor(pu.getTotalNumberOfInstances());
		assertEquals("Expected only one instance deployed on machine A", 1, machineA.getProcessingUnitInstances().length);
		assertEquals("Expected only one instance deployed on machine B", 1, machineB.getProcessingUnitInstances().length);
		
		assertMaxPerMachine(admin);
		
		ProcessingUnitInstance puInstance = machineA.getProcessingUnitInstances()[0];
		GridServiceContainer gscOnB = null;
		for (GridServiceContainer gsc : machineB.getGridServiceContainers()) {
			if (gsc.getProcessingUnitInstances().length == 0) {
				gscOnB = gsc; //empty GSC on B
			}
		}
		
		final CountDownLatch machineAaddedLatch = new CountDownLatch(2); //1 existing + 1 re-instantiated
		machineA.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				assertTrue(machineAaddedLatch.getCount() != 0);
				machineAaddedLatch.countDown();
			}
		});
		
		final AtomicInteger machineBaddedCount = new AtomicInteger(0);
		machineB.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				machineBaddedCount.incrementAndGet();
			}
		});
		
		log("relocating " + puInstanceToString(puInstance) + " from "
				+ gscToString(puInstance.getGridServiceContainer()) + " to "
				+ gscToString(gscOnB));
		puInstance.relocate(gscOnB);
		
		
		log("relocation should fail");
		log("wait for instance to be re-instantiated at origin");
		machineAaddedLatch.await();
		
		assertEquals("only 1 instance should instantiate", 1, machineBaddedCount.get());
		assertEquals(DeploymentStatus.INTACT, waitForDeploymentStatus(pu, DeploymentStatus.INTACT));
		assertEquals("Expected only one instance deployed on machine A", 1, machineA.getProcessingUnitInstances().length);
		assertEquals("Expected only one instance deployed on machine B", 1, machineB.getProcessingUnitInstances().length);
		assertMaxPerMachine(admin);
	}
}
