package test.webui.topology.applicationmap;

import static framework.utils.AdminUtils.loadGSC;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import java.util.List;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AssertUtils;
import framework.utils.DBUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.ToStringUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.HealthPanel;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.LoginPage;

public class MirrorNodeInApplicationMapTest extends AbstractSeleniumTest {

	private static final int HSQL_DB_PORT = 9876;
	private Machine machine;
	private int hsqlId;
	private GridServiceAgent gsa;
	private ProcessingUnit mirror;
	private String application = "App";
	
	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {
		
		log("waiting for 1 GSA");
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading GSM");
		GridServiceManager gsm = loadGSM(machine);
		
		log("loading 1 GSC on 1 machine");
		loadGSC(machine);
		
		
        log("load HSQL DB on machine - "+ToStringUtils.machineToString(machine));
        hsqlId = DBUtils.loadHSQLDB(machine, "MirrorPersistFailureAlertTest", HSQL_DB_PORT);
        LogUtils.log("Loaded HSQL successfully, id ["+hsqlId+"]");
        
        log("deploy mirror via GSM");
        DeploymentUtils.prepareApp("MHEDS");
		mirror = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "mirror")).
                setContextProperty("port", String.valueOf(HSQL_DB_PORT)).setContextProperty("host", machine.getHostAddress()).
                setContextProperty("com.gs.application", application));
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void mirrorTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		applicationMap.selectApplication(application);
		
		ApplicationNode testNode = applicationMap.getApplicationNode("mirror");
		
		// check node is displayed with the correct info
		assertNotNull(testNode);
		assertTrue(testNode.getPlannedInstances() == 1);
		assertTrue(testNode.getPuType().equals("MIRROR"));
		assertTrue(testNode.getNodeType().equals("PROCESSING_UNIT"));
		List<String> components = testNode.getComponents();
		assertTrue(components.size() == 0);
		assertTrue(testNode.getxPosition() == 3);
		
		ProcessingUnitUtils.waitForDeploymentStatus(mirror, DeploymentStatus.INTACT);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				ApplicationNode testNode = applicationMap.getApplicationNode("mirror");
				return ((testNode.getStatus().equals(DeploymentStatus.INTACT))
						&& (testNode.getActualInstances() == 1));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, 5000);
		
		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();
		
		// check the correct metrics are shown
		assertTrue(healthPanel.getMetric("CPU") != null);
		assertTrue(healthPanel.getMetric("Memory") != null);
		assertTrue(healthPanel.getMetric("Failed operations count") != null);
		assertTrue(healthPanel.getMetric("Mirror Write throughput") != null);
		assertTrue(healthPanel.getMetric("Mirror Update throughput") != null);
		assertTrue(healthPanel.getMetric("Mirror Remove throughput") != null);
	}
}
