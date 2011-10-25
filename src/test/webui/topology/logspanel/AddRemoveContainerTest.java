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
import test.webui.objects.topology.LogsPanel;
import test.webui.objects.topology.PuLogsPanelService;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.PuLogsPanelService.LogsMachine;

public class AddRemoveContainerTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit space;
	GridServiceContainer[] gscs;
	
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
		GridServiceManager gsmA = loadGSM(machineA); 
		gscs = loadGSCs(machineA, 2);
		
		LogUtils.log("deploying processing unit...");
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", "MyApp");
		space = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(space, DeploymentStatus.INTACT);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void addRemoveContainerTest() throws InterruptedException {
		
		LoginPage loginPage = getLoginPage();
		
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		topologyTab.getApplicationMap().selectApplication("MyApp");
		
		LogsPanel logsPanel = topologyTab.getTopologySubPanel().switchToLogsPanel();
		
		final PuLogsPanelService logsService = logsPanel.getPuLogsPanelService("Test");
		
		LogsMachine logMachineA = logsService.getMachine(machineA.getHostName());
		
		for (GridServiceContainer gsc : admin.getGridServiceContainers().getContainers()) {
			assertTrue(logMachineA.containsGridServiceContainer(gsc));
		}
		
		final GridServiceContainer gscToKill = admin.getGridServiceContainers().getContainers()[0];
		
		gscToKill.kill();
		
		admin.getGridServiceContainers().waitFor(1);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				LogsMachine machine = logsService.getMachine(machineA.getHostName());
				return (!machine.containsGridServiceContainer(gscToKill));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, 5000);
		
		final GridServiceContainer gscNew = AdminUtils.loadGSC(machineA);
		
		admin.getGridServiceContainers().waitFor(2);
		
		condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				LogsMachine machine = logsService.getMachine(machineA.getHostName());
				return (machine.containsGridServiceContainer(gscNew));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
	}

}
