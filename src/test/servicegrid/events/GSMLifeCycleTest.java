package test.servicegrid.events;

import static test.utils.AdminUtils.loadGSM;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.gsm.events.GridServiceManagerLifecycleEventListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class GSMLifeCycleTest extends EventTestSkel {

	

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {
		final CountDownLatch gsmAddLatch=new CountDownLatch(2);
		final CountDownLatch gsmRemoveLatch=new CountDownLatch(2);
		
		admin.addEventListener(new GridServiceManagerLifecycleEventListener(){

			public void gridServiceManagerAdded(
					GridServiceManager gridServiceManager) {
				gsmAddLatch.countDown();
				
			}

			public void gridServiceManagerRemoved(
					GridServiceManager gridServiceManager) {
				gsmRemoveLatch.countDown();
				
			}


		});
		GridServiceManager gsmA=loadGSM(machineA);
		GridServiceManager gsmB=loadGSM(machineB);
		gsmA.kill();
		gsmB.kill();
		assertTrue("failed to recieve gridServiceManagerAdded notification",gsmAddLatch.await(60, TimeUnit.SECONDS));
		assertTrue("failed to recieve gridServiceManagerRemoved notification", gsmRemoveLatch.await(60, TimeUnit.SECONDS));

	}
	
	@AfterMethod
	public void afterTest() {
		// TODO Auto-generated method stub

	}

}
