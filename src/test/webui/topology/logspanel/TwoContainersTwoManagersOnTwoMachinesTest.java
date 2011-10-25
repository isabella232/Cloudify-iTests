package test.webui.topology.logspanel;

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

import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.LogsPanel;
import test.webui.objects.topology.PuLogsPanelService;
import test.webui.objects.topology.PuLogsPanelService.LogsMachine;
import test.webui.objects.topology.TopologyTab;

/**
 * deploy a processing unit on two machines
 * one instance per machine
 * assert that logs view is showing both gsc's
 * @author elip
 *
 */
public class TwoContainersTwoManagersOnTwoMachinesTest extends AbstractSeleniumTest {

	Machine machineA;
	Machine machineB;
	
	ProcessingUnit space;
	
	GridServiceManager gsmA;
	GridServiceManager gsmB;
	
	@BeforeMethod(alwaysRun = true)
	public void startSetup() {
		log("waiting for 2 machine");
		admin.getMachines().waitFor(2);

		log("waiting for 2 GSA");
		admin.getGridServiceAgents().waitFor(2);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];
		GridServiceAgent gsaB = agents[1];

		machineA = gsaA.getMachine();
		machineB = gsaB.getMachine();

		log("starting: 1 GSM and 2 GSC's on 1 machine");
		gsmA = loadGSM(machineA); 
		gsmB = loadGSM(machineB); 
		loadGSCs(machineA, 1);
		loadGSCs(machineB, 1);
		
		LogUtils.log("deploying processing unit...");
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", "MyApp");
		space = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(space, DeploymentStatus.INTACT);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void twoContainersTest() throws InterruptedException {
		
		LoginPage loginPage = getLoginPage();
		
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		topologyTab.getApplicationMap().selectApplication("MyApp");
		
		LogsPanel logsPanel = topologyTab.getTopologySubPanel().switchToLogsPanel();
		
		PuLogsPanelService logsService = logsPanel.getPuLogsPanelService("Test");
		
		LogsMachine logMachineA = logsService.getMachine(machineA.getHostName());
		
		LogsMachine logMachineB = logsService.getMachine(machineB.getHostName());
		
		assertTrue(logMachineA != null);
		assertTrue(logMachineB != null);
		
		assertTrue(logMachineA.containsGridServiceContainer(machineA.getGridServiceContainers().getContainers()[0]));
		assertTrue(logMachineB.containsGridServiceContainer(machineB.getGridServiceContainers().getContainers()[0]));
		assertTrue(logMachineA.containsGridServiceManager(gsmA));
		assertTrue(logMachineB.containsGridServiceManager(gsmB));
		
		
	}
}
