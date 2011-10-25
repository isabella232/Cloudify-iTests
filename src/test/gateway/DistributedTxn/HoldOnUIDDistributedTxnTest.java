package test.gateway.DistributedTxn;

import static test.utils.LogUtils.log;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.gatewayPUs.common.Stock;

import test.AbstractTest;
import test.gateway.AbstractGatewayTest;
import test.utils.AssertUtils;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;

/**
 * @author Sagi Bernstein
 * @since 8.0.4
 *
 *	WORKFLOW: this test writes an object with uid 0 and 1 to israel and delays 1 replication
 *	than writes the 0 uid again.
 *  it asserts uid 0 has arrived only when both timeouts are over and the version is 2
 *
 */
public class HoldOnUIDDistributedTxnTest extends AbstractGatewayTest {
	private Admin adminIsrael;
	private Admin adminLondon;
	private GigaSpace gigaSpaceIsrael;
	private GigaSpace gigaSpaceLondon;

	private PlatformTransactionManager ptmIsrael;
	private PlatformTransactionManager ptmLondon;
	private GigaSpace txnGigaSpaceIsrael;
	private GigaSpace txnGigaSpaceLondon;
	private final int ENTRY_NUM = 2; 			//should be even
	private final int PARTITION_NUM = 2;
	private final String FILTER_WAITING_TIME = "60000";
	private String TXN_TO = "5000";
	private String TXN_OPS_LIM = "2";


	private String groupIsrael = null;
	private String groupLondon = null;
	private String hostIsrael = null;
	private String hostLondon = null;
	private GridServiceManager gsmIsrael;
	private GridServiceManager gsmLondon;


	public HoldOnUIDDistributedTxnTest() {
		if (isDevMode()) {
			groupIsrael = "israel-" + getUserName();
			groupLondon = "london-" + getUserName();
			hostIsrael = "localhost";
			hostLondon = "localhost";
		} else {
			groupIsrael = GROUP1;
			groupLondon = GROUP2;
			hostIsrael = HOST1;
			hostLondon = HOST2;
		}
	}

	@AfterMethod
	public void tearDown() {
		TeardownUtils.teardownAll(adminIsrael, adminLondon);
		adminIsrael.close();
		adminLondon.close();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="2", enabled=true)
	public void test() throws Exception {
		initialize();

		log("starting case 1");
		case1();

		log("assert clean sites");
	}

