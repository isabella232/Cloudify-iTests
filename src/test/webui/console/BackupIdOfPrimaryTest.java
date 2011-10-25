package test.webui.console;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.util.List;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.cluster.activeelection.SpaceMode;

import test.utils.ProcessingUnitUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.console.ConsoleTab;
import test.webui.objects.console.SpaceInstancesGrid;
import test.webui.objects.console.SpaceInstancesGrid.SpaceInstance;
import test.webui.objects.console.SpaceTreeSidePanel;
import test.webui.objects.console.SpaceTreeSidePanel.SpaceTreeNode;

public class BackupIdOfPrimaryTest extends AbstractSeleniumTest {
	
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
		SpaceDeployment deployment = new SpaceDeployment("Test1").partitioned(1, 1).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void backupIdTest() throws InterruptedException {
		
		LoginPage loginPage = getLoginPage();
		
		ConsoleTab consoleTab = loginPage.login().switchToConsole();
		
		SpaceTreeSidePanel sidePanel = consoleTab.getSpaceTreeSidePanel();
		
		SpaceTreeNode spaceNode = sidePanel.getSpaceTreeNode("Test1");
		
		assertTrue(spaceNode != null);
		
		spaceNode.select();
		
		SpaceInstance primary = null;
		SpaceInstance backup = null;
		
		SpaceInstancesGrid spaceInstancesGrid = consoleTab.getInstancesGrid();
		
		List<SpaceInstance> instances = spaceInstancesGrid.getSpaceInstances();
		
		for (SpaceInstance s : instances) {
			if (s.getSpaceType().equals(SpaceMode.BACKUP)) backup = s;
			if (s.getSpaceType().equals(SpaceMode.PRIMARY)) primary = s;
		}
		
		assertTrue(primary.getBackupId() == backup.getID());
		
		
	}

}
