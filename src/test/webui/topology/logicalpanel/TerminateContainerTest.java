package test.webui.topology.logicalpanel;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.cluster.activeelection.SpaceMode;

import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.dashboard.ServicesGrid.Icon;
import test.webui.objects.topology.LogicalPanel;
import test.webui.objects.topology.LogicalPanel.WebUIProcessingUnitInstance;
import test.webui.objects.topology.TopologyTab;

public class TerminateContainerTest extends AbstractSeleniumTest {
	
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
		Pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(Pu, DeploymentStatus.INTACT);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void basicTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		topologyTab.getApplicationMap().selectApplication("MyApp");
		
		final LogicalPanel logicalPanel = topologyTab.getTopologySubPanel().swtichToLogicalPanel();
		
		for (ProcessingUnitInstance puInstance : Pu.getInstances()) {
			assertTrue(logicalPanel.getProcessingUnitInstance(puInstance.getProcessingUnitInstanceName()) != null);
		}
		
		ProcessingUnitInstance instanceTokill = Pu.getInstances()[0];
		ProcessingUnitInstance instance = Pu.getInstances()[1];
		final String instanceToKillName = instanceTokill.getProcessingUnitInstanceName();
		final String instanceName = instance.getProcessingUnitInstanceName();
		instanceTokill.getGridServiceContainer().kill();
		admin.getGridServiceContainers().waitFor(1);
		Pu.waitFor(1);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return (logicalPanel.getProcessingUnitInstance(instanceToKillName) == null);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, 5000);
		
		
		Pu.getInstances()[0].getSpaceInstance().waitForMode(SpaceMode.PRIMARY, 30, TimeUnit.SECONDS);
		
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				WebUIProcessingUnitInstance processingUnitInstance = logicalPanel.getProcessingUnitInstance(instanceName);
				return ((processingUnitInstance != null)
					&& (processingUnitInstance.getIcon().equals(Icon.ALERT)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, 5000);
		
		
	}

}