	private void case1() throws Exception {
		log("write initial txn");
		writeDelayedTransaction();

		Thread.sleep(1000);
		
		log("write uid 0 again");
		writeNonDelayedTransaction(0);
		
		RepetitiveConditionProvider repCond = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				System.out.println("objects in london: "  + txnGigaSpaceLondon.count(new Object()) + " time: " + printCurrentTime());
				return txnGigaSpaceLondon.count(new Stock()) == 0;
			}
		};

		log("assert there no obejects in london for " + (Integer.valueOf(TXN_TO)/2));
		AssertUtils.repetitiveAssertConditionHolds("there are objects in the remote space", repCond, (Integer.valueOf(TXN_TO)/2), 200);


		repCond = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				System.out.println("objects in london: "  + txnGigaSpaceLondon.count(new Object()) + " time: " + printCurrentTime());
				Stock readStock = txnGigaSpaceLondon.readById(Stock.class, 0);
				return txnGigaSpaceLondon.count(new Stock()) == 1 && readStock.getVersion() == 2;
			}
		};
		log("assert there no obejects in london for " + (5000));
		repetitiveAssertTrue("object amount in the remote space is not 1", repCond, 5000);


		log("test completed");
	}


	private void writeDelayedTransaction() {
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		TransactionStatus status = ptmIsrael.getTransaction(definition);
		boolean rollback = false;

		try{
			for (int i = 0; i < 2; i++) {
				Stock stock = new Stock(i);
				stock.setData(i);
				txnGigaSpaceIsrael.write(stock);
			}
		} catch (TransactionException e) {
			ptmIsrael.rollback(status);
			rollback = true;
		}
		if(!rollback)
			ptmIsrael.commit(status);
	}

	private void writeNonDelayedTransaction(int id) {

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		TransactionStatus status = ptmIsrael.getTransaction(definition);

		boolean rollback = false;

		try{
			Stock stock = new Stock(id);
			stock.setData(0);
			txnGigaSpaceIsrael.write(stock);
		}
		catch (Exception e) {
			ptmIsrael.rollback(status);
			rollback = true;
		}
		if(!rollback)
			ptmIsrael.commit(status);
	}


	private void initialize() throws Exception {
		adminIsrael = new AdminFactory().addGroups(groupIsrael).createAdmin();
		adminLondon = new AdminFactory().addGroups(groupLondon).createAdmin();
		SetupUtils.assertCleanSetup(adminIsrael);
		SetupUtils.assertCleanSetup(adminLondon);

		GridServiceAgent gsaIsrael = adminIsrael.getGridServiceAgents().waitForAtLeastOne();
		GridServiceAgent gsaLondon = adminLondon.getGridServiceAgents().waitForAtLeastOne();
		AbstractTest.assertNotNull("Could not find agent on group: " + adminIsrael.getGroups()[0], gsaIsrael);
		AbstractTest.assertNotNull("Could not find agent on group: " + adminLondon.getGroups()[0], gsaLondon);

		gsaIsrael.startGridServiceAndWait(new GridServiceManagerOptions());
		gsaIsrael.startGridServiceAndWait(new GridServiceContainerOptions());
		gsaIsrael.startGridServiceAndWait(new GridServiceContainerOptions());

		gsaLondon.startGridServiceAndWait(new GridServiceManagerOptions());
		gsaLondon.startGridServiceAndWait(new GridServiceContainerOptions());
		gsaLondon.startGridServiceAndWait(new GridServiceContainerOptions());

		gsmIsrael = adminIsrael.getGridServiceManagers().waitForAtLeastOne();
		AbstractTest.assertNotNull(gsmIsrael);
		gsmLondon = adminLondon.getGridServiceManagers().waitForAtLeastOne();
		AbstractTest.assertNotNull(gsmLondon);

		log("deploying Israel site");
		deployIsraelSite();
		log("deploying London site");
		deployLondonSite();
		log("deploying London Txn conflict handler");
		deployLondonTxn();
		log("deploying Israel GW");
		deployIsraelGW();


		assertTrue(adminIsrael.getGridServiceContainers().waitFor(3));
		assertTrue(adminLondon.getGridServiceContainers().waitFor(3));

		Space spaceIsrael = adminIsrael.getSpaces().waitFor("israelSpace");
		Space spaceLondon = adminLondon.getSpaces().waitFor("londonSpace");

		ptmIsrael = new DistributedJiniTxManagerConfigurer().transactionManager();
		ptmLondon = new DistributedJiniTxManagerConfigurer().transactionManager();


		gigaSpaceIsrael = spaceIsrael.getGigaSpace();
		gigaSpaceLondon = spaceLondon.getGigaSpace();


		txnGigaSpaceIsrael = new GigaSpaceConfigurer(gigaSpaceIsrael.getSpace())
		.transactionManager(ptmIsrael).clustered(true).gigaSpace();
		txnGigaSpaceLondon = new GigaSpaceConfigurer(gigaSpaceLondon.getSpace())
		.transactionManager(ptmLondon).clustered(true).gigaSpace();


		// Verify gateway connections.
		assertGatewayReplicationConnected(spaceIsrael, 1);




	}

	private void deployLondonSite() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("spaceUrl", "/./londonSpace");
		deploySite(gsmLondon, gatewayDeployment("./apps/gateway/clusterWithoutTargetsMultiSource", "londonSpace", props));		
	}

	private void deployIsraelGW() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "ISRAEL");
		props.put("targetGatewayName", "LONDON");
		props.put("localClusterUrl", "jini://*/*/israelSpace");
		props.put("gatewayLookupGroup", UUID.randomUUID().toString());
		props.put("localGatewayHost", hostIsrael);
		props.put("localGatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);
		props.put("localGatewayLrmiPort", ISRAEL_GATEWAY_LRMI_PORT);
		props.put("targetGatewayHost", hostLondon);
		props.put("targetGatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
		props.put("targetGatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
		deployGateway(gsmIsrael, gatewayDeployment("./apps/gateway/gatewayDelegatorOnly", "ISRAEL-GW", props));

	}

	private void deployLondonTxn() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "LONDON");
		props.put("targetGatewayName", "ISRAEL");
		props.put("localClusterUrl", "jini://*/*/londonSpace");
		props.put("gatewayLookupGroup", UUID.randomUUID().toString());
		props.put("localGatewayHost", hostLondon);
		props.put("localGatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
		props.put("localGatewayCommunicationPort", LONDON_GATEWAY_LRMI_PORT);
		props.put("targetGatewayHost", hostIsrael);
		props.put("targetGatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);
		props.put("targetGatewayCommunicationPort", ISRAEL_GATEWAY_LRMI_PORT);
		props.put("conflictResolverClassName", "com.gigaspaces.gateway.TxnConflictResolver");
		props.put("maxRetriesOnTxLock", "3");
		props.put("txLockRetryIterval", "3");
		props.put("dist-tx-wait-timeout-millis", TXN_TO);
		props.put("dist-tx-wait-for-opers", TXN_OPS_LIM);
		props.put("numOfEntries", String.valueOf(ENTRY_NUM));
		deploySite(gsmLondon, sitePrepareAndDeployment("gateway-txn-conflict-handler", props));

	}

	private void deployIsraelSite() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "ISRAEL");
		props.put("gatewayTarget", "LONDON");
		props.put("spaceUrl", "/./israelSpace");
		props.put("timeout", FILTER_WAITING_TIME);
		deploySite(gsmIsrael, sitePrepareAndDeployment("multisource-filter", props).partitioned(PARTITION_NUM,1));

	}


}
