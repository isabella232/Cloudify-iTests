package test.servicegrid.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.admin.space.events.SpaceModeChangedEvent;
import org.openspaces.admin.space.events.SpaceModeChangedEventListener;
import org.testng.annotations.Test;

import test.utils.ProcessingUnitUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;

public class SpaceModeChangedTest extends EventTestSkel {

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {
		final CountDownLatch primarySpaceModeLatch=new CountDownLatch(2);
		final CountDownLatch backupSpaceModeLatch=new CountDownLatch(2);
		admin.getSpaces().getSpaceModeChanged().add(new SpaceModeChangedEventListener(){

			public void spaceModeChanged(SpaceModeChangedEvent event) {
				if (event.getNewMode().equals(SpaceMode.PRIMARY)){
					primarySpaceModeLatch.countDown();
				}else if(event.getNewMode().equals(SpaceMode.BACKUP)){
					backupSpaceModeLatch.countDown();
				}
			}
			
		});
		
		loadGS();
		ProcessingUnit puA = gsmA.deploy(new SpaceDeployment("A").partitioned(1, 1));
		puA.waitFor(puA.getTotalNumberOfInstances());
		puA.waitForSpace();
		ProcessingUnitUtils.waitForActiveElection(puA);
		//restart primary space causes 2 notification -> 1 primary, 1 backup
		SpaceInstance[] spaces=puA.getSpace().getInstances();
		for(ProcessingUnitInstance p : gscA.getProcessingUnitInstances()){
			if(p.getSpaceInstance().getMode().equals(SpaceMode.PRIMARY)){
				p.restartAndWait();
				p.waitForSpaceInstance();
				break;
			}
		}
		ProcessingUnitUtils.waitForActiveElection(puA);
		assertTrue("failed to recieve primary spaceModeChanged notification", primarySpaceModeLatch. await(60, TimeUnit.SECONDS));
		assertTrue("failed to recieve backup spaceModeChanged notification", backupSpaceModeLatch.await(60, TimeUnit.SECONDS));
	}	

}
