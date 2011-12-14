package test.webui.topology.logicalpanel;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.logicalpanel.LogicalPanel;

public class ProcessingUnitInstanceAddedTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit pu;
	GridServiceManager gsmA;
	
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
		gsmA = AdminUtils.loadGSM(machineA); 
		AdminUtils.loadGSCs(machineA, 2);
		
		LogUtils.log("deploying processing unit...");
		pu = gsmA.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("simpleStatelessPu.jar")).numberOfInstances(2)
			.setContextProperty(APPLICATION_CONTEXT_PROPERY, "App"));
	
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void basicTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		topologyTab.getApplicationMap().selectApplication("App");
		
		final LogicalPanel logicalPanel = topologyTab.getTopologySubPanel().swtichToLogicalPanel();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				for (ProcessingUnitInstance puInstance : pu.getInstances()) {
					if (logicalPanel.getProcessingUnitInstance(puInstance.getProcessingUnitInstanceName()) == null) {
						return false;
					}
				}
				return true;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, 5000);
		
		pu.incrementInstance();
		pu.waitFor(3);
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				for (ProcessingUnitInstance puInstance : pu.getInstances()) {
					if (logicalPanel.getProcessingUnitInstance(puInstance.getProcessingUnitInstanceName()) == null) {
						return false;
					}
				}
				return true;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, 5000);
		
	}

}
