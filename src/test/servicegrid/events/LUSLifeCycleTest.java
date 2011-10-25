package test.servicegrid.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.lus.LookupService;
import org.openspaces.admin.lus.events.LookupServiceLifecycleEventListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.AdminUtils;

public class LUSLifeCycleTest extends EventTestSkel {

	private LookupService lus;

	@Override
	@BeforeMethod
	public void setup() throws Exception{
		super.setup();
		lus = AdminUtils.loadLUS(machineA.getGridServiceAgent());
	}
	
	 // --------> disable this test until we find a way to kill a LUS that will not affect the following tests in the regression
	@Override
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled=false)
	public void test() throws Exception {
		final CountDownLatch addedLUSLatch=new CountDownLatch(1);
		final CountDownLatch removedLUSLatch=new CountDownLatch(1);
		admin.getLookupServices().addLifecycleListener(new LookupServiceLifecycleEventListener(){

			public void lookupServiceAdded(LookupService lookupService) {
				if (lookupService.equals(lus)) {
					addedLUSLatch.countDown();
				}
			}

			public void lookupServiceRemoved(LookupService lookupService) {
				if (lookupService.equals(lus)) {
					removedLUSLatch.countDown();
				}
			}
		});
		

		assertTrue("failed to receive lookupServiceAdded notification",addedLUSLatch.await(20, TimeUnit.SECONDS));

		lus.kill();
		assertTrue("failed to receive lookupServiceRemoved notification",removedLUSLatch.await(20, TimeUnit.SECONDS));

	}
	

}
