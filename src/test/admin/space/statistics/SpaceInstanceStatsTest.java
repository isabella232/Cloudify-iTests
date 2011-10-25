package test.admin.space.statistics;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.admin.space.SpaceInstanceStatistics;
import org.openspaces.core.executor.Task;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.TestUtils;

/**
 * @author rafi
 *
 */
public class SpaceInstanceStatsTest extends AbstractTest {

	public class TestData{
	    TestData() {}
	    TestData(int expectedTP) { 
	        this.expectedTP = expectedTP;
	    }
		CountDownLatch endOfActionsLatch = new CountDownLatch(numberOfSecondsToRun-2);;
		int expectedTP = 11;
		Timer timer = new Timer();
	}
	
	public interface StatMethod{
		double getStats(SpaceInstanceStatistics statistics);
	}
	
	private GridServiceManager gsm;
	private Space currSpace;
	private final int numberOfSecondsToRun = 20; //for each test
	private TestData td;

	@BeforeMethod
	public void setup(){
		this.gsm = TestUtils.waitAndLoad(admin, 1,1);
		admin.setStatisticsInterval(1, TimeUnit.SECONDS);
		admin.startStatisticsMonitor();
		td = new TestData();
	}	

	public void readWriteTakeTestBody(TimerTask t,TestData td,StatMethod statMethod, String testName) throws InterruptedException{
		TestUtils.deploySpace(gsm, "A",1,0);
		for (Space space : admin.getSpaces()){
			currSpace = space;
			for(SpaceInstance spaceInstance : space.getInstances()){
			    td.timer.schedule(t,0L, 1000L);
			    td.endOfActionsLatch.await(60, TimeUnit.SECONDS);
				SpaceInstanceStatistics statistics = spaceInstance.getStatistics();
				int stats=(int)Math.round(statMethod.getStats(statistics));
				td.timer.cancel();
				assertTrue("failed to receive correct "+testName+" statistics, should be "+td.expectedTP+" while its "+stats,stats==td.expectedTP);				
				break;
			}
		}
	}
		
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="2")
	public void testReadPerSec() throws InterruptedException{
		readWriteTakeTestBody(new TimerTask(){
	    	int numOfLoops = numberOfSecondsToRun; 
			@Override
			public void run() {			
				TestUtils.writePersonBatch(currSpace.getGigaSpace(), td.expectedTP);
				if (numOfLoops > 0){
					long startTime = System.currentTimeMillis();
		    		currSpace.getGigaSpace().readMultiple(null, td.expectedTP);
		    		long endTime = System.currentTimeMillis();
					System.out.println("readMultiple took "+(endTime-startTime)+" milliSeconds");	    		
		    		numOfLoops--;
		    		System.out.println("loop #"+(numberOfSecondsToRun-numOfLoops)+", readCount: "+currSpace.getStatistics().getReadCount() + "/ readPerSec: "+currSpace.getStatistics().getReadPerSecond());
		    		td.endOfActionsLatch.countDown();
		    	}else{	    		
		    		td.timer.cancel();
		    	}
		    }
	    },td,new StatMethod(){
			public double getStats(SpaceInstanceStatistics statistics) {
				return statistics.getReadPerSecond();
			}
		},"readPerSec");
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="2")
	public void testWritePerSec() throws InterruptedException{
		readWriteTakeTestBody(new TimerTask(){
	    	int numOfLoops = numberOfSecondsToRun;
			@Override
			public void run() {			
				if (numOfLoops > 0){
					long startTime = System.currentTimeMillis();
					TestUtils.writePersonBatch(currSpace.getGigaSpace(), td.expectedTP);
		    		long endTime = System.currentTimeMillis();
					System.out.println("writeMultiple took "+(endTime-startTime)+" milliSeconds");	    		
		    		numOfLoops--;
		    		System.out.println("loop #"+(numberOfSecondsToRun-numOfLoops)+", writeCount: "+currSpace.getStatistics().getWriteCount() + "/ WritePerSec: "+currSpace.getStatistics().getWritePerSecond());
		    		td.endOfActionsLatch.countDown();
		    	}else{	    		
		    		td.timer.cancel();
		    	}
		    }
	    },td,new StatMethod(){
			public double getStats(SpaceInstanceStatistics statistics) {
				return statistics.getWritePerSecond();
			}
		},"writePerSec");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="2")
	public void testTakePerSec() throws InterruptedException{
		readWriteTakeTestBody(new TimerTask(){
	    	int numOfLoops = numberOfSecondsToRun;
			@Override
			public void run() {			
				if (numOfLoops > 0){
					long startTime = System.currentTimeMillis();
					TestUtils.writePersonBatch(currSpace.getGigaSpace(), td.expectedTP);
					currSpace.getGigaSpace().takeMultiple(null, td.expectedTP);
					long endTime = System.currentTimeMillis();
					System.out.println("takeMultiple took "+(endTime-startTime)+" milliSeconds");	    		
		    		numOfLoops--;
		    		System.out.println("loop #"+(numberOfSecondsToRun-numOfLoops)+", takeCount: "+currSpace.getStatistics().getTakeCount() + "/ takePerSec: "+currSpace.getStatistics().getTakePerSecond());
		    		td.endOfActionsLatch.countDown();
		    	}else{	    		
		    		td.timer.cancel();
		    	}
		    }
			
	    },td,new StatMethod(){
			public double getStats(SpaceInstanceStatistics statistics) {
				return statistics.getTakePerSecond();
			}
		},"TakePerSec");
	}	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="2")
	public void testExecutePerSec() throws InterruptedException{
	    
	    final TestData testData = new TestData(1);
	    
		readWriteTakeTestBody(new TimerTask(){
	    	int numOfLoops = numberOfSecondsToRun;
			@Override
			public void run() {			
				if (numOfLoops > 0){
					Future<Integer> future = currSpace.getGigaSpace().execute(new MyStatisticsTask(numOfLoops),1);
					int result = 0;
					try {
					    result = future.get();
					} catch (ExecutionException e) {
					    AssertFail("Failed executing task");
					} catch (InterruptedException e) {
					    AssertFail("Failed executing task");
                    }
		    		numOfLoops--;
		    		System.out.println("loop #"+(numberOfSecondsToRun-numOfLoops)+", executeCount: "+currSpace.getStatistics().getExecuteCount() + "/ executePerSec: "+currSpace.getStatistics().getExecutePerSecond() + ", executre result:" + result);
		    		testData.endOfActionsLatch.countDown();
		    	}else{	    		
		    	    testData.timer.cancel();
		    	}
		    }
	    },testData,new StatMethod(){
			public double getStats(SpaceInstanceStatistics statistics) {
				return statistics.getExecutePerSecond();
			}
		},"ExecutePerSec");
	}
	
    static class MyStatisticsTask implements Task<Integer> {
        private static final long serialVersionUID = 162224308813832518L;
        private final int value;
        public MyStatisticsTask(int value) {
            this.value = value;
        }
        public Integer execute() throws Exception {
            return value;
        }
    }
}
