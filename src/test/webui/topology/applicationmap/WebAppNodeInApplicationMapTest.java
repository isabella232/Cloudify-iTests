package test.webui.topology.applicationmap;

import java.util.List;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.applicationmap.ApplicationMap;
import test.webui.objects.topology.applicationmap.ApplicationNode;
import test.webui.objects.topology.healthpanel.HealthPanel;
import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

public class WebAppNodeInApplicationMapTest extends AbstractSeleniumTest {

	Machine machineA;
	ProcessingUnit puSessionTest;
	
	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {	
		LogUtils.log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		LogUtils.log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		machineA = gsaA.getMachine();

		LogUtils.log("starting: 1 GSM and 1 GSC's on 1 machine");
		GridServiceManager gsmA = AdminUtils.loadGSM(machineA); 
		AdminUtils.loadGSCs(machineA, 1);
		
		LogUtils.log("deploying processing unit...");
		puSessionTest = gsmA.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("session-test-embedded.war")));
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void webAppNodeTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		applicationMap.selectApplication("Unassigned Services");
		
		ApplicationNode testNode = applicationMap.getApplicationNode("session-test-embedded");
		
		assertNotNull(testNode);
		assertTrue(testNode.getPlannedInstances() == 1);
		assertTrue(testNode.getPuType().equals("WEB_APP"));
		assertTrue(testNode.getNodeType().equals("PROCESSING_UNIT"));
		List<String> components = testNode.getComponents();
		assertTrue(components.contains("web"));
		assertTrue(testNode.getxPosition() == 0);
		
		ProcessingUnitUtils.waitForDeploymentStatus(puSessionTest, DeploymentStatus.INTACT);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				ApplicationNode testNode = applicationMap.getApplicationNode("session-test-embedded");
				return ((testNode.getStatus().equals(DeploymentStatus.INTACT))
						&& (testNode.getActualInstances() == 1));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, 5000);
		
		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();
		
		// check the correct metrics are shown
		assertTrue(healthPanel.getMetric("CPU") != null);
		assertTrue(healthPanel.getMetric("Memory") != null);
		assertTrue(healthPanel.getMetric("GC") != null);
		assertTrue(healthPanel.getMetric("HTTP throughput") != null);
	}
}
