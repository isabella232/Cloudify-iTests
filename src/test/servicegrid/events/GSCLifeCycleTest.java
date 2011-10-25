package test.servicegrid.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.events.GridServiceContainerLifecycleEventListener;
import org.testng.annotations.Test;

public class GSCLifeCycleTest extends EventTestSkel {

	

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {
		final CountDownLatch gscAddLatch=new CountDownLatch(2);
		final CountDownLatch gscRemoveLatch=new CountDownLatch(2);
		admin.addEventListener(new GridServiceContainerLifecycleEventListener(){

			public void gridServiceContainerAdded(
					GridServiceContainer gridServiceContainer) {
				gscAddLatch.countDown();
			}

			public void gridServiceContainerRemoved(
					GridServiceContainer gridServiceContainer) {
				gscRemoveLatch.countDown();
			}
			
		});
		loadGS();
		assertTrue("failed to recieve gridServiceContainerAdded notification",gscAddLatch.await(60, TimeUnit.SECONDS));
		testCleanup();
		assertTrue("failed to recieve gridServiceContainerRemoved notification", gscRemoveLatch.await(60, TimeUnit.SECONDS));
	}

}
