package test.gateway;

import com.gatewayPUs.common.Stock;
import com.j_spaces.core.client.ReadModifiers;
import com.j_spaces.core.client.SQLQuery;
import com.j_spaces.core.client.UpdateModifiers;
import net.jini.core.lease.Lease;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.transaction.manager.DistributedJiniTxManagerConfigurer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import test.utils.LogUtils;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;
import test.utils.TestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 
 * @author idan
 * @since 8.0.3
 *
 */
public class ConflictResolutionTest extends AbstractGatewayTest {

    private String group1;
	private String group2;
	private String host1;
	private String host2;
	private Admin admin1;
	private Admin admin2;
	private GigaSpace gigaSpace1;
	private GigaSpace gigaSpace2;

	public ConflictResolutionTest() {
		if (isDevMode()) {
			group1 = "israel-" + getUserName();
			group2 = "london-" + getUserName();
			host1 = "localhost";
			host2 = "localhost";
		} else {
			group1 = GROUP1;
			group2 = GROUP2;
			host1 = HOST1;
			host2 = HOST2;
		}
	}
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2", enabled=true)
    public void testConflictResolution() throws Exception {
        initialize();

        LogUtils.log("testing: partial update when object does not exist");
        testPartialUpdateWhenObjectDoesntExist();

        LogUtils.log("testing: partial update when object exist");
        testPartialUpdateWhenObjectExist();

        LogUtils.log("testing: update when object does not exist");
        testUpdateWhenObjectDoesntExist();

        LogUtils.log("testing: write when entry already exists");
        testWriteWhenEntryAlreadyExists();

        LogUtils.log("testing: update version conflict");
        testUpdateVersionConflict();

        LogUtils.log("testing: entry is locked under transaction");
        testEntryLockedUnderTransaction();

        LogUtils.log("testing: override entry data on conflict");
        testOverrideEntryDataOnConflict();

        LogUtils.log("testing: update version override on conflict");
        testVersionOverrideOnConflict();

        
//        LogUtils.log("testing: number of resolve attempts");
//        testNumberOfResolveAttempts();

    }

    private void testVersionOverrideOnConflict() {
    	Stock stock = new Stock(300);
    	gigaSpace2.write(stock);
    	stock = gigaSpace2.readById(Stock.class, stock.getStockId());
    	gigaSpace2.write(stock);
    	gigaSpace2.write(stock);
    	Assert.assertEquals(3, stock.getVersion());
    	
    	final Stock s = new Stock(300);
    	s.setStockName("AAAA");
    	s.setVersion(1);
    	gigaSpace1.write(s);
    	final GigaSpace g2 = gigaSpace2;
    	TestUtils.repetitive(new Runnable() {
			public void run() {
				SQLQuery<Stock> query = new SQLQuery<Stock>(Stock.class, "stockId = 300 AND stockName = 'AAAA'");
				Stock result = g2.read(query);
				Assert.assertNotNull(result);
				Assert.assertEquals(s.getVersion(), result.getVersion());
			}
    	}, (int)DEFAULT_TEST_TIMEOUT);
    	
    	final Stock stock2 = new Stock(400);
    	gigaSpace1.write(stock2);
    	
    	Stock stock3 = gigaSpace2.readById(Stock.class, 400, null, (int)DEFAULT_TEST_TIMEOUT, ReadModifiers.REPEATABLE_READ);
    	gigaSpace2.write(stock3);
    	gigaSpace2.write(stock3);
    	gigaSpace2.write(stock3);
    	
    	stock2.setStockName("BBBB");
    	gigaSpace1.write(stock2);
    	
    	TestUtils.repetitive(new Runnable() {
			public void run() {
				SQLQuery<Stock> query = new SQLQuery<Stock>(Stock.class, "stockId = 400 AND stockName = 'BBBB'");
				Stock result = g2.read(query);
				Assert.assertNotNull(result);
				Assert.assertEquals(stock2.getVersion(), result.getVersion());
			}
    	}, (int)DEFAULT_TEST_TIMEOUT);
    	
    	final Stock stock4 = new Stock(500);
    	gigaSpace1.write(stock4);
    	gigaSpace1.write(stock4);
    	gigaSpace1.write(stock4);
    	
    	Object takeResult = gigaSpace2.takeById(Stock.class, 500, null, (int)DEFAULT_TEST_TIMEOUT, ReadModifiers.REPEATABLE_READ);
    	Assert.assertNotNull(takeResult);
    	
    	stock4.setStockName("CCCC");
    	gigaSpace1.write(stock4);
    	
    	TestUtils.repetitive(new Runnable() {
			public void run() {
				SQLQuery<Stock> query = new SQLQuery<Stock>(Stock.class, "stockId = 500 AND stockName = 'CCCC'");
				Stock result = g2.read(query);
				Assert.assertNotNull(result);
				Assert.assertEquals(stock4.getVersion(), result.getVersion());
			}
    	}, (int)DEFAULT_TEST_TIMEOUT);
    	
	}

//	private void testNumberOfResolveAttempts() {
//		TestEntry testEntry = new TestEntry(0, null);
//		gigaSpace2.write(testEntry);
//		Person person1 = new Person(200L, "ABCD");
//		gigaSpace1.write(person1);
//		Person person2 = gigaSpace2.take(person1, SINGLE_OPERATION_TIMEOUT);
//		Assert.assertNotNull(person2);
//		person1.setName("AAAA");
//		gigaSpace1.write(person1);
//		TestUtils.repetitive(new Runnable() {
//			public void run() {
//				TestEntry entry = gigaSpace2.readById(TestEntry.class, 0);
//				Assert.assertNotNull(entry);
//				Assert.assertNotNull(entry.getValue());
//			}
//		}, SINGLE_OPERATION_TIMEOUT);
//	}

