package test.webui.console;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.AssertUtils;
import test.utils.ProcessingUnitUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.console.ConsoleTab;
import test.webui.objects.console.SpaceTreeSidePanel;
import test.webui.objects.console.SpaceTreeSidePanel.SpaceTreeNode;

public class SpaceAdditionAutoRefreshTest extends AbstractSeleniumTest {

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
		
		// deploy a pu
		SpaceDeployment deployment = new SpaceDeployment("Test1").partitioned(1, 0).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void spaceAdditionTest() throws InterruptedException {
		
		LoginPage loginPage = getLoginPage();
		
		ConsoleTab consoleTab = loginPage.login().switchToConsole();
		
		final SpaceTreeSidePanel sidePanel = consoleTab.getSpaceTreeSidePanel();
		
		SpaceTreeNode spaceNode = sidePanel.getSpaceTreeNode("Test1");
		
		assertTrue(spaceNode != null);
		
		// deploy a pu
		SpaceDeployment deployment = new SpaceDeployment("Test2").partitioned(1, 0).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				SpaceTreeNode spaceNode = sidePanel.getSpaceTreeNode("Test2");
				return (spaceNode != null);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, 5000);
		
		
	}
	
	
	
}
