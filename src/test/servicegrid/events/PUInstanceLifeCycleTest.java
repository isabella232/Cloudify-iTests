package test.servicegrid.events;

import static test.utils.DeploymentUtils.getArchive;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceLifecycleEventListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.DeploymentUtils;

public class PUInstanceLifeCycleTest extends EventTestSkel {
	
	@BeforeMethod
	public void setup() throws Exception{
		super.setup();
		loadGS();
	}
	
	

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {
		final CountDownLatch pu1AddedLatch=new CountDownLatch(4);
		final CountDownLatch pu2AddedLatch=new CountDownLatch(1);
		final CountDownLatch pu1RemovedLatch=new CountDownLatch(4);
		final CountDownLatch pu2RemovedLatch=new CountDownLatch(1);
		
		admin.addEventListener(new ProcessingUnitInstanceLifecycleEventListener(){

			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				if (pu1AddedLatch.getCount()>0){
					pu1AddedLatch.countDown();
				}else{
					pu2AddedLatch.countDown();
				}
				
			}

			public void processingUnitInstanceRemoved(
					ProcessingUnitInstance processingUnitInstance) {
				if (pu1RemovedLatch.getCount()>0){
					pu1RemovedLatch.countDown();
				}else{
					pu2RemovedLatch.countDown();
				}
				
			}
			
		});
		
		DeploymentUtils.prepareApp("data");
		ProcessingUnit pu = gsmB.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("data", "processor")));
		pu.waitFor(pu.getTotalNumberOfInstances());
		assertTrue("failed to receive processingUnitInstanceAdded notification - pu1",pu1AddedLatch.await(20, TimeUnit.SECONDS));
			
		ProcessingUnit pu2 = gsmA.deploy(new ProcessingUnitDeployment(getArchive("petclinic.war")));
		pu2.waitFor(pu2.getTotalNumberOfInstances());
		assertTrue("failed to receive processingUnitInstanceAdded notification - pu2",pu2AddedLatch.await(20, TimeUnit.SECONDS));
		
		pu.undeploy();
		assertTrue("failed to receive processingUnitInstanceRemoved notification - pu1",pu1RemovedLatch.await(20, TimeUnit.SECONDS));
		
		pu2.undeploy();
		assertTrue("failed to receive processingUnitInstanceRemoved notification - pu2",pu2RemovedLatch.await(20, TimeUnit.SECONDS));
		
	}
	
}
