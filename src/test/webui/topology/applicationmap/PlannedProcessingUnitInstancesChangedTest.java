package test.webui.topology.applicationmap;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.LoginPage;

public class PlannedProcessingUnitInstancesChangedTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit pu;
	
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
		pu = gsmA.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("simpleStatelessPu.jar")));
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void plannedInstancesTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		ApplicationNode testNode = applicationMap.getApplicationNode("simpleStatelessPu");
		
		final int plannedInstances = pu.getTotalNumberOfInstances();
		assertTrue(testNode.getPlannedInstances() == plannedInstances);

		pu.incrementInstance();
		pu.waitFor(plannedInstances + 1);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				ApplicationNode testNode = applicationMap.getApplicationNode("simpleStatelessPu");
				return (testNode.getPlannedInstances() == (plannedInstances + 1));
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,10000);
		
	}

}
