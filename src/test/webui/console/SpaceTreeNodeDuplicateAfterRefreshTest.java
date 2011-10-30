package test.webui.console;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.ProcessingUnitUtils;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.console.ConsoleTab;
import test.webui.objects.console.SpaceTreeSidePanel;
import test.webui.objects.console.SpaceTreeSidePanel.SpaceTreeNode;
import test.webui.objects.dashboard.DashboardTab;

public class SpaceTreeNodeDuplicateAfterRefreshTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit pu;
	GridServiceManager gsmA;
	
	@BeforeMethod(alwaysRun = true)
	public void startSetup() {
		log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		machineA = gsaA.getMachine();

		log("starting: 1 GSM and 2 GSC's on 1 machine");
		gsmA = loadGSM(machineA); 
		loadGSCs(machineA, 2);

	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void duplicateTest() throws InterruptedException {
		
		LoginPage loginPage = getLoginPage();
		
		DashboardTab dashboardTab = loginPage.login().switchToDashboard();
		
		// deploy a pu
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 0).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
		ConsoleTab consoleTab = dashboardTab.switchToConsole();
		
		SpaceTreeSidePanel sidePanel = consoleTab.getSpaceTreeSidePanel();
		
		SpaceTreeNode spaceNode = sidePanel.getSpaceTreeNode(pu.getName());
		
		assertTrue(spaceNode != null);
		
		assertTrue(spaceNode.isExactlyOne());
		
		consoleTab = refreshPage().switchToConsole();
		
		spaceNode = sidePanel.getSpaceTreeNode(pu.getName());
		
		assertTrue(spaceNode != null);
		
		assertTrue(spaceNode.isExactlyOne());
	}

}
