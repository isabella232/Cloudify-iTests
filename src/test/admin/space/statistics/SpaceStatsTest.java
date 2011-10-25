package test.admin.space.statistics;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceStatistics;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.Task;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.data.Person;
import test.utils.TestUtils;

public class SpaceStatsTest extends AbstractTest {

	private GridServiceManager gsm;
	private ProcessingUnit pu;

	@BeforeMethod
	public void setup() {
		this.gsm = TestUtils.waitAndLoad(admin,1,2);
		admin.setStatisticsInterval(1, TimeUnit.SECONDS);
		admin.startStatisticsMonitor();
		pu = TestUtils.deploySpace(gsm, "A",2,1);
	}	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="2")
	public void testReadCount() throws InterruptedException {
		GigaSpace gigaSpace = pu.getSpace().getGigaSpace();
		TestUtils.writePersonBatch(gigaSpace, 50);
		for (int i = 1; i < 15; i++) {
			gigaSpace.read(new Person());
			Thread.sleep(2000);
			SpaceStatistics stats =pu.getSpace().getStatistics();
			System.out.println(stats.getReadCount());
			int readCount = Long.valueOf(stats.getReadCount()).intValue();
			System.out.println("readCount: "+readCount);
			assertTrue("incorrect readCount statistics, should be "+i*2+" while it is "+readCount,readCount==i*2);
			
			SpaceStatistics primaryStats = pu.getSpace().getPrimariesStatistics();
			org.testng.AssertJUnit.assertEquals("incorrect readCount for primaries", i*2, primaryStats.getReadCount());

			SpaceStatistics backupStats = pu.getSpace().getBackupsStatistics();
			org.testng.AssertJUnit.assertEquals("incorrect readCount for backups", 0, backupStats.getReadCount());
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="2")
	public void testWriteCount() throws InterruptedException {
		GigaSpace gigaSpace = pu.getSpace().getGigaSpace();
		for (int i = 1; i < 15; i++) {
			Person p=new Person();
			p.setId(Integer.valueOf(i).longValue());
			gigaSpace.write(p);
			Thread.sleep(2000);
			SpaceStatistics stats =pu.getSpace().getStatistics();
			int writeCount = Long.valueOf(stats.getWriteCount()).intValue();
			System.out.println("WriteCount: "+writeCount);
			assertTrue("incorrect WriteCount statistics, should be "+i*2+" while it is "+writeCount,writeCount==i*2);
			
			SpaceStatistics primaryStats = pu.getSpace().getPrimariesStatistics();
			org.testng.AssertJUnit.assertEquals("incorrect writeCount for primaries", i, primaryStats.getWriteCount());

			SpaceStatistics backupStats = pu.getSpace().getBackupsStatistics();
			org.testng.AssertJUnit.assertEquals("incorrect writeCount for backups", i, backupStats.getWriteCount());
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="2")
	public void testTakeCount() throws InterruptedException {
		GigaSpace gigaSpace = pu.getSpace().getGigaSpace();
		TestUtils.writePersonBatch(gigaSpace, 15);
		for (int i = 1; i < 15; i++) {
			gigaSpace.take(new Person());
			Thread.sleep(2000);
			SpaceStatistics stats =pu.getSpace().getStatistics();
			int takeCount = Long.valueOf(stats.getTakeCount()).intValue();
			System.out.println("TakeCount: "+takeCount);
			assertTrue("incorrect TakeCount statistics, should be "+i*2+" while it is "+takeCount,takeCount==i*2);
			
			SpaceStatistics primaryStats = pu.getSpace().getPrimariesStatistics();
			org.testng.AssertJUnit.assertEquals("incorrect takeCount for primaries", i, primaryStats.getTakeCount());

			SpaceStatistics backupStats = pu.getSpace().getBackupsStatistics();
			org.testng.AssertJUnit.assertEquals("incorrect takeCount for backups", i, backupStats.getTakeCount());
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="2")
	public void testExecuteCount() throws InterruptedException {
		GigaSpace gigaSpace = pu.getSpace().getGigaSpace();
		for (int i = 1; i < 15; i++) {
			Future<Integer> future = gigaSpace.execute(new MyStatisticsTask(i), (i%2) + 1);
			int result = 0;
			try {
			    result = future.get();
            } catch (ExecutionException e) {
                AssertFail("Failed executing task");
            }
			Thread.sleep(2000);
			SpaceStatistics stats =pu.getSpace().getStatistics();
			int executeCount = Math.round(stats.getExecuteCount());
			System.out.println("executeCount: "+executeCount+", result:" + result);
			assertTrue("incorrect executeCount statistics, should be "+i+" while it is "+executeCount,executeCount==i);
			
			SpaceStatistics primaryStats = pu.getSpace().getPrimariesStatistics();
			org.testng.AssertJUnit.assertEquals("incorrect executeCount for primaries", i, primaryStats.getExecuteCount());

			SpaceStatistics backupStats = pu.getSpace().getBackupsStatistics();
			org.testng.AssertJUnit.assertEquals("incorrect executeCount for backups", 0, backupStats.getExecuteCount());
		}
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
