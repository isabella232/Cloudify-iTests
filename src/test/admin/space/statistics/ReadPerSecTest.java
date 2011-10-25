package test.admin.space.statistics;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.admin.space.SpaceInstanceStatistics;
import org.testng.annotations.BeforeMethod;

import test.AbstractTest;

/**
 * 1.deploying 1 space instance
 * 2.using a timer to execute ReadTask which readsMultiple(numberOfReads) from the space every second.
 * 3.after 10 seconds, comparing ReadPerSec statistics of the space to 10*numberOfReads.
 * @author Rafi Pinto
 */
public class ReadPerSecTest extends AbstractTest{
	
	private final int numberOfReads = 11; //expected readPerSec
	private final int numberOfSecondsToRun = 20; //how many times the task is performed (1 sec intervals)
	
	private Machine machine;
	private GridServiceManager gsm;
	private Timer timer;
	private CountDownLatch endOfReadsLatch;
	private Space currSpace;
	
	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		gsm = loadGSM(machine);
		loadGSC(machine);
	}
	
	//@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {
		assertTrue(admin.getMachines().waitFor(1));
		assertTrue(admin.getGridServiceAgents().waitFor(1));
		admin.setStatisticsInterval(1, TimeUnit.SECONDS);
		admin.startStatisticsMonitor();
		endOfReadsLatch=new CountDownLatch(numberOfSecondsToRun-2);
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("A"));
		pu.waitForSpace();		
		for (Space space : admin.getSpaces()) {
			currSpace = space;
			for (int i = 0; i < numberOfReads; i++) {
				currSpace.getGigaSpace().write(new Object());
			}
			
			for (SpaceInstance spaceInstance : space) {
				timer = new Timer();
			    timer.schedule(new ReadTask(), 0, 1000);
				endOfReadsLatch.await(60, TimeUnit.SECONDS);
				SpaceInstanceStatistics statistics = spaceInstance.getStatistics();
				System.out.println("Current Stats: " + statistics.getReadCount() + "/" + statistics.getReadPerSecond());
				System.out.println("Previous Stats: " + statistics.getPrevious().getReadCount() + "/" + statistics.getPrevious().getReadPerSecond());
				
				int readStats=(int)Math.ceil(statistics.getReadPerSecond());
				timer.cancel();
				assertTrue("failed to receive correct read statistics, readPerSec should be "+numberOfReads+" while its "+readStats,readStats==numberOfReads);				
				break;
			}
			break;
		}
		
	}
	
	class ReadTask extends TimerTask {
	   int numOfLoops = numberOfSecondsToRun;
		@Override
		public void run() {			
			if (numOfLoops > 0){
				long startTime = System.currentTimeMillis();
	    		currSpace.getGigaSpace().readMultiple(null, numberOfReads);
	    		long endTime = System.currentTimeMillis();
				System.out.println("readMultiple took "+(endTime-startTime)+" milliSeconds");	    		
	    		numOfLoops--;
	    		System.out.println("loop #"+(numberOfSecondsToRun-numOfLoops)+", readCount: "+currSpace.getStatistics().getReadCount() + "/ readPerSec: "+currSpace.getStatistics().getReadPerSecond());
	    		endOfReadsLatch.countDown();
	    	}else{	    		
	    		timer.cancel();
	    	}
			
	    }
	  }
}
