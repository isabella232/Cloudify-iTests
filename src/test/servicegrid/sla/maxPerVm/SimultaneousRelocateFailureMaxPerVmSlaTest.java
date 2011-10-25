package test.servicegrid.sla.maxPerVm;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.SLAUtils.assertMaxPerVM;
import static test.utils.ToStringUtils.gscToString;
import static test.utils.ToStringUtils.puInstanceToString;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
 * Topology: 1 machine, 1 GSM 3 GSCs Tests that two instances of same partition
 * are simultaneously relocated to the same GSC - one succeeds, the other fails
 * and is re-instantiated.
 * 
 * 1. deploy partition 1,1 max-per-vm = 1 
 * 2. verify 1 instance per GSC 
 * 3. load 3rd GSC
 * 4. relocate both instances simultaneously to same GSC
 * 5. One instance succeeds, the other will fail and is re-instantiated
 * 
 * @see GS-8739
 * @author Moran Avigdor
 */
public class SimultaneousRelocateFailureMaxPerVmSlaTest extends AbstractTest {
	private Machine machine;
	private GridServiceManager gsm;
	private GridServiceContainer[] gscs;
	private GridServiceContainer targetGSC;
	
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
		
		targetGSC = loadGSC(machine);
		
		final AtomicInteger  addedCount = new AtomicInteger();
		final CountDownLatch rellocationLatch = new CountDownLatch(2);
		
		gscs[0].getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				addedCount.incrementAndGet();
				rellocationLatch.countDown();
			}
		},false);
		
		gscs[1].getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				assertTrue(rellocationLatch.getCount() != 0);
				rellocationLatch.countDown();
			}
		},false);
		
		targetGSC.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				addedCount.incrementAndGet();
				rellocationLatch.countDown();
			}
		},false);
		
		/*
		 * simultaneously relocate each of the instances to the 3rd target GSC.
		 * note: there is no need to do this in separate threads - this is fast enough to recreate the original bug.
		 */
		ProcessingUnitInstance puInstance1 = gscs[0].getProcessingUnitInstances()[0];
		log("relocating " + puInstanceToString(puInstance1) + " from " + gscToString(gscs[0]) + " to " + gscToString(targetGSC));
		puInstance1.relocate(targetGSC);
		
		ProcessingUnitInstance puInstance2 = gscs[1].getProcessingUnitInstances()[0];
		log("relocating " + puInstanceToString(puInstance2) + " from " + gscToString(gscs[0]) + " to " + gscToString(targetGSC));
		puInstance2.relocate(targetGSC);
		
		log("one relocation should succeed, the other should fail and re-instantiate");
		log("wait for 1.5 seconds for relocation to complete...");
		boolean await = rellocationLatch.await(90, TimeUnit.SECONDS);
		assertTrue("relocation failed to complete!", await);
		assertEquals("expected 2 events of instantiations", 2, addedCount.get());
		
		assertEquals(DeploymentStatus.INTACT, waitForDeploymentStatus(pu, DeploymentStatus.INTACT));
		assertEquals("expected only 2 instances", 2, machine.getProcessingUnitInstances().length);
		assertMaxPerVM(admin);
	}
}
