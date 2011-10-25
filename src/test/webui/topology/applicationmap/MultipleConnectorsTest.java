package test.webui.topology.applicationmap;

import static test.utils.LogUtils.log;

import java.util.List;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.AdminUtils;
import test.utils.DBUtils;
import test.utils.DeploymentUtils;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;
import test.utils.ToStringUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.ApplicationMap.ApplicationNode.Connector;
import test.webui.objects.topology.TopologyTab;

public class MultipleConnectorsTest extends AbstractSeleniumTest {

	private static final int HSQL_DB_PORT = 9876;
	private Machine machineA;
	private int hsqlId;
	private ProcessingUnit mirror;
	private String application = "App";
	private GridServiceManager gsmA;
	private ProcessingUnit pu;

	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {	
		LogUtils.log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		LogUtils.log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		machineA = gsaA.getMachine();

		LogUtils.log("starting: 1 GSM and 2 GSC's on 1 machine");
		gsmA = AdminUtils.loadGSM(machineA); 
		AdminUtils.loadGSCs(machineA, 2);

		log("load HSQL DB on machine - "+ToStringUtils.machineToString(machineA));
		hsqlId = DBUtils.loadHSQLDB(machineA, "MirrorPersistFailureAlertTest", HSQL_DB_PORT);
		LogUtils.log("Loaded HSQL successfully, id ["+hsqlId+"]");

		log("deploy mirror via GSM");
		DeploymentUtils.prepareApp("MHEDS");
		mirror = gsmA.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "mirror")).
				setContextProperty("port", String.valueOf(HSQL_DB_PORT)).setContextProperty("host", machineA.getHostAddress()).
				setContextProperty("com.gs.application", application));

		for (int i = 0 ; i < 3 ; i++) {
			LogUtils.log("deploying processing unit...");
			SpaceDeployment deployment = new SpaceDeployment("Test" + (i + 1)).partitioned(1, 0).maxInstancesPerVM(1)
			.setContextProperty(DEPENDS_ON_CONTEXT_PROPERTY, "[mirror]").setContextProperty(APPLICATION_CONTEXT_PROPERY, "App");
			pu = gsmA.deploy(deployment);
			ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		}
		LogUtils.log("deploying processing unit...");
		pu = gsmA.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("simpleStatelessPu.jar"))
		.setContextProperty(DEPENDS_ON_CONTEXT_PROPERTY, "[Test1,Test2]").setContextProperty(APPLICATION_CONTEXT_PROPERY, "App"));
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void multipleConnectoresTest() throws InterruptedException {

		// get new login page
		LoginPage loginPage = getLoginPage();

		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();

		final ApplicationMap applicationMap = topologyTab.getApplicationMap();

		applicationMap.selectApplication("App");

		ApplicationNode mirrorNode = applicationMap.getApplicationNode(mirror.getName());

		List<Connector> connectors = mirrorNode.getConnectors();

		assertTrue(connectors.size() == 4);

		for (Connector con : connectors) {
			String source = con.getSource().getName();
			if (source.contains("Test")) {
				assertTrue(con.getTarget().getName().equals(mirrorNode.getName()));
			}
		}

		ApplicationNode stateless = applicationMap.getApplicationNode("simpleStatelessPu");

		List<Connector> statelessCon = stateless.getConnectors();

		assertTrue(statelessCon.size() == 2);

		for (Connector con : statelessCon) {
			assertTrue(con.getTarget().getName().equals("Test1") || con.getTarget().getName().equals("Test2"));


		}
	}
}

