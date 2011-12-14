package test.webui.topology.detailspanel;

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
import test.webui.objects.topology.physicalpanel.HostData;
import test.webui.objects.topology.physicalpanel.PhysicalPanel;
import test.webui.objects.topology.sidepanel.ComparisonCharts;
import framework.utils.AdminUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

/**
 * Sanity check for comparison charts
 * 1. Select a certain hosts comparison chart checkbox
 * 2. verify the Metrics (both top and bottom) now contain 1 graph)
 * @author elip
 *
 */
public class BasicComparisonChartsTest extends AbstractSeleniumTest {
	
	
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
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty(APPLICATION_CONTEXT_PROPERY, "MyApp");
		Pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(Pu, DeploymentStatus.INTACT);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void basicDetailsTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		applicationMap.selectApplication("MyApp");
		
		PhysicalPanel physicalPanel = topologyTab.getTopologySubPanel().switchToPhysicalPanel();
		HostData me = physicalPanel.getHostData(machineA.getHostName());
		me.selectComparisonCharts();
		ComparisonCharts copmCharts = topologyTab.getDetailsPanel().switchToComparisonCharts();
		assertTrue("Graph for host " + machineA.getHostName() + "was not found in bottom metric ", copmCharts.getBottomMetric().getNumberOfGraphs() == 1);
		assertTrue("Graph for host " + machineA.getHostName() + "was not found in top metric ", copmCharts.getTopMetric().getNumberOfGraphs() == 1);
		
	}

}
