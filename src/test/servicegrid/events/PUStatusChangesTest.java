package test.servicegrid.events;

import static test.utils.DeploymentUtils.getArchive;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.events.BackupGridServiceManagerChangedEvent;
import org.openspaces.admin.pu.events.ManagingGridServiceManagerChangedEvent;
import org.openspaces.admin.pu.events.ProcessingUnitLifecycleEventListener;
import org.openspaces.admin.pu.events.ProcessingUnitStatusChangedEvent;
import org.testng.annotations.Test;

import test.utils.DeploymentUtils;
import test.utils.ProcessingUnitUtils;

public class PUStatusChangesTest extends EventTestSkel {



	@Override
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {
		final CountDownLatch puAddedLatch=new CountDownLatch(2);
		final CountDownLatch puRemovedLatch=new CountDownLatch(2);
		final CountDownLatch puScheduledLatch=new CountDownLatch(2);
		final CountDownLatch puIntactLatch=new CountDownLatch(2);
		final CountDownLatch puNewGSMLatch=new CountDownLatch(2);
		final CountDownLatch puBackupGSMLatch=new CountDownLatch(1);

		admin.addEventListener(new ProcessingUnitLifecycleEventListener(){

			public void processingUnitAdded(ProcessingUnit processingUnit) {
				puAddedLatch.countDown();				
			}

			public void processingUnitRemoved(ProcessingUnit processingUnit) {
				puRemovedLatch.countDown();
			}

			public void processingUnitStatusChanged( 
					ProcessingUnitStatusChangedEvent event) {
				if(event.getNewStatus().equals(DeploymentStatus.SCHEDULED)){
					puScheduledLatch.countDown();
				}else if(event.getNewStatus().equals(DeploymentStatus.INTACT)){
					puIntactLatch.countDown();
				}
			}

			public void processingUnitManagingGridServiceManagerChanged(
					ManagingGridServiceManagerChangedEvent event) {
				if (event.getNewGridServiceManager() != null){
					puNewGSMLatch.countDown();
				}
				
			}

			public void processingUnitBackupGridServiceManagerChanged(
					BackupGridServiceManagerChangedEvent event) {
				puBackupGSMLatch.countDown();
				
			}
			
		});
		loadGS();	
		DeploymentUtils.prepareApp("data");
		ProcessingUnit pu2 = gsmB.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("data", "processor")).partitioned(1, 1));
		try {
			assertTrue("pu2 deployment timed out",pu2.waitFor(pu2.getTotalNumberOfInstances(),60,TimeUnit.SECONDS)); // this deployment should produce 1 scheduled, and 1 intact notifications
			// also  1 processingUnitManagingGridServiceManagerChanged and 1 processingUnitBackupGridServiceManagerChanged
			assertTrue("pu2 active election timed out",ProcessingUnitUtils.waitForActiveElection(pu2,60,TimeUnit.SECONDS));

			ProcessingUnit pu = gsmA.deploy(new ProcessingUnitDeployment(getArchive("petclinic.war")));
			try {
				assertTrue("pu deployment timed out",pu.waitFor(pu.getTotalNumberOfInstances(),60,TimeUnit.SECONDS));// this deployment should produce 1 scheduled and 1 intact notification
				assertTrue("failed to recieve all processingUnitStatusChanged scheduled notifications",puScheduledLatch.await(60,TimeUnit.SECONDS));
				assertTrue("failed to recieve all processingUnitStatusChanged intact notifications",puIntactLatch.await(60,TimeUnit.SECONDS));
				assertTrue("failed to recieve all processingUnitAdded notifications",puAddedLatch.await(60,TimeUnit.SECONDS));					
				assertTrue("failed to recieve all processingUnitManagingGridServiceManagerChanged notifications",puNewGSMLatch.await(60,TimeUnit.SECONDS));
				assertTrue("failed to recieve all processingUnitBackupGridServiceManagerChanged notifications",puBackupGSMLatch.await(60,TimeUnit.SECONDS));
			}
			finally {
				pu.undeploy();
			}
		} 
		finally {
			pu2.undeploy();
		}

		assertTrue("failed to recieve all processingUnitRemoved notifications",puRemovedLatch.await(60,TimeUnit.SECONDS));
	}	

}
