package test.gateway;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gateway.Gateway;
import org.openspaces.admin.gateway.GatewayDelegator;
import org.openspaces.admin.gateway.GatewaySink;
import org.openspaces.admin.gateway.Gateways;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.space.Space;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;
import test.utils.ThreadBarrier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;

import static test.utils.LogUtils.log;

/**
 * @author sagib
 * @since 8.0.4
 */
public class GatewayFullGraphAdminApiTest extends AbstractGatewayTest {

	private Admin adminISR;
	private Admin adminLON;
	private Admin adminNY;
	private String groupISR = null;
	private String groupLON = null;
	private String groupNY = null;
	private String hostISR = null;
	private String hostLON = null;
	private String hostNY = null;

	public GatewayFullGraphAdminApiTest() {
		if (isDevMode()) {
			groupISR = "israel-" + getUserName();
			groupLON = "london-" + getUserName();
			groupNY = "ny-" + getUserName();
			hostISR = "localhost";
			hostLON = "localhost";
			hostNY = "localhost";
		} else {
			groupISR = GROUP1;
			groupLON = GROUP2;
			groupNY = GROUP3;
			hostISR = HOST1;
			hostLON = HOST2;
			hostNY = HOST3;
		}
	}

	@AfterMethod
	public void tearDown() {
		Admin[] admins = {adminISR, adminLON, adminNY};
		TeardownUtils.teardownAll(admins);
		adminISR.close();
		adminLON.close();
		adminNY.close();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="3", enabled=true)
	public void test() throws Exception {
		initialize();

		Gateways gateways = adminISR.getGateways();
		Assert.assertEquals(gateways.isEmpty(), false);
		Assert.assertEquals(gateways.getSize(), 1);
		Assert.assertEquals(gateways.getAdmin(), adminISR);
		Assert.assertNotNull(gateways.iterator());
		Assert.assertEquals(gateways.getNames().size(),1);
		Assert.assertNotNull(gateways.getGateway("ISRAEL"));
		Assert.assertEquals(gateways.getGateways().length, 1);
		Assert.assertNotNull(gateways.waitFor("ISRAEL"));
		Assert.assertNotNull(gateways.waitFor("ISRAEL", 30, TimeUnit.SECONDS));

		Gateway gateway = gateways.getGateway("ISRAEL");
		Assert.assertTrue(gateway.getNames().containsKey("ISRAEL"));
		Assert.assertNotNull(gateway.waitForSinkSource("LONDON"));
		Assert.assertNotNull(gateway.waitForSinkSource("LONDON", 30, TimeUnit.SECONDS));
		Assert.assertNotNull(gateway.waitForSink("LONDON"));
		Assert.assertNotNull(gateway.waitForSink("LONDON", 30, TimeUnit.SECONDS));
		Assert.assertEquals(gateway.getGatewayProcessingUnits().length, 1);
		Assert.assertNotNull(gateway.waitForGatewayProcessingUnit("ISRAEL"));
		Assert.assertNotNull(gateway.waitForGatewayProcessingUnit("ISRAEL", 30, TimeUnit.SECONDS));
		Assert.assertNotNull(gateway.getGatewayProcessingUnit("ISRAEL"));
		Assert.assertNotNull(gateway.getSink("LONDON"));
		Assert.assertNotNull(gateway.getSinkSource("LONDON"));
		Assert.assertNotNull(gateway.getDelegator("LONDON"));
		Assert.assertNotNull(gateway.waitForDelegator("LONDON"));
		Assert.assertNotNull(gateway.waitForDelegator("LONDON", 30, TimeUnit.SECONDS));
		Assert.assertEquals(gateway.getName(), "ISRAEL");
		Assert.assertEquals(gateway.isEmpty(), false);
		Assert.assertEquals(gateway.getSize(), 1);
		Assert.assertNotNull(gateway.waitFor(1));
		Assert.assertNotNull(gateway.waitFor(1, 30 , TimeUnit.SECONDS));

		GatewaySink gatewaySink = gateway.getSink("LONDON");
		Assert.assertNotNull(gatewaySink.getGatewayProcessingUnit());
		Assert.assertNotNull(gatewaySink.getLocalSpaceUrl());
		Assert.assertTrue(gatewaySink.containsSource("LONDON"));
		Assert.assertNotNull(gatewaySink.getSourceByName("LONDON"));
		Assert.assertFalse(gatewaySink.requiresBootstrapOnStartup());
		Assert.assertEquals(gatewaySink.getSources().length, 3);
		
		GatewayDelegator gatewayDelegator = gateway.getDelegator("LONDON");
		Assert.assertNotNull(gatewayDelegator.getGatewayProcessingUnit());
		Assert.assertEquals(gatewayDelegator.getDelegationTargets().length, 2);
		Assert.assertTrue(gatewayDelegator.containsTarget("LONDON"));
		
		

	}
	private void initialize() throws Exception {
		log("initializing..");
		final ThreadBarrier barrier = new ThreadBarrier(4);
		final Object monitor = new Object();
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				adminISR = new AdminFactory().addGroups(groupISR).createAdmin();
				SetupUtils.assertCleanSetup(adminISR);
				GridServiceAgent gsaISR = adminISR.getGridServiceAgents().waitForAtLeastOne();
				
				gsaISR.startGridService(new GridServiceManagerOptions());
				gsaISR.startGridService(new GridServiceContainerOptions());
				gsaISR.startGridService(new GridServiceContainerOptions());

				final GridServiceManager gsmISR = adminISR.getGridServiceManagers().waitForAtLeastOne();
				AbstractTest.assertNotNull(gsmISR);
				
				
				log("deploying Israel site");
				Map<String, String> props = new HashMap<String, String>();
				props.put("localGatewayName", "ISRAEL");
				props.put("gatewayTarget", "LONDON");
				props.put("gatewayTarget2", "NY");
				props.put("spaceUrl", "/./israelSpace");
				synchronized (monitor) {
					deploySite(
							gsmISR,
							siteDeployment(
									"./apps/gateway/clusterWith2targets",
									"israelSpace", props).numberOfInstances(1)
									.numberOfBackups(1));
				}
				props.clear();
				
				log("deploying Israel Gateway");
				props.put("localGatewayName", "ISRAEL");
				props.put("targetGatewayName", "LONDON");
				props.put("target2GatewayName", "NY");
				props.put("localClusterUrl", "jini://*/*/israelSpace");
				props.put("gatewayLookupGroup", UUID.randomUUID().toString());
				props.put("localGatewayHost", hostISR);
				props.put("localGatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);
				props.put("localGatewayLrmiPort", ISRAEL_GATEWAY_LRMI_PORT);
				props.put("targetGatewayHost", hostLON);
				props.put("targetGatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
				props.put("targetGatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
				props.put("target2GatewayHost", hostNY);
				props.put("target2GatewayDiscoveryPort", NY_GATEWAY_DISCOVERY_PORT);
				props.put("target2GatewayLrmiPort", NY_GATEWAY_LRMI_PORT);
				synchronized (monitor) {
					deployGateway(
							gsmISR,
							siteDeployment("./apps/gateway/gatewayDoubleLink",
									"ISRAEL", props));
				}
				props.clear();

				try {
					barrier.await();
				} catch (InterruptedException e) {
					Assert.fail();
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					Assert.fail();
					e.printStackTrace();
				}
			}
		}).start();

		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				adminLON = new AdminFactory().addGroups(groupLON).createAdmin();
				SetupUtils.assertCleanSetup(adminLON);
				GridServiceAgent gsaLON = adminLON.getGridServiceAgents().waitForAtLeastOne();
				
				gsaLON.startGridService(new GridServiceManagerOptions());
				gsaLON.startGridService(new GridServiceContainerOptions());
				gsaLON.startGridService(new GridServiceContainerOptions());

				final GridServiceManager gsmLON = adminLON.getGridServiceManagers().waitForAtLeastOne();
				AbstractTest.assertNotNull(gsmLON);

				
				log("deploying London site");
				Map<String, String> props = new HashMap<String, String>();
				props.put("localGatewayName", "LONDON");
				props.put("gatewayTarget", "ISRAEL");
				props.put("gatewayTarget2", "NY");
				props.put("spaceUrl", "/./londonSpace");
				synchronized (monitor) {
					deploySite(
							gsmLON,
							siteDeployment(
									"./apps/gateway/clusterWith2targets",
									"londonSpace", props).numberOfInstances(1)
									.numberOfBackups(1));
				}
				props.clear();
				
				
				log("deploying London Gateway");
				props.put("localGatewayName", "LONDON");
				props.put("targetGatewayName", "ISRAEL");
				props.put("target2GatewayName", "NY");
				props.put("localClusterUrl", "jini://*/*/londonSpace");
				props.put("gatewayLookupGroup", UUID.randomUUID().toString());
				props.put("localGatewayHost", hostLON);
				props.put("localGatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
				props.put("localGatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
				props.put("targetGatewayHost", hostISR);
				props.put("targetGatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);
				props.put("targetGatewayLrmiPort", ISRAEL_GATEWAY_LRMI_PORT);
				props.put("target2GatewayHost", hostNY);
				props.put("target2GatewayDiscoveryPort", NY_GATEWAY_DISCOVERY_PORT);
				props.put("target2GatewayLrmiPort", NY_GATEWAY_LRMI_PORT);
				synchronized (monitor) {
					deployGateway(
							gsmLON,
							siteDeployment("./apps/gateway/gatewayDoubleLink",
									"LONDON", props));
				}
				props.clear();
				
				try {
					barrier.await();
				} catch (InterruptedException e) {
					Assert.fail();
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					Assert.fail();
					e.printStackTrace();
				}
			}
		}).start();
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
		
		adminNY = new AdminFactory().addGroups(groupNY).createAdmin();
		SetupUtils.assertCleanSetup(adminNY);
		
		GridServiceAgent gsaNY = adminNY.getGridServiceAgents().waitForAtLeastOne();
		
		gsaNY.startGridService(new GridServiceManagerOptions());
		gsaNY.startGridService(new GridServiceContainerOptions());
		gsaNY.startGridService(new GridServiceContainerOptions());
		
		final GridServiceManager gsmNY = adminNY.getGridServiceManagers().waitForAtLeastOne();
		AbstractTest.assertNotNull(gsmNY);
		
		
		
		log("deploying NY site");
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "NY");
		props.put("gatewayTarget", "ISRAEL");
		props.put("gatewayTarget2", "LONDON");
		props.put("spaceUrl", "/./nySpace");
		synchronized (monitor) {
			deploySite(
					gsmNY,
					siteDeployment("./apps/gateway/clusterWith2targets",
							"nySpace", props).numberOfInstances(1)
							.numberOfBackups(1));
		}
		props.clear();
		
		
		
