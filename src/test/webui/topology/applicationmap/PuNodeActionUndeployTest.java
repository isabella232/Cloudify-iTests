package test.webui.topology.applicationmap;

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

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.applicationmap.ApplicationMap;
import test.webui.objects.topology.applicationmap.ApplicationNode;
import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.ProcessingUnitUtils;

public class PuNodeActionUndeployTest extends AbstractSeleniumTest {
	
	private Machine machineA;
	private ProcessingUnit pu;

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
		AdminUtils.loadGSCs(machineA, 2);
		
		log("deploying processing unit...");
		SpaceDeployment deployment = new SpaceDeployment("toolbox-test").partitioned(1, 0).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void undeployNode() throws Exception {
		
		LoginPage loginPage = getLoginPage();
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		final ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				return (applicationMap.getApplicationNode("toolbox-test") != null);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		ApplicationNode testNode = applicationMap.getApplicationNode("toolbox-test");
		
		testNode.undeploy();
		
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.UNDEPLOYED);
		
		condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				ApplicationNode pu = applicationMap.getApplicationNode("toolbox-test");
				return (pu == null);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, 5000);

	}
	

}
