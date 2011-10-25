package test.servicegrid.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.admin.space.events.SpaceInstanceLifecycleEventListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.ProcessingUnitUtils;

public class SpaceInstanceLifeCycleTest extends EventTestSkel {

	@BeforeMethod
	public void setup() throws Exception{
		super.setup();
		loadGS();
	}
		
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {
		final CountDownLatch spaceInstanceAddedLatch=new CountDownLatch(4);
		final CountDownLatch spaceInstanceRemovedLatch=new CountDownLatch(4);
		
		admin.getSpaces().addLifecycleListener(new SpaceInstanceLifecycleEventListener(){
			public void spaceInstanceAdded(SpaceInstance spaceInstance) {
				spaceInstanceAddedLatch.countDown();
			}
			
			public void spaceInstanceRemoved(SpaceInstance spaceInstance) {
				spaceInstanceRemovedLatch.countDown();
			}
		});
		
		ProcessingUnit puA = gsmA.deploy(new SpaceDeployment("A").partitioned(2, 1));
		puA.waitForSpace();
		ProcessingUnitUtils.waitForActiveElection(puA);
		assertTrue("failed to recieve spaceInstanceAdded notification", spaceInstanceAddedLatch.await(60, TimeUnit.SECONDS));
		puA.undeploy();
		assertTrue("failed to recieve spaceInstanceRemoved notification", spaceInstanceRemovedLatch.await(60, TimeUnit.SECONDS));
	}
	

}
