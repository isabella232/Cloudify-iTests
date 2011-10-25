package test.servicegrid.sla.maxPerVm;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.SLAUtils.assertMaxPerVM;
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
 * Topology: 1 machine, 1 GSM 2 GSCs
 * Tests that an instance being relocated to a GSC with an instance
 * of the same partition fails, and is re-instantiated at origin.
 * 
 * 1. deploy partition 1,1 max-per-vm = 1
 * 2. verify 1 instance per GSC
 * 3. relocate instance from one GSC to the other - relocation fails due to max-per-vm
 * 4. instance is re-instantiated at origin
 * 
 * @author Moran Avigdor
 */
public class RelocateFailureMaxPerVmSlaTest extends AbstractTest {
	private Machine machine;
	private GridServiceManager gsm;
	private GridServiceContainer[] gscs;
	
	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading 1 GSM, 2 GSCs on " + machine.getHostName());
		gsm = loadGSM(machine);
		gscs = loadGSCs(machine, 2);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() throws Exception {
		
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("S").partitioned(1, 1).maxInstancesPerVM(1));
		pu.waitFor(2);
		
		assertMaxPerVM(admin);
		
		final CountDownLatch gsc0AddedLatch = new CountDownLatch(2); //1 existing + 1 re-instantiated
		gscs[0].getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				assertTrue(gsc0AddedLatch.getCount() != 0);
				gsc0AddedLatch.countDown();
			}
		});
		
		final AtomicInteger gsc1AddedCount = new AtomicInteger(0);
		gscs[1].getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				gsc1AddedCount.incrementAndGet();
			}
		});
		
		ProcessingUnitInstance puInstance = gscs[0].getProcessingUnitInstances()[0];
		log("relocating " + puInstanceToString(puInstance) + " from " + gscToString(gscs[0]) + " to " + gscToString(gscs[1]));
		puInstance.relocate(gscs[1]);
		
		log("relocation should fail");
		log("wait for instance to be re-instantiated at origin");
		gsc0AddedLatch.await();
		
		assertEquals("only 1 instance should instantiate", 1, gsc1AddedCount.get());
		assertEquals(DeploymentStatus.INTACT, waitForDeploymentStatus(pu, DeploymentStatus.INTACT));
		assertEquals("expected only 2 instances", 2, machine.getProcessingUnitInstances().length);
		assertMaxPerVM(admin);
	}
}
