package test.servicegrid.failover;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.DistributionUtils.assertBackups;
import static test.utils.DistributionUtils.assertPrimaries;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;
import static test.utils.SLAUtils.assertMaxPerVM;
import static test.utils.ToStringUtils.gscToString;
import static test.utils.ToStringUtils.puInstanceToString;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.GridServiceContainers;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener;
import org.testng.annotations.Test;

import test.servicegrid.sla.maxPerVm.MaxPerVmSlaOneMachineTest;

import com.gigaspaces.cluster.activeelection.SpaceMode;
/**
 * Topology:
 * - 1 machine
 * - 1 GSM and 2 GSC at Machine
 * - Cluster partitioned 2,1 max-per-vm = 1
 * 
 * Test max-per-vm SLA is obeyed after failover
 * 
 * Start 1 GSM, 2 GSCs
 * Deploy partitioned 2,1 max-per-vm=1
 * 
 * a. evenly distributed across 2 GSCs
 * b. primary on each GSC
 * c. 2 primaries, 2 backups
 * --
 * d. kill -9 GSC 2
 * e. primaries are elected on GSC 1
 * f. backups are not instantiated on GSC 1
 * g. start GSC 2
 * h. backups are instantiated on GSC 2
 * 
 * @author Moran Avigdor
 */
public class MaxPerVmSlaAfterFailoverTest extends MaxPerVmSlaOneMachineTest {
	
	@Override
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() {
		super.test();
		
		killGsc2();
		assertMaxPerVM(admin);
		
		startGsc2();
		assertMaxPerVM(admin);
	}

	private void killGsc2() {
		GridServiceContainers gridServiceContainers = admin.getGridServiceContainers();
		GridServiceContainer gsc1 = gridServiceContainers.getContainers()[0];
		GridServiceContainer gsc2 = gridServiceContainers.getContainers()[1];
		log("kill GSC#2: " + gscToString(gsc2));
		gsc2.kill();

		//2 Spaces are available both primary
		log("wait for active election of primaries ...");
		for (ProcessingUnitInstance puInstance : gsc1.getProcessingUnitInstances()) {
			assertTrue("wait for PRIMARY mode change for " + getProcessingUnitInstanceName(puInstance),
					puInstance.getSpaceInstance().waitForMode(
							SpaceMode.PRIMARY, 60000, TimeUnit.MILLISECONDS));
		}
		
		assertPrimaries(admin, 2);
		assertBackups(admin, 0);
	}
	
	private void startGsc2() {
		GridServiceContainers gridServiceContainers = admin.getGridServiceContainers();
		GridServiceContainer gsc1 = gridServiceContainers.getContainers()[0];
		log("start GSC#2");
		GridServiceContainer gsc2 = loadGSC(gsc1.getMachine()); //load GSC2 on same machine as GSC1
		log("started GSC#2: " + gscToString(gsc2));
		
		final CountDownLatch addedLatch = new CountDownLatch(2);
		gsc2.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				log("processing unit added event: " + puInstanceToString(processingUnitInstance));
				addedLatch.countDown();				
			}
		});
		
		try {
			addedLatch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		assertPrimaries(admin, 2);
		assertBackups(admin, 2);
	}
}
