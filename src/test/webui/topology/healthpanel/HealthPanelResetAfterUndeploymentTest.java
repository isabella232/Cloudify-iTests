package test.webui.topology.healthpanel;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
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
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

public class HealthPanelResetAfterUndeploymentTest extends AbstractSeleniumTest {

	Machine machineA;
	ProcessingUnit pu;
	
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
		GridServiceManager gsmA = AdminUtils.loadGSM(machineA); 
		AdminUtils.loadGSCs(machineA, 2);
		
		LogUtils.log("deploying processing unit...");
		SpaceDeployment deployment = new SpaceDeployment("Test").numberOfInstances(2).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void memoryMetricTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap appMap = topologyTab.getApplicationMap();
		
		ApplicationNode testNode = appMap.getApplicationNode("Test");
		
		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();
		
		// check the correct metrics are shown
		assertTrue(healthPanel.getMetric("CPU") != null);
		assertTrue(healthPanel.getMetric("Memory") != null);
		assertTrue(healthPanel.getMetric("GC") != null);
		assertTrue(healthPanel.getMetric("Space Write throughput") != null);
		assertTrue(healthPanel.getMetric("Space Read throughput") != null);
		
		topologyTab.getTopologySubPanel().switchToPhysicalPanel();
		
		pu.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.UNDEPLOYED);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				ApplicationNode node = appMap.getApplicationNode("Test");		
				return node == null;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, 5000);
		
		healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();
		
		// check the correct metrics are shown
		assertTrue(healthPanel.getMetric("CPU") == null);
		assertTrue(healthPanel.getMetric("Memory") == null);
		assertTrue(healthPanel.getMetric("GC") == null);
		assertTrue(healthPanel.getMetric("Space Write throughput") == null);
		assertTrue(healthPanel.getMetric("Space Read throughput") == null);
		
	}
	
}
