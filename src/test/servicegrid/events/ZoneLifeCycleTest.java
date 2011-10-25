package test.servicegrid.events;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.zone.Zone;
import org.openspaces.admin.zone.events.ZoneLifecycleEventListener;
import org.testng.annotations.Test;

public class ZoneLifeCycleTest extends EventTestSkel {

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {
		final CountDownLatch addedZoneLatch=new CountDownLatch(2);
		final CountDownLatch removedZoneLatch=new CountDownLatch(2);
		admin.getZones().addLifecycleListener(new ZoneLifecycleEventListener(){

			public void zoneAdded(Zone zone) {
				addedZoneLatch.countDown();
				
			}

			public void zoneRemoved(Zone zone) {
				removedZoneLatch.countDown();
				
			}
			
		});
		gsmA=loadGSM(machineA);
		gsmB=loadGSM(machineB);
		gscA=loadGSC(machineA,"zone1");
		gscB=loadGSC(machineB,"zone2");
		gscA.kill();
		gscB.kill();
		assertTrue("failed to receive zoneAdded notification",addedZoneLatch.await(20, TimeUnit.SECONDS));
		assertTrue("failed to receive zoneRemoved notification",removedZoneLatch.await(20, TimeUnit.SECONDS));
	}

}
