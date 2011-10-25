package test.gateway;

import static test.utils.LogUtils.log;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.gatewayPUs.common.MessageGW;

import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.ScriptUtils;
import test.utils.SetupUtils;
import test.utils.SftpUtils;
import test.utils.TeardownUtils;


/**
 * @author sagib
 * @since 8.0.3
 */
public class GatewayRollingUpgradeSimpleTest extends AbstractGatewayTest {

	private Admin adminISR;
	private Admin adminLON;
	private Admin adminNY;

	private String groupISR = null;
	private String groupLON = null;
	private String groupNY = null;
	private String hostISR = null;
	private String hostLON = null;
	private String hostNY = null;

	private GridServiceManager gsmNY;
	private GridServiceManager gsmISR;
	private GridServiceManager gsmLON;

	private String password = null;

	private final String s = "/";


	private String folderLayout = "META-INF/spring/";
	private String gwFileToReplace =  ScriptUtils.getBuildPath() + "/deploy/gateway-components/" + folderLayout + "pu.xml";
	private String siteFileToReplace = ScriptUtils.getBuildPath() + "/deploy/cluster/" + folderLayout + "pu.xml";
	private String curDir = System.getProperty("user.dir");

	private final String LonGwFileToReplace = ScriptUtils.getBuildPath() + s + "deploy" + s + "LondonGW" + s + folderLayout + "pu.xml";
	private final String LonSiteFileToReplace = ScriptUtils.getBuildPath() + s + "deploy" + s + "ClusterLondon" + s + folderLayout + "pu.xml";
	private final String NYGwFileToReplace = ScriptUtils.getBuildPath() + s + "deploy" + s + "NYGW" + s + folderLayout + "pu.xml";
	private final String NYSiteFileToReplace = ScriptUtils.getBuildPath() + s + "deploy" + s + "ClusterNY" + s + folderLayout + "pu.xml";


	private Space spaceLON;
	private Space spaceNY;
	private Space spaceISR;

	public GatewayRollingUpgradeSimpleTest() {
		if (isDevMode()) {
			groupISR = GROUP1;
			groupLON = GROUP2;
			groupNY = GROUP3;
			hostISR = "localhost";
			hostLON = "localhost";
			hostNY = "localhost";
			password = "SagB789";
		} else {
			groupISR = GROUP1;
			groupLON = GROUP2;
			groupNY = GROUP3;
			hostISR = HOST1;
			hostLON = HOST2;
			hostNY = HOST3;
			password = getUserName();
		}
	}

