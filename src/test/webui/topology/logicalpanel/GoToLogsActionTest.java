package test.webui.topology.logicalpanel;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AdminUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.LogicalPanel;
import test.webui.objects.topology.LogicalPanel.WebUIProcessingUnitInstance;
import test.webui.objects.topology.LogsPanel;
import test.webui.objects.topology.TopologyTab;

public class GoToLogsActionTest extends AbstractSeleniumTest {
	
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
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", "MyApp");
		Pu = gsmA.deploy(deployment.setContextProperty(APPLICATION_CONTEXT_PROPERY, "App"));
		ProcessingUnitUtils.waitForDeploymentStatus(Pu, DeploymentStatus.INTACT);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void goTologsTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		topologyTab.getApplicationMap().selectApplication("App");
		
		LogicalPanel logicalPanel = topologyTab.getTopologySubPanel().swtichToLogicalPanel();
		
		ProcessingUnitInstance instance = Pu.getInstances()[0];
		String processingUnitInstanceName = instance.getProcessingUnitInstanceName();
		
		WebUIProcessingUnitInstance testInstance = logicalPanel.getProcessingUnitInstance(processingUnitInstanceName);
		
		testInstance.goToLogs();
		
		LogsPanel logsPanel = topologyTab.getTopologySubPanel().switchToLogsPanel();
		
		assertTrue(logsPanel.getCurrentLog().equals(processingUnitInstanceName));
	}

}
