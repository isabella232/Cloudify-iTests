package test.wan;

import net.jini.core.lease.Lease;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.transaction.manager.LocalJiniTxManagerConfigurer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.data.Data;
import test.utils.ThreadBarrier;

import com.j_spaces.core.client.ReadModifiers;
import com.j_spaces.core.client.UpdateModifiers;

/**
 * Test WAN write failure retry mechanism.
 * Flow:
 * 	1. Write an object to cluster #1.
 *  2. Lock the object in cluster #2.
 *  3. Attempt to update the object through cluster #1 (infinite retry loop).
 *  4. Release the lock.
 *  5. Verify updated object in cluster #2.
 * 
 * @author idan
 * @since 8.0.1
 *
 */
public class WanWriteFailureTest extends AbstractWanTest {

	private static final long WAIT = 10000;
	
	@Override
	@AfterMethod
	public void afterTest() {
		super.afterTest();
	}

	@Override
	@BeforeMethod
	public void beforeTest() {
		super.beforeTest();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {
		try {
			
			Data data = new Data();
			data.setData("A");
			data.setId("1000");
			data.setType(0);
			
			gigaSpace1.write(data);
			Thread.sleep(WAIT);
			
			final ThreadBarrier barrier = new ThreadBarrier(2);
			final Thread locker = new Thread(new Runnable() {
				public void run() {
					try {
						PlatformTransactionManager ptm = new LocalJiniTxManagerConfigurer(gigaSpace2.getSpace()).transactionManager();
						GigaSpace txnGigaSpace = new GigaSpaceConfigurer(gigaSpace2.getSpace()).transactionManager(ptm).gigaSpace();
						DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
						TransactionStatus status = ptm.getTransaction(definition);
						
						txnGigaSpace.readById(Data.class, "1000", null, 0, ReadModifiers.EXCLUSIVE_READ_LOCK);
						
						barrier.await();

						Thread.sleep(WAIT);
						
						// release lock
						ptm.rollback(status);
						
					} catch (Exception e) {
						AssertFail(e.getMessage(), e);
					}
				}
			});
						
			locker.start();
			
			// wait for lock
			barrier.await();
			
			// attempt to update
			data.setData("B");
			gigaSpace1.write(data, Lease.FOREVER, WAIT * 2, UpdateModifiers.UPDATE_ONLY);
			
			Thread.sleep(WAIT * 2);
			
			Data d = gigaSpace2.readById(Data.class, data.getId(), data.getType(), WAIT);
			assertNotNull(d);
			assertEquals(data.getData(), d.getData());
			
		} catch(Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
			throw e;
		}

	}

}
