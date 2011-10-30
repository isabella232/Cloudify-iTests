package test.webui.topology.applicationmap;

import java.net.MalformedURLException;
import java.util.List;

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
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.HealthPanel;
import test.webui.objects.topology.TopologyTab;

public class StatefullNodeInApplicationMapTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit Pu;
	
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
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty(APPLICATION_CONTEXT_PROPERY, "App");
		Pu = gsmA.deploy(deployment);
//		ProcessingUnitUtils.waitForDeploymentStatus(Pu, DeploymentStatus.INTACT);
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void basicApplicationMapTest() throws InterruptedException, MalformedURLException {
	
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		applicationMap.selectApplication("App");
		
		ApplicationNode testNode = applicationMap.getApplicationNode("Test");
		
		/* assert node was initially loaded correctly */
		assertNotNull(testNode);
		assertTrue(testNode.getPlannedInstances() == 2);
		assertTrue(testNode.getPuType().equals("STATEFUL"));
		assertTrue(testNode.getNodeType().equals("PROCESSING_UNIT"));
		List<String> components = testNode.getComponents();
		assertTrue(components.contains("processing"));
		assertTrue(components.contains("partition-ha"));
		assertTrue(components.size() == 2);
		assertTrue(testNode.getxPosition() == 2);
		
		ProcessingUnitUtils.waitForDeploymentStatus(Pu, DeploymentStatus.INTACT);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				ApplicationNode testNode = applicationMap.getApplicationNode("Test");
				return ((testNode.getStatus().equals(DeploymentStatus.INTACT))
						&& (testNode.getActualInstances() == 2));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, 5000);
		
		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();
		
		// check the correct metrics are shown
		assertTrue(healthPanel.getAssociatedPuName().equals(testNode.getName()));
		assertTrue(healthPanel.getMetric("CPU") != null);
		assertTrue(healthPanel.getMetric("Memory") != null);
		assertTrue(healthPanel.getMetric("GC") != null);
		assertTrue(healthPanel.getMetric("Space Write throughput") != null);
		assertTrue(healthPanel.getMetric("Space Read throughput") != null);

	}

}