    private void testPartialUpdateWhenObjectDoesntExist() {
        // test abort
        Stock stock = new Stock(1000);
        stock.setStockName("KOBI KI");
        gigaSpace1.write(stock);
        Stock result = gigaSpace2.takeById(Stock.class, stock.getStockId(), null, (int)DEFAULT_TEST_TIMEOUT);
        Assert.assertNotNull(result);
        stock.setStockName("Mr. KOBI KI");
        gigaSpace1.write(stock, Lease.FOREVER, 0, UpdateModifiers.PARTIAL_UPDATE);
        result = gigaSpace2.readById(Stock.class, stock.getStockId(), null, NEEDED_SHORT_TIMEOUT);
        Assert.assertNull(result);
        // test override
        stock = new Stock(2000);
        stock.setStockName("RONNI");
        stock.setData(1);
        gigaSpace1.write(stock);
        result = gigaSpace2.takeById(Stock.class, stock.getStockId(), null, (int)DEFAULT_TEST_TIMEOUT);
        Assert.assertNotNull(result);
        stock.setStockName("Miss RONNI");
        gigaSpace1.write(stock, Lease.FOREVER, 0, UpdateModifiers.PARTIAL_UPDATE);
        result = gigaSpace2.readById(Stock.class, stock.getStockId(), null, (int)DEFAULT_TEST_TIMEOUT);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStockName(), "Miss RONNI");
        Assert.assertEquals(result.getData().intValue(), 1);
	}

    private void testPartialUpdateWhenObjectExist() {
        // test override
        Stock stock = new Stock(3000);
        stock.setStockName("RONNI");
        stock.setData(1);
        gigaSpace1.write(stock);
        Stock readStock = gigaSpace2.readById(Stock.class, stock.getStockId(), null, (int)DEFAULT_TEST_TIMEOUT);
        readStock.setData(2);
        gigaSpace2.write(readStock);
        gigaSpace2.write(readStock);
        Stock stock2 = new Stock(3000);
        stock2.setStockName("Miss RONNI");
        gigaSpace1.write(stock2, Lease.FOREVER, 0, UpdateModifiers.PARTIAL_UPDATE);
        Stock template = new Stock(3000);
        template.setStockName("Miss RONNI");
        Stock result = gigaSpace2.read(template, (int)DEFAULT_TEST_TIMEOUT);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStockName(), "Miss RONNI");
        Assert.assertEquals(result.getData().intValue(), 2);
	}


	private void testOverrideEntryDataOnConflict() {
		Stock stock1 = new Stock(100);
        stock1.setStockName("AAAA");
		gigaSpace1.write(stock1);
		Stock stock2 = gigaSpace2.take(stock1, (int)DEFAULT_TEST_TIMEOUT);
		Assert.assertNotNull(stock2);
		stock1.setStockName("BBBB");
		gigaSpace1.write(stock1);
		TestUtils.repetitive(new Runnable() {
			public void run() {
				SQLQuery<Stock> query = new SQLQuery<Stock>(Stock.class, "stockId = 100");
				Stock result = gigaSpace2.read(query);				
				Assert.assertNotNull(result);
				// Conflict resolver changes stock name to "CCCC"
				Assert.assertEquals("CCCC", result.getStockName());
				
			}
		}, (int)DEFAULT_TEST_TIMEOUT);
	}

	private void testEntryLockedUnderTransaction() throws Exception {
		// abort
		Stock person1 = new Stock(7);
        person1.setStockName("AAAA");
		gigaSpace1.write(person1);
		Stock person2 = gigaSpace2.read(person1, (int)DEFAULT_TEST_TIMEOUT);
		Assert.assertNotNull(person2);
		PlatformTransactionManager ptm = new DistributedJiniTxManagerConfigurer().defaultTimeout(60000).transactionManager();
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		GigaSpace txn = new GigaSpaceConfigurer(gigaSpace2.getSpace()).transactionManager(ptm).clustered(true).gigaSpace();
		TransactionStatus status = ptm.getTransaction(definition);
		txn.read(person2, 0, ReadModifiers.EXCLUSIVE_READ_LOCK);
		
		person1.setStockName("BBBB");
		gigaSpace1.write(person1);
		
		Thread.sleep(5000);
		
		ptm.commit(status);
		
		person2 = gigaSpace2.read(person1, NEEDED_SHORT_TIMEOUT);
		Assert.assertNull(person2);
		
		// override
		// irrelevant...
	}

	private void testUpdateVersionConflict() {
		// abort
		Stock person1 = new Stock(5);
        person1.setStockName("1111");
		gigaSpace1.write(person1);
		Stock person2 = gigaSpace2.readById(Stock.class, person1.getStockId(), null, (int)DEFAULT_TEST_TIMEOUT);
		Assert.assertNotNull(person2);
		person2.setStockName("2222");
		gigaSpace2.write(person2);
		person2.setStockName("3333");
		gigaSpace2.write(person2);
		person1.setStockName("0000");
		gigaSpace1.write(person1);
		person2 = gigaSpace2.read(person1, NEEDED_SHORT_TIMEOUT);
		Assert.assertNull(person2);
		
		// override
		person1 = new Stock(6);
        person1.setStockName("1111");
		gigaSpace1.write(person1);
		person2 = gigaSpace2.readById(Stock.class, person1.getStockId(), null, (int)DEFAULT_TEST_TIMEOUT);
		Assert.assertNotNull(person2);
		person2.setStockName("2222");
		gigaSpace2.write(person2);
		person2.setStockName("3333");
		gigaSpace2.write(person2);
		person1.setStockName("0000");
		gigaSpace1.write(person1);
		person2 = gigaSpace2.read(person1, (int)DEFAULT_TEST_TIMEOUT);
		Assert.assertNotNull(person2);
	}

	private void testUpdateWhenObjectDoesntExist() {
        // test abort
        Stock person = new Stock(1);
        person.setStockName("John Doe");
        gigaSpace1.write(person);
        Stock result = gigaSpace2.takeById(Stock.class, person.getStockId(), null, (int)DEFAULT_TEST_TIMEOUT);
        Assert.assertNotNull(result);
        person.setStockName("Mr. John Doe");
        gigaSpace1.write(person);
        result = gigaSpace2.readById(Stock.class, person.getStockId(), null, NEEDED_SHORT_TIMEOUT);
        Assert.assertNull(result);
        // test override
        person = new Stock(2);
        person.setStockName("ABCD");
        gigaSpace1.write(person);
        result = gigaSpace2.takeById(Stock.class, person.getStockId(), null, (int)DEFAULT_TEST_TIMEOUT);
        Assert.assertNotNull(result);
        person.setStockName("EFGH");
        gigaSpace1.write(person);
        result = gigaSpace2.readById(Stock.class, person.getStockId(), null, (int)DEFAULT_TEST_TIMEOUT);
        Assert.assertNotNull(result);
	}

	private void testWriteWhenEntryAlreadyExists() {
		// abort
        final Stock person1 = new Stock(3);
        person1.setStockName("1212");
        gigaSpace2.write(person1);
        person1.setStockName("1111");
        gigaSpace1.write(person1);
        TestUtils.repetitive(new Runnable() {
			public void run() {
				Stock result = gigaSpace2.readById(Stock.class, person1.getStockId());
				Assert.assertNotNull(result);
				Assert.assertEquals("1212", result.getStockName());
			}
        }, (int)DEFAULT_TEST_TIMEOUT);
        // override
        final Stock person2 = new Stock(4);
        person2.setStockName("1212");
        gigaSpace2.write(person2);
        person2.setStockName("1111");
        gigaSpace1.write(person2);
        TestUtils.repetitive(new Runnable() {
			public void run() {
				Stock result = gigaSpace2.readById(Stock.class, person2.getStockId());
				Assert.assertNotNull(result);
				Assert.assertEquals("1111", result.getStockName());
			}
        }, (int)DEFAULT_TEST_TIMEOUT);
		
	}

	private void initialize() {
        admin1 = new AdminFactory().addGroups(group1).createAdmin();
        admin2 = new AdminFactory().addGroups(group2).createAdmin();
        SetupUtils.assertCleanSetup(admin1);
        SetupUtils.assertCleanSetup(admin2);

        GridServiceAgent gsa1 = admin1.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent gsa2 = admin2.getGridServiceAgents().waitForAtLeastOne();

        gsa1.startGridServiceAndWait(new GridServiceManagerOptions());
        gsa1.startGridServiceAndWait(new GridServiceContainerOptions());
        gsa1.startGridServiceAndWait(new GridServiceContainerOptions());

        gsa2.startGridServiceAndWait(new GridServiceManagerOptions());
        gsa2.startGridServiceAndWait(new GridServiceContainerOptions());
        gsa2.startGridServiceAndWait(new GridServiceContainerOptions());

        final GridServiceManager gsm1 = admin1.getGridServiceManagers().waitForAtLeastOne();
        final GridServiceManager gsm2 = admin2.getGridServiceManagers().waitForAtLeastOne();

        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("gatewayTarget", "LONDON");
        props.put("spaceUrl", "/./israelSpace");
        deploySite(gsm1, siteDeployment("./apps/gateway/cluster", "israelSpace", props));
        props.clear();

        props.put("spaceUrl", "/./londonSpace");
        deploySite(gsm2, siteDeployment("./apps/gateway/clusterWithoutTargets", "londonSpace", props));
        props.clear();

        props.put("localGatewayName", "ISRAEL");
        props.put("targetGatewayName", "LONDON");
        props.put("localClusterUrl", "jini://*/*/israelSpace?groups=" + group1);
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", host1);
        props.put("localGatewayDiscoveryPort", "10001");
        props.put("localGatewayLrmiPort", "7000");
        props.put("targetGatewayHost", host2);
        props.put("targetGatewayDiscoveryPort", "10002");
        props.put("targetGatewayLrmiPort", "7001");
        deployGateway(gsm1, gatewayDeployment("./apps/gateway/gateway-components", "ISRAEL", props));
        props.clear();

        props.put("localGatewayName", "LONDON");
        props.put("targetGatewayName", "ISRAEL");
        props.put("localClusterUrl", "jini://*/*/londonSpace?groups=" + group2);
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", host2);
        props.put("localGatewayDiscoveryPort", "10002");
        props.put("localGatewayCommunicationPort", "7001");
        props.put("targetGatewayHost", host1);
        props.put("targetGatewayDiscoveryPort", "10001");
        props.put("targetGatewayCommunicationPort", "7000");
        props.put("conflictResolverClassName", "com.gigaspaces.gateway.MyConflictResolver");
        props.put("maxRetriesOnTxLock", "3");
        props.put("txLockRetryIterval", "3");
        deploySite(gsm2, sitePrepareAndDeployment("gateway-conflict-resolver", props));
        props.clear();

        assertTrue(admin1.getGridServiceContainers().waitFor(3));
        assertTrue(admin2.getGridServiceContainers().waitFor(3));

        Space space1 = admin1.getSpaces().waitFor("israelSpace");
        Space space2 = admin2.getSpaces().waitFor("londonSpace");

        gigaSpace1 = space1.getGigaSpace();
        gigaSpace2 = space2.getGigaSpace();

        // Verify gateway connections.
        assertGatewayReplicationConnected(space1, 1);
	}
	
    @AfterMethod
    public void tearDown() {
        TeardownUtils.teardownAll(admin1, admin2);
        admin1.close();
        admin2.close();
    }
    
}
