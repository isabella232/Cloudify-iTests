package test.gateway;

import static test.utils.LogUtils.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import test.utils.ScriptUtils;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;


/**
 * Instructions:
 * Test uses your local machine as host for all sites.
 * Use the "start_agents.bat/sh" script found in this test folder to load the agents with groups for each site.
 * 
 * @author sagib
 * @since 8.0.4
 */
public class GatewayRollingUpgradeLocalTest extends AbstractGatewayTest {

	private Admin adminISR;
	private Admin adminLON;
	private Admin adminNY;

	private String groupISR = null;
	private String groupLON = null;
	private String groupNY = null;
	private GridServiceManager gsmNY;
	private GridServiceManager gsmISR;
	private GridServiceManager gsmLON;
	
	private final String s = "/";

	private final String folderLayout = "META-INF/spring" + s;
	private final String LonGwFileToReplace = ScriptUtils.getBuildPath() + s + "deploy/LondonGW" + s + folderLayout + "pu.xml";
	private final String LonSiteFileToReplace = ScriptUtils.getBuildPath() + s + "deploy/ClusterLondon" + s + folderLayout + "pu.xml";
	private final String NYGwFileToReplace = ScriptUtils.getBuildPath() + s + "deploy/NYGW" + s + folderLayout + "pu.xml";
	private final String NYSiteFileToReplace = ScriptUtils.getBuildPath() + s + "deploy/ClusterNY" + s + folderLayout + "pu.xml";
	private final String curDir = System.getProperty("user.dir");
	private Space spaceLON;
	private Space spaceNY;
	private Space spaceISR;

	public GatewayRollingUpgradeLocalTest() {
		groupISR = "israel-group";
		groupLON = "london-group";
		groupNY = "ny-group";
	}

	@AfterMethod
	public void tearDown() {
		Admin[] admins = {adminLON, adminNY, adminISR};
		TeardownUtils.teardownAll(admins);
		adminLON.close();
		adminNY.close();
		adminISR.close();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="3", enabled=false)
	public void test() throws Exception {
		initialize();

		deployIsrael();

		spaceLON = adminLON.getSpaces().waitFor("londonSpace");
		spaceNY = adminNY.getSpaces().waitFor("NYSpace");
		spaceISR = adminISR.getSpaces().waitFor("israelSpace");

		upgradeNY();
		restart(adminNY,"NY-GW");
		restart(adminNY,"NYSpace");

		upgradeLondon();
		restart(adminLON, "London-GW");
		restart(adminLON, "londonSpace");

		assertGatewayReplicationConnected(spaceLON, 2);
		assertGatewayReplicationConnected(spaceNY, 2);
		assertGatewayReplicationConnected(spaceISR, 2);

		log("test done");
	}

	private void upgradeLondon() throws Exception {
		String fileToDeploy = curDir +"/./apps/gateway/hardcoded/DoubleLinkLondonGW/" + folderLayout + "pu.xml";
		copyfile(fileToDeploy,LonGwFileToReplace);
		
		fileToDeploy = curDir +"/./apps/gateway/hardcoded/ClusterWith2TargetsLondon/" + folderLayout + "pu.xml";
		copyfile(fileToDeploy,LonSiteFileToReplace);
	}




	private void upgradeNY() throws Exception {
		String fileToDeploy = curDir +"/./apps/gateway/hardcoded/DoubleLinkNYGW/" + folderLayout + "pu.xml";
		copyfile(fileToDeploy,NYGwFileToReplace);
		
		fileToDeploy = curDir +"/./apps/gateway/hardcoded/ClusterWith2TargetsNY/" + folderLayout + "pu.xml";
		copyfile(fileToDeploy,NYSiteFileToReplace);
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
		ProcessingUnitDeployment deployment = new ProcessingUnitDeployment(new File("./apps/gateway/hardcoded/DoubleLinkIsraelGW")).name("ISRAEL-GW");
		ProcessingUnit pu = gsmISR.deploy(deployment);
		assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));
	}

	private void deployIsraelSite() {

		ProcessingUnitDeployment deployment = new ProcessingUnitDeployment(new File("./apps/gateway/hardcoded/ClusterWith2TargetsIsrael")).name("israelSpace");
		ProcessingUnit pu = gsmISR.deploy(deployment);
		assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));
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

		// Verify delegators are connected.
		assertGatewayReplicationConnected(spaceLON, 1);
		assertGatewayReplicationConnected(spaceNY, 1);


		log("finished initialziation");
	}

	private void deployNYGW() {
		ProcessingUnitDeployment deployment = new ProcessingUnitDeployment(new File("./apps/gateway/hardcoded/NYGW")).name("NY-GW");
		ProcessingUnit pu = gsmNY.deploy(deployment);
		assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));
	}

	private void deployLondonGW() {
		ProcessingUnitDeployment deployment = new ProcessingUnitDeployment(new File("./apps/gateway/hardcoded/LondonGW")).name("London-GW");
		ProcessingUnit pu = gsmLON.deploy(deployment);
		assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));
	}

	private void deployNYSite() {
		ProcessingUnitDeployment deployment = new ProcessingUnitDeployment(new File("./apps/gateway/hardcoded/ClusterNY")).name("NYSpace");
		ProcessingUnit pu = gsmNY.deploy(deployment);
		assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));
	}

	private void deployLondonSite() {
		ProcessingUnitDeployment deployment = new ProcessingUnitDeployment(new File("./apps/gateway/hardcoded/ClusterLondon")).name("londonSpace");
		ProcessingUnit pu = gsmLON.deploy(deployment);
		assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));
	}

	private void restart(Admin admin, String puName) {
		for (ProcessingUnitInstance pui : admin.getProcessingUnits().getProcessingUnit(puName).getInstances()) {
			pui.restartAndWait();
		}
	}

	private void copyfile(String fileToDeploy, String fileToReplace) throws Exception {
		File f1 = new File(fileToDeploy);
		File f2 = new File(fileToReplace);
		InputStream in = new FileInputStream(f1);
		OutputStream out = new FileOutputStream(f2);
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0){
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}
	
}