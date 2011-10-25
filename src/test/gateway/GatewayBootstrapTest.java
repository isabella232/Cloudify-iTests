package test.gateway;

import org.junit.Assert;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import test.AbstractTest;
import test.data.Person;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;
import test.utils.TestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Basic gateway test.
 *
 * @author idan
 * @since 8.0.3
 */
public class GatewayBootstrapTest extends AbstractGatewayTest {

	private Admin admin1;
	private Admin admin2;
	private GigaSpace gigaSpace2;
	private GigaSpace gigaSpace1;

	private String group1 = null;
	private String group2 = null;
	private String host1 = null;
	private String host2 = null;
	private GridServiceManager gsm1;
	private GridServiceManager gsm2;

	public GatewayBootstrapTest() {
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

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="2", enabled=true)
	public void testBootstrap() throws Exception {
		initialize();
		final Person[] persons = new Person[ENTRY_SIZE];
		for (int i = 0; i < persons.length; i++) {
			persons[i] = new Person((long) i);
		}
		gigaSpace1.writeMultiple(persons);
		Thread.sleep(5000);

		Assert.assertEquals(0, gigaSpace2.count(null));

		ProcessingUnit sink = admin2.getProcessingUnits().waitFor("LONDON");
		sink.waitFor(1);

		Thread.sleep(10000);
		enableIncomingReplication(admin2, "LONDON", "ISRAEL");

		// Verify delegators & sinks are connected.
		Space space1 = admin1.getSpaces().waitFor("israelSpace");
		Space space2 = admin2.getSpaces().waitFor("londonSpace");
		assertGatewayReplicationConnected(space1, 1);
		assertGatewayReplicationConnected(space2, 1);

		TestUtils.repetitive(new Runnable() {

			public void run() {
				Person[] result = gigaSpace2.readMultiple(new Person());
				Assert.assertEquals(persons.length, result.length);
			}

		}, (int)DEFAULT_TEST_TIMEOUT);

		ProcessingUnit spacePu = admin2.getProcessingUnits().waitFor("londonSpace");
		spacePu.waitFor(2);
		spacePu.undeploy();

		sink.getInstances()[0].restartAndWait();
		deployCluster("LONDON", "ISRAEL", "londonSpace", "/./londonSpace", gsm2);

		bootstrap(admin2, "LONDON", "ISRAEL");

		// Verify delegators & sinks are connected.
		space1 = admin1.getSpaces().waitFor("israelSpace");
		space2 = admin2.getSpaces().waitFor("londonSpace");
		assertGatewayReplicationConnected(space1, 1);
		assertGatewayReplicationConnected(space2, 1);

		TestUtils.repetitive(new Runnable() {

			public void run() {
				Person[] result = gigaSpace2.readMultiple(new Person());
				Assert.assertEquals(persons.length, result.length);
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
		AbstractTest.assertNotNull("Could not find agent on group: " + admin1.getGroups()[0], gsa1);
		AbstractTest.assertNotNull("Could not find agent on group: " + admin2.getGroups()[0], gsa2);

		gsa1.startGridServiceAndWait(new GridServiceManagerOptions());
		gsa1.startGridServiceAndWait(new GridServiceContainerOptions());
		gsa1.startGridServiceAndWait(new GridServiceContainerOptions());

		gsa2.startGridServiceAndWait(new GridServiceManagerOptions());
		gsa2.startGridServiceAndWait(new GridServiceContainerOptions());
		gsa2.startGridServiceAndWait(new GridServiceContainerOptions());

		gsm1 = admin1.getGridServiceManagers().waitForAtLeastOne();
		gsm2 = admin2.getGridServiceManagers().waitForAtLeastOne();

		deployCluster("ISRAEL", "LONDON", "israelSpace", "/./israelSpace", gsm1);
		deployCluster("LONDON", "ISRAEL", "londonSpace", "/./londonSpace", gsm2);

		deployIsraelGateway();
		deployLondonGateway();

		admin1.getGridServiceContainers().waitFor(3);
		admin2.getGridServiceContainers().waitFor(3);

		Space space1 = admin1.getSpaces().waitFor("israelSpace");
		Space space2 = admin2.getSpaces().waitFor("londonSpace");

		gigaSpace1 = space1.getGigaSpace();
		gigaSpace2 = space2.getGigaSpace();
	}

	private void deployLondonGateway() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "LONDON");
		props.put("targetGatewayName", "ISRAEL");
		props.put("localClusterUrl", "jini://*/*/londonSpace?groups=" + group2);
		props.put("gatewayLookupGroup", UUID.randomUUID().toString());
		props.put("localGatewayHost", host2);
		props.put("localGatewayDiscoveryPort", "10002");
		props.put("localGatewayLrmiPort", "7001");
		props.put("targetGatewayHost", host1);
		props.put("targetGatewayDiscoveryPort", "10001");
		props.put("targetGatewayLrmiPort", "7000");
		deployGateway(gsm2, gatewayDeployment("./apps/gateway/gatewayBootstrap", "LONDON", props));
		props.clear();
	}

	private void deployIsraelGateway() {
		Map<String, String> props = new HashMap<String, String>();
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
	}

	private void deployCluster(String localGatewayName, String targetGatewayName, String puName, String spaceUrl, GridServiceManager gsm) {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", localGatewayName);
		props.put("gatewayTarget", targetGatewayName);
		props.put("spaceUrl", spaceUrl);
		deploySite(gsm, siteDeployment("./apps/gateway/cluster", puName, props));
		props.clear();
	}

	@AfterMethod
	public void tearDown() {
		TeardownUtils.teardownAll(admin1, admin2);
		admin1.close();
		admin2.close();
	}
}
