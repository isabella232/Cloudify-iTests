package test.webui.topology.logspanel;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.LogsPanel;
import test.webui.objects.topology.PuLogsPanelService;
import test.webui.objects.topology.PuLogsPanelService.LogsMachine;
import test.webui.objects.topology.PuLogsPanelService.LogsMachine.LogsGridServiceContainer;
import test.webui.objects.topology.TopologyTab;

public class PuInstanceInContainerTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit space;
	GridServiceContainer[] gscs;
	
	@BeforeMethod(alwaysRun = true)
	public void startSetup() throws Exception {
		log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		machineA = gsaA.getMachine();

		log("starting: 1 GSM and 2 GSC's on 1 machine");
		GridServiceManager gsmA = loadGSM(machineA); 
		gscs = loadGSCs(machineA, 2);
		
		LogUtils.log("deploying processing unit...");
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 1).maxInstancesPerVM(1);
		space = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(space, DeploymentStatus.INTACT);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void applicationNameTest() throws InterruptedException {
		
		LoginPage loginPage = getLoginPage();
		
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		LogsPanel logsPanel = topologyTab.getTopologySubPanel().switchToLogsPanel();
		
		PuLogsPanelService logsService = logsPanel.getPuLogsPanelService("Test");
		
		LogsMachine logsMachine = logsService.getMachine(machineA.getHostName());
		
		ProcessingUnit testPu = admin.getProcessingUnits().getProcessingUnit("Test");
		
		ProcessingUnitInstance instance1 = testPu.getInstances()[0];
		GridServiceContainer contianer1 = instance1.getGridServiceContainer();
		
		ProcessingUnitInstance instance2 = testPu.getInstances()[1];
		GridServiceContainer contianer2 = instance2.getGridServiceContainer();
		
		LogsGridServiceContainer logsContainer1 = logsMachine.getContianer(contianer1);
		assertTrue(logsContainer1 != null);
		assertTrue(logsContainer1.getPuInstances().contains(instance1.getProcessingUnitInstanceName()));
		
		LogsGridServiceContainer logsContainer2 = logsMachine.getContianer(contianer2);
		assertTrue(logsContainer2 != null);
		assertTrue(logsContainer2.getPuInstances().contains(instance2.getProcessingUnitInstanceName()));
		
	}

}
