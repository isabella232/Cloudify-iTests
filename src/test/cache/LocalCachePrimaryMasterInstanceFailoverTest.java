package test.cache;



import org.junit.Assert;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.space.SpaceStatistics;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.cache.LocalCacheSpaceConfigurer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AdminUtils;
import test.utils.ThreadBarrier;

import com.gatewayPUs.common.Stock;
import com.j_spaces.core.IJSpace;

/**
 * 
 * @author gal
 *	In the beforeMethod: start a master space and make an initial write on the master and mark part of the objects as "cached".
 *	Then start a client space with local cache and make an initial read of all the "cached" objects. 
 *
 *	In the test: start reading and updating the cache with multiple threads and during this work destroy the primary 
 *	instance of the master space, all the while asserting the updates and reads don't return null.
 *	After the threads stop their work wait for the master space to recover and assert his update counter is consistent
 *	with the number of updates and that the read counter is 0 (reads are made to the cache only)
 *	
 */
public class LocalCachePrimaryMasterInstanceFailoverTest extends AbstractTest {

	private GigaSpace clientGigaSpace;
	private GigaSpace serverGigaSpace;
	private int numOfThreads = 5;
	private ThreadBarrier barrier;
	final private int initialWriteAmount = 10000;
	final private int cachedObjects = initialWriteAmount/5;
	final private String masterSpaceName = "masterSpace";
	final private String cachedObject = "cached";
	final private String unCachedObject = "unCached";
	
	@Override
	@BeforeMethod
	public void beforeTest(){
		super.beforeTest();
		AdminUtils.loadGSM(admin.getGridServiceAgents().waitForAtLeastOne());
		AdminUtils.loadGSC(admin.getGridServiceAgents().waitForAtLeastOne());
		AdminUtils.loadGSC(admin.getGridServiceAgents().waitForAtLeastOne());
		
		admin.getGridServiceManagers().deploy(new SpaceDeployment(masterSpaceName)
											  .numberOfBackups(1)
											  .numberOfInstances(1));
		Space masterSpace = admin.getSpaces().waitFor(masterSpaceName);
		masterSpace.waitFor(masterSpace.getTotalNumberOfInstances());
		
		serverGigaSpace = new GigaSpaceConfigurer(masterSpace.getGigaSpace().getSpace()).clustered(true).gigaSpace();
		initialWriteToMaster();
		IJSpace localCacheProxy = new LocalCacheSpaceConfigurer(masterSpace.getGigaSpace().getSpace()).localCache();
		clientGigaSpace = new GigaSpaceConfigurer(localCacheProxy).gigaSpace();	
		initialReadFromClient();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="1", enabled=true)
	public void test() throws Exception {
		barrier = new ThreadBarrier(numOfThreads + 1);		
		final ProcessingUnit masterSpacePu = admin.getProcessingUnits().waitFor(masterSpaceName);
		
		startWorkOnClient();
		barrier.await();
		
		masterSpacePu.waitFor(masterSpacePu.getTotalNumberOfInstances());
		SpaceStatistics statistics = masterSpacePu.getSpace().getPrimariesStatistics();
		long updateCount = statistics.getUpdateCount();
		long readCount = statistics.getReadCount();
		Assert.assertEquals("Not all the update operations where performed on the master space", 
				cachedObjects * numOfThreads , updateCount);
		Assert.assertTrue("Client read operations on cached data where made on space", readCount == 0);
	}

	private void startWorkOnClient() {		
		for (int i = 0; i < numOfThreads; ++i) 			
			new Thread(new ReadAndWriteToClient(i)).start();		       
    }
	
	private void initialWriteToMaster() {
		for(int i=0; i<initialWriteAmount ; i++){
			Stock stock = new Stock(i);
			if(i < cachedObjects)
				stock.setStockName(cachedObject);
			else
				stock.setStockName(unCachedObject);
			
			serverGigaSpace.write(stock);			
		}
		Assert.assertEquals("Initial write failed" ,
				serverGigaSpace.count(new Stock()) ,initialWriteAmount);
	}
	
	private void initialReadFromClient() {
		
		Stock template = new Stock();
		template.setStockName(cachedObject);
		Stock [] stocks = clientGigaSpace.readMultiple(template);
		Assert.assertEquals("Initial write failed" ,cachedObjects ,stocks.length);
	}
	
	private class ReadAndWriteToClient implements Runnable{
		
		int threadNum;
		
		public ReadAndWriteToClient(int threadNum){
			this.threadNum = threadNum;
		}
		
        public void run() {
            try {
            	final ProcessingUnit masterSpacePu = admin.getProcessingUnits().waitFor(masterSpaceName);

            	if(threadNum == numOfThreads/2){
            		Thread.sleep(1000 * numOfThreads/2);
        			masterSpacePu.getPartitions()[0].getPrimary().destroy();
            	}            	
            	for(int i=0 ; i<cachedObjects ; i++){
            		Stock stock = new Stock(i);
            		stock.setStockName(cachedObject);
            		Assert.assertNotNull("Attempt to read: " + stock + " Recived null !", 
            				clientGigaSpace.read(stock));
            		stock.setData(i);
            		Assert.assertNotNull("Attempt to update: " + stock + " Recived null !", 
            				clientGigaSpace.write(stock));
            	}
            	barrier.await();
            } catch (Throwable t) {
                barrier.reset(t);
            }
        }
    }
}