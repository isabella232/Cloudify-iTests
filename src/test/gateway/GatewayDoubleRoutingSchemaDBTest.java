package test.gateway;

import com.gigaspaces.cluster.activeelection.SpaceMode;
import com.j_spaces.core.client.Modifiers;
import net.jini.core.lease.Lease;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.ReplicationStatus;
import org.openspaces.admin.space.ReplicationTarget;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpacePartition;
import org.openspaces.core.GigaSpace;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import test.data.Stock;
import test.utils.*;
import test.utils.AssertUtils.RepetitiveConditionProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static test.utils.LogUtils.log;

/**
 * @author sagib
 * @since 8.0.3
 */
public class GatewayDoubleRoutingSchemaDBTest extends AbstractGatewayTest {

	private Admin adminIsrael;
	private Admin adminLondon;
	private Admin adminNY;
	private GigaSpace gigaSpaceNY;
	private GigaSpace gigaSpaceIsrael;
	private GigaSpace gigaSpaceLondon;

	private String group1 = null;
	private String group2 = null;
	private String group3 = null;
	private int dbPortISR;
	private int hsqlId1;
	private int dbPortNY;
	private int hsqlId2;

	private GridServiceManager gsmNY;
	private GridServiceManager gsmISR;
	private GridServiceManager gsmLON;

	public GatewayDoubleRoutingSchemaDBTest() {
		if (isDevMode()) {
			group1 = "israel-" + getUserName();
			group2 = "london-" + getUserName();
			group3 = "ny-" + getUserName();
		} else {
			group1 = GROUP1;
			group2 = GROUP2;
			group3 = GROUP3;
		}
		dbPortISR = 5000 + (int) (Math.random() * 5000);
		while (dbPortISR == 7000 || dbPortISR == 7001 || dbPortISR == 7002
				|| dbPortISR == 7003 || dbPortISR == 7004)
			dbPortISR = 5000 + (int) (Math.random() * 5000);
		dbPortNY = 5000 + (int) (Math.random() * 5000);
		while (dbPortNY == 7000 || dbPortNY == 7001 || dbPortNY == 7002
				|| dbPortNY == 7003 || dbPortNY == 7004)
			dbPortNY = 5000 + (int) (Math.random() * 5000);
	}

	@AfterMethod
	public void tearDown() {
		adminNY.getMachines().getMachines()[0].getGridServiceAgents()
				.getAgents()[0].killByAgentId(hsqlId1);
		adminLondon.getMachines().getMachines()[0].getGridServiceAgents()
				.getAgents()[0].killByAgentId(hsqlId2);
		TeardownUtils.teardownAll(adminIsrael, adminLondon, adminNY);
		adminIsrael.close();
		adminLondon.close();
		adminNY.close();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "3", enabled = true)
	public void test() throws Exception {
		initialize();

		case1();
		case2();
		case3();
		case4();
		case5();
		case6();
		case7();
		case8();
		case9();
		case10();

	}

	public void case1() throws Exception {
		log("starting test case 1");
		setUpTestCase();
		Stock stock = new Stock(1);
		stock.setStockName("GS");
		gigaSpaceIsrael.write(stock);

		TestUtils.repetitive(new Runnable() {
			public void run() {
				Assert.assertNotNull(gigaSpaceIsrael.read(new Stock()));
				Assert.assertEquals(gigaSpaceNY.read(new Stock()),
						gigaSpaceIsrael.read(new Stock()));
				Assert.assertNull(gigaSpaceLondon.read(new Stock()));
			}
		}, (int) DEFAULT_TEST_TIMEOUT);
	}

	public void case2() throws Exception {
		log("starting test case 2");
		setUpTestCase();
		Stock stock = new Stock(1);
		stock.setStockName("GS");
		gigaSpaceNY.write(stock);

		TestUtils.repetitive(new Runnable() {
			public void run() {
				Assert.assertNotNull(gigaSpaceNY.read(new Stock()));
				Assert.assertEquals(gigaSpaceNY.read(new Stock()),
						gigaSpaceIsrael.read(new Stock()));
				Assert.assertNull(gigaSpaceLondon.read(new Stock()));
			}
		}, (int) DEFAULT_TEST_TIMEOUT);
	}

