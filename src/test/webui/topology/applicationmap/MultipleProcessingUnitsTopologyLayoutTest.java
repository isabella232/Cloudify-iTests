package test.webui.topology.applicationmap;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.services.PuTreeGrid.WebUIProcessingUnit;
import test.webui.objects.services.PuTreeGrid;
import test.webui.objects.services.ServicesTab;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.HealthPanel;
import test.webui.objects.topology.TopologyTab;

public class MultipleProcessingUnitsTopologyLayoutTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit Pu;
	GridServiceManager gsmA;
	
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
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void multiplePus() throws InterruptedException {
		
		int waitingTime = 10000;
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		ServicesTab servicesTab = loginPage.login().switchToServices();
		
		//deploy two processing units
		LogUtils.log("deploying processing unit...");
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 0).maxInstancesPerVM(1);
		ProcessingUnit test = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(test, DeploymentStatus.INTACT);
		
		final PuTreeGrid puGrid = servicesTab.getPuTreeGrid();
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				WebUIProcessingUnit testPu = null;
				testPu = puGrid.getProcessingUnit("Test");
				return ((testPu.getStatus() != null) 
						&& (testPu.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		TopologyTab topologyTab = servicesTab.switchToTopology();
		
		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();
		
		final ApplicationMap map = topologyTab.getApplicationMap();
		
		map.selectApplication("Unassigned Services");
		
		ApplicationNode testNode = map.getApplicationNode("Test");
		
		// check the correct metrics are shown
		assertTrue(healthPanel.getMetric("CPU").isDisplayed());
		assertTrue(healthPanel.getMetric("Memory").isDisplayed());
		assertTrue(healthPanel.getMetric("GC").isDisplayed());
		/*
		assertTrue(healthPanel.getMetric("Space Write throughput").isDisplayed());
		assertTrue(healthPanel.getMetric("Space Read throughput").isDisplayed());
		assertTrue(healthPanel.getMetric("Replication(bytes/sec)").isDisplayed());
		*/
		
		LogUtils.log("deploying second processing unit...");
		deployment = new SpaceDeployment("Test2").partitioned(1, 0).maxInstancesPerVM(1);
		ProcessingUnit test2 = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(test2, DeploymentStatus.INTACT);
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return ((map.getApplicationNode("Test2") != null)
						&& (map.getApplicationNode("Test2").getStatus() != null)
						&& (map.getApplicationNode("Test2").getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		

		LogUtils.log("undeploying first processing unit...");
		test.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(test, DeploymentStatus.UNDEPLOYED);
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (map.getApplicationNode("Test") == null);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		ApplicationNode testNode2 = map.getApplicationNode("Test2");
		
		// check the correct metrics are shown
		assertTrue(healthPanel.getMetric("CPU").isDisplayed());
		assertTrue(healthPanel.getMetric("Memory").isDisplayed());
		assertTrue(healthPanel.getMetric("GC").isDisplayed());
		assertTrue(healthPanel.getMetric("Space Write throughput").isDisplayed());
		assertTrue(healthPanel.getMetric("Space Read throughput").isDisplayed());
		
		servicesTab = topologyTab.switchToServices();
		test2.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(test2, DeploymentStatus.UNDEPLOYED);
		
		topologyTab = servicesTab.switchToTopology();
		
		assertTrue(map.getApplicationNode("Test2") == null);
		
	}

}
