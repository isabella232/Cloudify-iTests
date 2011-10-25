package test.servicegrid.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.space.events.SpaceLifecycleEventListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.ProcessingUnitUtils;

public class SpaceLifeCycleTest extends EventTestSkel {

	@BeforeMethod
	public void setup() throws Exception{
		super.setup();
		loadGS();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {
		final CountDownLatch spaceAddedLatch=new CountDownLatch(1);
		final CountDownLatch spaceRemovedLatch=new CountDownLatch(1);
		admin.getSpaces().addLifecycleListener(new SpaceLifecycleEventListener(){

			public void spaceAdded(Space space) {
				spaceAddedLatch.countDown();
			}

			public void spaceRemoved(Space space) {
				spaceRemovedLatch.countDown();
			}
		});
		
		ProcessingUnit puA = gsmA.deploy(new SpaceDeployment("A").partitioned(1, 1).maxInstancesPerMachine(1));
		puA.waitForSpace();	
		ProcessingUnitUtils.waitForActiveElection(puA);
		assertTrue("failed to recieve spaceAdded notification",spaceAddedLatch.await(60, TimeUnit.SECONDS));
		
		puA.undeploy();
		
		assertTrue("failed to recieve spaceRemoved notification", spaceRemovedLatch.await(60, TimeUnit.SECONDS));
	}
	
}