	public void case3() throws Exception {
		log("starting test case 3");
		setUpTestCase();
		Stock stock = new Stock(1);
		stock.setStockName("GS");
		gigaSpaceLondon.write(stock);

		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				Assert.assertNull(gigaSpaceIsrael.read(new Stock()));
				Assert.assertNull(gigaSpaceNY.read(new Stock()));
				return true;
			}
		};
		
		AssertUtils.repetitiveAssertConditionHolds("there are objects in un replicated space", condition, 4000, 500);
	}

	public void case4() throws Exception {
		log("starting test case 4");
		setUpTestCase();
		Stock[] stocks = new Stock[ENTRY_SIZE];
		for (int i = 0; i < ENTRY_SIZE; i++) {
			Stock stock = new Stock(i);
			stock.setStockName("GS");
			stocks[i] = stock;
		}

		gigaSpaceIsrael.writeMultiple(stocks);

		TestUtils.repetitive(new Runnable() {
			public void run() {
				AssertUtils.assertEquivalenceArrays(
						"readMultiple returned wrong results",
						gigaSpaceNY.readMultiple(new Stock()),
						gigaSpaceIsrael.readMultiple(new Stock()));
				Assert.assertEquals(0,
						gigaSpaceLondon.readMultiple(new Stock()).length);
			}
		}, (int) DEFAULT_TEST_TIMEOUT);
	}

	public void case5() throws Exception {
		log("starting test case 5");
		setUpTestCase();
		Stock[] stocks = new Stock[ENTRY_SIZE];
		for (int i = 0; i < ENTRY_SIZE; i++) {
			Stock stock = new Stock(i);
			stock.setStockName("GS");
			stocks[i] = stock;
		}

		gigaSpaceNY.writeMultiple(stocks);

		TestUtils.repetitive(new Runnable() {
			public void run() {
				AssertUtils.assertEquivalenceArrays(
						"readMultiple returned wrong results",
						gigaSpaceNY.readMultiple(new Stock()),
						gigaSpaceIsrael.readMultiple(new Stock()));
				Assert.assertEquals(0,
						gigaSpaceLondon.readMultiple(new Stock()).length);
			}
		}, (int) DEFAULT_TEST_TIMEOUT);

		for (int i = 0; i < ENTRY_SIZE; i++) {
			stocks[i].setStockName("CA");
		}
		gigaSpaceIsrael.writeMultiple(stocks);
		TestUtils.repetitive(new Runnable() {
			public void run() {
				AssertUtils.assertEquivalenceArrays(
						"readMultiple returned wrong results",
						gigaSpaceNY.readMultiple(new Stock()),
						gigaSpaceIsrael.readMultiple(new Stock()));
				Assert.assertEquals(0,
						gigaSpaceLondon.readMultiple(new Stock()).length);
			}
		}, (int) DEFAULT_TEST_TIMEOUT);

		TestUtils.repetitive(new Runnable() {
			public void run() {
				Stock[] stocks = gigaSpaceIsrael.readMultiple(new Stock());
				for (int i = 0; i < stocks.length; i++) {
					Assert.assertEquals("CA", stocks[i].getStockName());
				}
			}
		}, (int) DEFAULT_TEST_TIMEOUT);

	}

	public void case6() throws Exception {
		log("starting test case 6");
		setUpTestCase();

		undeploy(adminLondon, "londonSpace");
		gigaSpaceIsrael.write(new Stock(0));

		TestUtils.repetitive(new Runnable() {
			public void run() {
				Assert.assertEquals(null, gigaSpaceNY.read(new Stock()));
			}
		}, (int) DEFAULT_TEST_TIMEOUT);

		deployLondonSite();

		assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(adminNY,
				adminIsrael, adminLondon);
		TestUtils.repetitive(new Runnable() {
			public void run() {
				Assert.assertEquals(gigaSpaceIsrael.read(new Stock()),
						gigaSpaceNY.read(new Stock()));
			}
		}, (int) DEFAULT_TEST_TIMEOUT);
	}

	public void case7() throws Exception {
		log("starting test case 7");
		setUpTestCase();

		undeploy(adminIsrael, "ISRAEL-ROUTING");
		gigaSpaceIsrael.write(new Stock(0));

		TestUtils.repetitive(new Runnable() {
			public void run() {
				Assert.assertEquals(null, gigaSpaceNY.read(new Stock()));
				Assert.assertEquals(null, gigaSpaceLondon.read(new Stock()));
			}
		}, (int) DEFAULT_TEST_TIMEOUT);

		deployIsraelRouting();

		assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(adminNY,
				adminIsrael, adminLondon);
		TestUtils.repetitive(new Runnable() {
			public void run() {
				Assert.assertEquals(gigaSpaceIsrael.read(new Stock()),
						gigaSpaceNY.read(new Stock()));
			}
		}, (int) DEFAULT_TEST_TIMEOUT);
	}

	public void case8() throws Exception {
		log("starting test case 8");
		setUpTestCase();

		killGSC(adminLondon, "LONDON-GW");

		gigaSpaceIsrael.write(new Stock(0));

		assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(adminNY,
				adminIsrael, adminLondon);
		TestUtils.repetitive(new Runnable() {
			public void run() {
				Assert.assertEquals(gigaSpaceIsrael.read(new Stock()),
						gigaSpaceNY.read(new Stock()));
			}
		}, (int) DEFAULT_TEST_TIMEOUT);
	}

	public void case9() throws Exception {
		log("starting test case 9");
		setUpTestCase();

		final Stock[] stocks = new Stock[ENTRY_SIZE];
		for (int i = 0; i < ENTRY_SIZE; i++) {
			stocks[i] = new Stock(i);
		}

		killGSC(adminIsrael, "ISRAEL-SINK");

		gigaSpaceNY.writeMultiple(stocks, Lease.FOREVER,
				Modifiers.NO_RETURN_VALUE);

		TestUtils.repetitive(new Runnable() {
			public void run() {
				Stock[] stocks = gigaSpaceNY.readMultiple(new Stock());
				Stock[] stocks1 = gigaSpaceIsrael.readMultiple(new Stock());
				Stock[] stocks2 = gigaSpaceLondon.readMultiple(new Stock());
				Assert.assertEquals(0, stocks2.length);
				Assert.assertEquals(0, stocks1.length);
				Assert.assertEquals(ENTRY_SIZE, stocks.length);
			}
		}, (int) DEFAULT_TEST_TIMEOUT);

		assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(adminNY,
				adminIsrael, adminLondon);
		TestUtils.repetitive(new Runnable() {
			public void run() {
				Stock[] stocks = gigaSpaceNY.readMultiple(new Stock());
				Stock[] stocks1 = gigaSpaceIsrael.readMultiple(new Stock());
				Stock[] stocks2 = gigaSpaceLondon.readMultiple(new Stock());
				AssertUtils.assertEquivalenceArrays(
						"readMultiple returned wrong results", stocks, stocks1);
				Assert.assertEquals(0, stocks2.length);
			}
		}, (int) DEFAULT_TEST_TIMEOUT);

	}

	public void case10() throws Exception {
		log("starting test case 10");
		setUpTestCase();

		final Stock[] stocks = new Stock[ENTRY_SIZE];
		for (int i = 0; i < ENTRY_SIZE; i++) {
			stocks[i] = new Stock(i);
		}
		gigaSpaceIsrael.writeMultiple(stocks, Lease.FOREVER,
				Modifiers.NO_RETURN_VALUE);

		restartSitePrimaries(adminLondon, "londonSpace");

		TestUtils.repetitive(new Runnable() {
			public void run() {
				assertPrimaries(adminLondon, 1);
			}
		}, (int) DEFAULT_TEST_TIMEOUT);

		assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(adminIsrael,
				adminLondon);
		TestUtils.repetitive(new Runnable() {
			public void run() {
				Stock[] stocks = gigaSpaceNY.readMultiple(new Stock());
				Stock[] stocks1 = gigaSpaceIsrael.readMultiple(new Stock());
				AssertUtils.assertEquals(stocks.length, stocks1.length);
				AssertUtils.assertEquivalenceArrays(
						"readMultiple returned wrong results", stocks, stocks1);
			}
		}, (int) DEFAULT_TEST_TIMEOUT);
	}

	public void setUpTestCase() {
		gigaSpaceNY.clear(null);
		gigaSpaceIsrael.clear(null);
		gigaSpaceLondon.clear(null);
		assertGatewayReplicationHasNotingToReplicate(adminNY, adminIsrael, adminLondon);
	}

	private void killGSC(Admin admin, String puiName) {
		for (GridServiceContainer gsc : admin.getGridServiceContainers()) {
			for (ProcessingUnitInstance pui : gsc.getProcessingUnitInstances()) {
				if (pui.getName().equals(puiName)) {
					gsc.kill();
				}
			}
		}
	}

	private void undeploy(Admin admin, String puName) {
		for (GridServiceContainer gsc : admin.getGridServiceContainers()) {
			for (ProcessingUnitInstance pui : gsc.getProcessingUnitInstances()) {
				if (pui.getName().equals(puName)) {
					pui.getProcessingUnit().undeploy();
				}
			}
		}
	}

	private void restartSitePrimaries(Admin admin, String puName) {
		for (ProcessingUnitInstance pui : admin.getProcessingUnits()
				.getProcessingUnit(puName).getInstances()) {
			if (pui.getSpaceInstance().getMode().compareTo(SpaceMode.PRIMARY) == 0) {
				pui.restart();
			}
		}

	}

	private void initialize() throws Exception {
		log("initializing..");
		adminIsrael = new AdminFactory().addGroups(group1).createAdmin();
		adminLondon = new AdminFactory().addGroups(group2).createAdmin();
		adminNY = new AdminFactory().addGroups(group3).createAdmin();

		SetupUtils.assertCleanSetup(adminIsrael);
		SetupUtils.assertCleanSetup(adminLondon);
		SetupUtils.assertCleanSetup(adminNY);

		GridServiceAgent gsa1 = adminIsrael.getGridServiceAgents()
				.waitForAtLeastOne();
		GridServiceAgent gsa2 = adminLondon.getGridServiceAgents()
				.waitForAtLeastOne();
		GridServiceAgent gsa3 = adminNY.getGridServiceAgents()
				.waitForAtLeastOne();

		gsa1.startGridService(new GridServiceManagerOptions());
		gsa1.startGridService(new GridServiceContainerOptions());
		gsa1.startGridService(new GridServiceContainerOptions());

		gsa2.startGridService(new GridServiceManagerOptions());
		gsa2.startGridService(new GridServiceContainerOptions());
		gsa2.startGridService(new GridServiceContainerOptions());

		gsa3.startGridService(new GridServiceManagerOptions());
		gsa3.startGridService(new GridServiceContainerOptions());
		gsa3.startGridService(new GridServiceContainerOptions());

		gsmISR = adminIsrael.getGridServiceManagers().waitForAtLeastOne();
		gsmLON = adminLondon.getGridServiceManagers().waitForAtLeastOne();
		gsmNY = adminNY.getGridServiceManagers().waitForAtLeastOne();

		log("load HSQL DB on machine - "
				+ adminNY.getMachines().getMachines()[0].getHostAddress());
		hsqlId1 = DBUtils.loadHSQLDB(adminNY.getMachines().getMachines()[0],
				"DB1", dbPortISR);

		log("load HSQL DB on machine - "
				+ adminLondon.getMachines().getMachines()[0].getHostAddress());
		hsqlId2 = DBUtils.loadHSQLDB(adminLondon.getMachines().getMachines()[0],
				"DB2", dbPortNY);

		Thread.sleep(10000);

		log("deploying PUs");
		deployIsraelMirror();
		deployIsraelSite();
		deployNYMirror();
		deployNYSite();

		deployLondonSite();
		deployIsraelSink();
		deployIsraelRouting();
		deployNYSink();
		deployNYRouting();
		deployLondonGW();

		adminIsrael.getGridServiceContainers().waitFor(3);
		adminLondon.getGridServiceContainers().waitFor(3);
		adminNY.getGridServiceContainers().waitFor(3);

		Space space1 = adminIsrael.getSpaces().waitFor("israelSpace");
		Space space2 = adminLondon.getSpaces().waitFor("londonSpace");
		Space space = adminNY.getSpaces().waitFor("NYSpace");

		gigaSpaceNY = space.getGigaSpace();
		gigaSpaceIsrael = space1.getGigaSpace();
		gigaSpaceLondon = space2.getGigaSpace();

		log("validating gateway components");

		// Verify delegators & sinks are connected.
		assertGatewayReplicationConnected(space, 1);
		assertGatewayReplicationConnected(space1, 1);
		assertGatewayReplicationConnected(space2, 0);

		log("finished initialziation");

	}

	@Override
	protected void assertGatewayReplicationConnected(Space space, final int numberOfGatewayTargets) {
	    for (final SpacePartition spaceInstance : space.getPartitions()) {
	        TestUtils.repetitive(new Runnable() {

	            public void run() {
	                int gatewayReplicationsCount = 0;
	                for (ReplicationTarget target : spaceInstance.getPrimary().getReplicationTargets()) {
	                    if (target.getMemberName().startsWith("gateway:")) {
	                        Assert.assertEquals(ReplicationStatus.ACTIVE, target.getReplicationStatus());
	                        gatewayReplicationsCount++;
	                    }
	                }
	                Assert.assertEquals(numberOfGatewayTargets, gatewayReplicationsCount);
	            }

	        }, (int)60000 * 5);
	    }
	}
	
	private void deployLondonGW() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "LONDON");
		props.put("targetGatewayName", "NY");
		props.put("target2GatewayName", "ISRAEL");
		props.put("localClusterUrl", "jini://*/*/londonSpace");
		props.put("gatewayLookupGroup", UUID.randomUUID().toString());
		props.put("localGatewayHost", gsmLON.getVirtualMachine().getMachine()
				.getHostName());
		props.put("localGatewayDiscoveryPort", "10001");
		props.put("localGatewayLrmiPort", "7001");
		props.put("targetGatewayHost", gsmNY.getVirtualMachine().getMachine()
				.getHostName());
		props.put("targetGatewayDiscoveryPort", "10002");
		props.put("targetGatewayLrmiPort", "7002");
		props.put("target2GatewayHost", gsmISR.getVirtualMachine().getMachine()
				.getHostName());
		props.put("target2GatewayDiscoveryPort", "10000");
		props.put("target2GatewayLrmiPort", "7000");
		deployGateway(
				gsmLON,
				siteDeployment(
						"./apps/gateway/gatewayDelegatorWith2TargetsOnly",
						"LONDON-GW", props));
	}

	private void deployNYRouting() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "NY");
		props.put("targetGatewayName", "ISRAEL");
		props.put("targetDelegateThrough", "LONDON");
		props.put("localClusterUrl", "jini://*/*/NYSpace");
		props.put("gatewayLookupGroup", UUID.randomUUID().toString());
		props.put("localGatewayHost", gsmNY.getVirtualMachine().getMachine()
				.getHostName());
		props.put("localGatewayDiscoveryPort", "10004");
		props.put("localGatewayLrmiPort", "7004");
		props.put("targetGatewayHost", gsmLON.getVirtualMachine().getMachine()
				.getHostName());
		props.put("targetGatewayDiscoveryPort", "10001");
		props.put("targetGatewayLrmiPort", "7001");
		deployGateway(
				gsmNY,
				siteDeployment(
						"./apps/gateway/gatewayRoutingComponentsWithRelocate",
						"NY-ROUTING", props));
	}

	private void deployNYSink() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "NY");
		props.put("targetGatewayName", "ISRAEL");
		props.put("target2GatewayName", "LONDON");
		props.put("localClusterUrl", "jini://*/*/NYSpace");
		props.put("gatewayLookupGroup", UUID.randomUUID().toString());
		props.put("localGatewayHost", gsmNY.getVirtualMachine().getMachine()
				.getHostName());
		props.put("localGatewayDiscoveryPort", "10002");
		props.put("localGatewayLrmiPort", "7002");
		props.put("targetGatewayHost", gsmISR.getVirtualMachine().getMachine()
				.getHostName());
		props.put("targetGatewayDiscoveryPort", "10000");
		props.put("targetGatewayLrmiPort", "7000");
		props.put("target2GatewayHost", gsmLON.getVirtualMachine().getMachine()
				.getHostName());
		props.put("target2GatewayDiscoveryPort", "10001");
		props.put("target2GatewayLrmiPort", "7001");
		deployGateway(
				gsmNY,
				siteDeployment("./apps/gateway/gatewayTwoSourcesSinkOnly",
						"NY-SINK", props));
	}

	private void deployIsraelRouting() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "ISRAEL");
		props.put("targetGatewayName", "NY");
		props.put("targetDelegateThrough", "LONDON");
		props.put("localClusterUrl", "jini://*/*/israelSpace");
		props.put("gatewayLookupGroup", UUID.randomUUID().toString());
		props.put("localGatewayHost", gsmISR.getVirtualMachine().getMachine()
				.getHostName());
		props.put("localGatewayDiscoveryPort", "10003");
		props.put("localGatewayLrmiPort", "7003");
		props.put("targetGatewayHost", gsmLON.getVirtualMachine().getMachine()
				.getHostName());
		props.put("targetGatewayDiscoveryPort", "10001");
		props.put("targetGatewayLrmiPort", "7001");
		deployGateway(
				gsmISR,
				siteDeployment(
						"./apps/gateway/gatewayRoutingComponentsWithRelocate",
						"ISRAEL-ROUTING", props));
	}

	private void deployIsraelSink() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "ISRAEL");
		props.put("target2GatewayName", "NY");
		props.put("targetGatewayName", "LONDON");
		props.put("localClusterUrl", "jini://*/*/israelSpace");
		props.put("gatewayLookupGroup", UUID.randomUUID().toString());
		props.put("localGatewayHost", gsmISR.getVirtualMachine().getMachine()
				.getHostName());
		props.put("localGatewayDiscoveryPort", "10000");
		props.put("localGatewayLrmiPort", "7000");
		props.put("targetGatewayHost", gsmLON.getVirtualMachine().getMachine()
				.getHostName());
		props.put("targetGatewayDiscoveryPort", "10001");
		props.put("targetGatewayLrmiPort", "7001");
		props.put("target2GatewayHost", gsmNY.getVirtualMachine().getMachine()
				.getHostName());
		props.put("target2GatewayDiscoveryPort", "10002");
		props.put("target2GatewayLrmiPort", "7002");
		deployGateway(
				gsmISR,
				siteDeployment("./apps/gateway/gatewayTwoSourcesSinkOnly",
						"ISRAEL-SINK", props));
	}

	private void deployLondonSite() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "LONDON");
		props.put("spaceUrl", "/./londonSpace");
		deploySite(
				gsmLON,
				siteDeployment("./apps/gateway/clusterWithoutTargets",
						"londonSpace", props));
	}

	private void deployNYSite() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "NY");
		props.put("gatewayTarget", "ISRAEL");
		props.put("spaceUrl", "/./NYSpace");
		props.put("host", gsmNY.getVirtualMachine().getMachine().getHostName());
		props.put("port", String.valueOf(dbPortNY));
		deploySite(gsmNY,
				sitePrepareAndDeployment("processorAsyncPersistent", props));
	}

	private void deployNYMirror() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("remoteSpace", "NYSpace");
		props.put("host", gsmNY.getVirtualMachine().getMachine().getHostName());
		props.put("port", String.valueOf(dbPortNY));
		deploySite(gsmNY, sitePrepareAndDeployment("mirror", props));
	}

	private void deployIsraelSite() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "ISRAEL");
		props.put("gatewayTarget", "NY");
		props.put("spaceUrl", "/./israelSpace");
		props.put("host", gsmISR.getVirtualMachine().getMachine().getHostName());
		props.put("port", String.valueOf(dbPortISR));
		deploySite(gsmISR,
				sitePrepareAndDeployment("processorAsyncPersistent", props)
						.numberOfInstances(2).numberOfBackups(1));
	}

	private void deployIsraelMirror() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("remoteSpace", "israelSpace");
		props.put("host", gsmISR.getVirtualMachine().getMachine().getHostName());
		props.put("port", String.valueOf(dbPortISR));
		deploySite(gsmISR, sitePrepareAndDeployment("mirror", props));
	}
}