	@AfterMethod
	public void tearDown() {
		Admin[] admins = {adminLON, adminNY, adminISR};
		TeardownUtils.teardownAll(admins);
		adminLON.close();
		adminNY.close();
		adminISR.close();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "3", enabled=true)
	public void test() throws Exception {
		initialize();

		spaceLON = adminLON.getSpaces().waitFor("londonSpace");
		spaceNY = adminNY.getSpaces().waitFor("NYSpace");
		final GigaSpace gigaSpaceLondon = spaceLON.getGigaSpace();
		final GigaSpace gigaSpaceNY = spaceNY.getGigaSpace();

		GigaSpace [] targets = new GigaSpace[1];
		targets[0] = gigaSpaceNY;
		log("write multiple to London and assert replication to NY");
		writeMultiple(gigaSpaceLondon, targets, false);

		deployIsrael();


		spaceISR = adminISR.getSpaces().waitFor("israelSpace");
		
		log("replacing NY pu xmls");
		upgradeNY();
		
		log("restarting NY GW");
		restart(adminNY,"NY-GW");
		
		log("restarting NY site");
		restart(adminNY,"NYSpace");

		log("replacing London pu xmls");
		upgradeLondon();
		
		log("restarting London GW");
		restart(adminLON, "LONDON-GW");
		
		log("restarting London site");
		restart(adminLON, "londonSpace");

		
		log("asserting full graph replication");
		assertGatewayReplicationConnected(spaceISR, 2);
		assertGatewayReplicationConnected(spaceNY, 2);
		assertGatewayReplicationConnected(spaceLON, 2);

		log("assert that NY still contains written objects");
		Assert.assertTrue(gigaSpaceNY.readMultiple(new Object()).length == ENTRY_SIZE);

		GigaSpace gigaSpaceIsrael = spaceISR.getGigaSpace();

		log("write multiple to israel");
		final MessageGW[] msgArray = new MessageGW[ENTRY_SIZE];
		for (int i = ENTRY_SIZE; i < ENTRY_SIZE * 2; i++) {
			msgArray[i - ENTRY_SIZE] = new MessageGW(i, "Hello, world!");
		}
		gigaSpaceIsrael.writeMultiple(msgArray);
		
		RepetitiveConditionProvider condNY = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return gigaSpaceNY.readMultiple(new Object()).length == ENTRY_SIZE * 2;
			}
		};
		
		log("assert that NY contains both original and replicated from Israel objects");
		repetitiveAssertTrue("checking replication from Israel to NY", condNY, (int)DEFAULT_TEST_TIMEOUT);
		
		RepetitiveConditionProvider condLondon = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return gigaSpaceLondon.readMultiple(new Object()).length == ENTRY_SIZE * 2;
			}
		};
		
		log("assert that London contains both original and replicated from Israel objects");
		repetitiveAssertTrue("checking replication from Israel to London",condLondon, (int)DEFAULT_TEST_TIMEOUT);
		Assert.assertTrue(gigaSpaceLondon.readMultiple(new Object()).length == ENTRY_SIZE * 2);

		
		log("test done");
	}

	private void upgradeLondon() throws IOException {
		String fileToDeploy = curDir +"/./apps/gateway/hardcoded/DoubleLinkLondonGW/" + folderLayout + "pu.xml";
		SftpUtils.copyFiles(hostLON,getUserName(),password ,fileToDeploy,gwFileToReplace, null);

		fileToDeploy = curDir +"/./apps/gateway/hardcoded/ClusterWith2TargetsLondon/" + folderLayout + "pu.xml";
		SftpUtils.copyFiles(hostLON,getUserName(),password ,fileToDeploy,siteFileToReplace, null);
	}


	private void upgradeNY() throws IOException {
		String fileToDeploy = curDir +"/./apps/gateway/hardcoded/DoubleLinkNYGW/" + folderLayout + "pu.xml";
		SftpUtils.copyFiles(hostNY,getUserName(),password ,fileToDeploy,gwFileToReplace, null);

		fileToDeploy = curDir +"/./apps/gateway/hardcoded/ClusterWith2TargetsNY/" + folderLayout + "pu.xml";
		SftpUtils.copyFiles(hostNY,getUserName(),password ,fileToDeploy,siteFileToReplace, null);

	}

	private void deployIsrael() {
		GridServiceAgent gsaISR = adminISR.getGridServiceAgents().waitForAtLeastOne();

		gsaISR.startGridService(new GridServiceManagerOptions());
		gsaISR.startGridService(new GridServiceContainerOptions());
		gsaISR.startGridService(new GridServiceContainerOptions());

		gsmISR = adminISR.getGridServiceManagers().waitForAtLeastOne();

		log("deploying israel PUs");
		deployIsraelSite();
		deployIsraelGW();


		adminISR.getGridServiceContainers().waitFor(2);

	}

	private void deployIsraelGW() {
		Map<String, String> props = new HashMap<String, String>();
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
		deployGateway(gsmISR, siteDeployment("./apps/gateway/gatewayDoubleLink", "ISRAEL-GW", props));		
	}

	private void deployIsraelSite() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "ISRAEL");
		props.put("gatewayTarget", "LONDON");
		props.put("gatewayTarget2", "NY");
		props.put("spaceUrl", "/./israelSpace");
		deployGateway(gsmISR, siteDeployment("./apps/gateway/clusterWith2targets", "israelSpace", props));	
	}

	private void initialize() throws InterruptedException {
		log("initializing..");
		adminISR = new AdminFactory().addGroups(groupISR).createAdmin();
		adminLON = new AdminFactory().addGroups(groupLON).createAdmin();
		adminNY = new AdminFactory().addGroups(groupNY).createAdmin();
		SetupUtils.assertCleanSetup(adminISR);
		SetupUtils.assertCleanSetup(adminLON);
		SetupUtils.assertCleanSetup(adminNY);

		GridServiceAgent gsaLON = adminLON.getGridServiceAgents().waitForAtLeastOne();
		GridServiceAgent gsaNY = adminNY.getGridServiceAgents().waitForAtLeastOne();

		gsaLON.startGridService(new GridServiceManagerOptions());
		gsaLON.startGridService(new GridServiceContainerOptions());
		gsaLON.startGridService(new GridServiceContainerOptions());

		gsaNY.startGridService(new GridServiceManagerOptions());
		gsaNY.startGridService(new GridServiceContainerOptions());
		gsaNY.startGridService(new GridServiceContainerOptions());

		gsmLON = adminLON.getGridServiceManagers().waitForAtLeastOne();
		gsmNY = adminNY.getGridServiceManagers().waitForAtLeastOne();

		log("deploying PUs");
		deployLondonSite();
		deployNYSite();
		deployLondonGW();
		deployNYGW();

		adminLON.getGridServiceContainers().waitFor(2);
		adminNY.getGridServiceContainers().waitFor(3);

		spaceLON = adminLON.getSpaces().waitFor("londonSpace");
		spaceNY = adminNY.getSpaces().waitFor("NYSpace");

		log("validating gateway components");

		// Verify delegators & sinks are connected.
		assertGatewayReplicationConnected(spaceLON, 1);
		assertGatewayReplicationConnected(spaceNY, 1);


		log("finished initialziation");
	}

	private void deployNYGW() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "NY");
		props.put("targetGatewayName", "LONDON");
		props.put("localClusterUrl", "jini://*/*/NYSpace?groups=" + groupNY);
		props.put("gatewayLookupGroup", UUID.randomUUID().toString());
		props.put("localGatewayHost", hostNY);
		props.put("localGatewayDiscoveryPort", NY_GATEWAY_DISCOVERY_PORT);
		props.put("localGatewayLrmiPort", NY_GATEWAY_LRMI_PORT);
		props.put("targetGatewayHost", hostLON);
		props.put("targetGatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
		props.put("targetGatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
		deployGateway(gsmNY, siteDeployment("./apps/gateway/gateway-components", "NY-GW", props));		
	}

	private void deployLondonGW() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "LONDON");
		props.put("targetGatewayName", "NY");
		props.put("localClusterUrl", "jini://*/*/londonSpace?groups=" + groupLON);
		props.put("gatewayLookupGroup", UUID.randomUUID().toString());
		props.put("localGatewayHost", hostLON);
		props.put("localGatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
		props.put("localGatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
		props.put("targetGatewayHost", hostNY);
		props.put("targetGatewayDiscoveryPort", NY_GATEWAY_DISCOVERY_PORT);
		props.put("targetGatewayLrmiPort", NY_GATEWAY_LRMI_PORT);
		deployGateway(gsmLON, siteDeployment("./apps/gateway/gateway-components", "LONDON-GW", props));		

	}

	private void deployNYSite() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "NY");
		props.put("gatewayTarget", "LONDON");
		props.put("spaceUrl", "/./NYSpace");
		deploySite(gsmNY, siteDeployment("./apps/gateway/cluster", "NYSpace", props));

	}

	private void deployLondonSite() {
		Map<String, String> props = new HashMap<String, String>();
		props.put("localGatewayName", "LONDON");
		props.put("gatewayTarget", "NY");
		props.put("spaceUrl", "/./londonSpace");
		deploySite(gsmLON, siteDeployment("./apps/gateway/cluster", "londonSpace", props));

	}

	private void deployHardcodedNYGW() {
		ProcessingUnitDeployment deployment = new ProcessingUnitDeployment(new File("./apps/gateway/hardcoded/NYGW")).name("NY-GW");
		ProcessingUnit pu = gsmNY.deploy(deployment);
		assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));
	}

	private void deployHardcodedLondonGW() {
		ProcessingUnitDeployment deployment = new ProcessingUnitDeployment(new File("./apps/gateway/hardcoded/LondonGW")).name("LONDON-GW");
		ProcessingUnit pu = gsmLON.deploy(deployment);
		assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));
	}

	private void deployHardcodedNYSite() {
		ProcessingUnitDeployment deployment = new ProcessingUnitDeployment(new File("./apps/gateway/hardcoded/ClusterNY")).name("NYSpace");
		ProcessingUnit pu = gsmNY.deploy(deployment);
		assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));
	}

	private void deployHardcodedLondonSite() {
		ProcessingUnitDeployment deployment = new ProcessingUnitDeployment(new File("./apps/gateway/hardcoded/ClusterLondon")).name("londonSpace");
		ProcessingUnit pu = gsmLON.deploy(deployment);
		assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));
	}

	private void deployHardcodedIsraelGW() {
		ProcessingUnitDeployment deployment = new ProcessingUnitDeployment(new File("./apps/gateway/hardcoded/DoubleLinkIsraelGW")).name("ISRAEL-GW");
		ProcessingUnit pu = gsmISR.deploy(deployment);
		assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));
	}

	private void deployHardcodedIsraelSite() {

		ProcessingUnitDeployment deployment = new ProcessingUnitDeployment(new File("./apps/gateway/hardcoded/ClusterWith2TargetsIsrael")).name("israelSpace");
		ProcessingUnit pu = gsmISR.deploy(deployment);
		assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));
	}

	private void restart(Admin admin, String puName) {
		for (ProcessingUnitInstance pui : admin.getProcessingUnits().getProcessingUnit(puName).getInstances()) {
			pui.restartAndWait();
		}
	}

}