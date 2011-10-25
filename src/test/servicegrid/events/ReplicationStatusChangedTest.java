package test.servicegrid.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.ReplicationStatus;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.space.events.ReplicationStatusChangedEvent;
import org.openspaces.admin.space.events.ReplicationStatusChangedEventListener;
import org.testng.Assert;
import org.testng.annotations.Test;

import test.utils.ProcessingUnitUtils;

public class ReplicationStatusChangedTest extends EventTestSkel {
	


	@Override
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {

		
		final CountDownLatch disconnectedStatusLatch= new CountDownLatch(1);
		final CountDownLatch activeStatusLatch= new CountDownLatch(2);

		admin.getSpaces().getReplicationStatusChanged().add(new ReplicationStatusChangedEventListener(){

			public void replicationStatusChanged(
					ReplicationStatusChangedEvent event) { 
				if (event.getNewStatus().equals(ReplicationStatus.ACTIVE)){
					activeStatusLatch.countDown();
				}
				else if (event.getNewStatus().equals(ReplicationStatus.DISCONNECTED)){
					disconnectedStatusLatch.countDown();
				}
				else {						
					Assert.fail("status notification isnt expected: "+event.getNewStatus());
				}
			}
		});
		loadGS();
		ProcessingUnit pu = gsmA.deploy(new SpaceDeployment("A").partitioned(1, 1));
		pu.waitFor(pu.getTotalNumberOfInstances());
		pu.waitForSpace();
		ProcessingUnitUtils.waitForActiveElection(pu);
		
		for(ProcessingUnitInstance pui : pu.getInstances()){
			pui.restartAndWait();
			break;
		}
		//we should get 1 active (replication)
		//the restart causes 2 notifications, 1 disconnected ( for the space that wasn't restarted) and 1 active (replication)
		assertTrue("failed to receive disconnected notification",disconnectedStatusLatch.await(60, TimeUnit.SECONDS));
		assertTrue("failed to receive active notification",activeStatusLatch.await(60, TimeUnit.SECONDS));		

	}	

}
