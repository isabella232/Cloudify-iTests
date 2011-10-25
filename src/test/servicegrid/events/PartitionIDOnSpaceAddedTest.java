package test.servicegrid.events;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.admin.space.events.SpaceInstanceLifecycleEventListener;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.ProcessingUnitUtils;
import test.utils.ThreadBarrier;

public class PartitionIDOnSpaceAddedTest extends EventTestSkel {

	@BeforeMethod
	public void setup() throws Exception{
		super.setup();
		loadGS();
	}
		
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {
		final ThreadBarrier barrier = new ThreadBarrier(2);
		
		
		admin.getSpaces().addLifecycleListener(new SpaceInstanceLifecycleEventListener(){
			private AtomicInteger lastPartitionID = new AtomicInteger(-1);
			public void spaceInstanceAdded(SpaceInstance spaceInstance) {
				
				if(!lastPartitionID.compareAndSet(-1, spaceInstance.getPartition().getPartitionId())){
					try{
						assertTrue("Both partitions ids are: "+lastPartitionID,lastPartitionID.get()!=spaceInstance.getPartition().getPartitionId());
						barrier.await();
					}catch(Error e){
						barrier.reset(new Throwable(e.getMessage()));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}					
			}
			
			public void spaceInstanceRemoved(SpaceInstance spaceInstance) {
			
			}
		});
		
		ProcessingUnit puA = gsmA.deploy(new SpaceDeployment("A").partitioned(2, 0));
		puA.waitForSpace();
		ProcessingUnitUtils.waitForActiveElection(puA);
		try{
			barrier.await(60,TimeUnit.SECONDS);
		}catch(TimeoutException e){
			Assert.fail("failed to receive 2 spaceAdded notifications");
		}catch(BrokenBarrierException e){
			Assert.fail(e.getCause().getMessage());
		}
	}
	

}