		log("deploying NY Gateway");
		props.put("localGatewayName", "NY");
		props.put("targetGatewayName", "ISRAEL");
		props.put("target2GatewayName", "LONDON");
		props.put("localClusterUrl", "jini://*/*/nySpace");
		props.put("gatewayLookupGroup", UUID.randomUUID().toString());
		props.put("localGatewayHost", hostNY);
		props.put("localGatewayDiscoveryPort", NY_GATEWAY_DISCOVERY_PORT);
		props.put("localGatewayLrmiPort", NY_GATEWAY_LRMI_PORT);
		props.put("targetGatewayHost", hostISR);
		props.put("targetGatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);
		props.put("targetGatewayLrmiPort", ISRAEL_GATEWAY_LRMI_PORT);
		props.put("target2GatewayHost", hostLON);
		props.put("target2GatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
		props.put("target2GatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
		synchronized (monitor) {
			deployGateway(
					gsmNY,
					siteDeployment("./apps/gateway/gatewayDoubleLink", "NY",
							props));
		}
		props.clear();
				
		try {
			barrier.await();
		} catch (InterruptedException e) {
			Assert.fail();
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			Assert.fail();
			e.printStackTrace();
		}
			}
		}).start();

		barrier.await();
		
		adminISR.getGridServiceContainers().waitFor(3);
		adminLON.getGridServiceContainers().waitFor(3);
		adminNY.getGridServiceContainers().waitFor(3);


		Space space1 = adminISR.getSpaces().waitFor("israelSpace");
		Space space2 = adminLON.getSpaces().waitFor("londonSpace");
		Space space3 = adminNY.getSpaces().waitFor("nySpace");

		space1.getGigaSpace();
		space2.getGigaSpace();
		space3.getGigaSpace();


		log("validating gateway components");


		// Verify delegators & sinks are connected.
		assertGatewayReplicationConnected(space1, 3, 2);
		assertGatewayReplicationConnected(space2, 3, 2);
		assertGatewayReplicationConnected(space3, 3, 2);

		log("finished initialziation");

	}

}