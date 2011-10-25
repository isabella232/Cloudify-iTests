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

import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.TopologyTab;

public class DeployAppWhileFocusOnDifferentAppTest extends AbstractSeleniumTest {

	private GridServiceAgent gsa;
	ProcessingUnit loader;
	ProcessingUnit runtime;
	ProcessingUnit puSessionTest;
	ProcessingUnit mySpacePu;
	ProcessingUnit test;
	GridServiceManager gsmA;
	private Machine machine;
	
	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {
		
		log("waiting for 1 GSA");
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading GSM");
		gsmA = loadGSM(machine);
		
		log("loading 1 GSC on 1 machine");
		AdminUtils.loadGSCs(machine, 2);
		
		LogUtils.log("deploying processing unit...");
		SpaceDeployment  deployment = new SpaceDeployment("Test1").partitioned(1, 0).maxInstancesPerVM(1).setContextProperty(APPLICATION_CONTEXT_PROPERY, "App1");
		ProcessingUnit pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);

	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void test() throws InterruptedException {
		
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap appMap = topologyTab.getApplicationMap();
		
		appMap.selectApplication("App1");
		
		LogUtils.log("deploying processing unit...");
		SpaceDeployment  deployment = new SpaceDeployment("Test2").partitioned(1, 0).maxInstancesPerVM(1).setContextProperty(APPLICATION_CONTEXT_PROPERY, "App2");
		final ProcessingUnit pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				ApplicationNode testNode = appMap.getApplicationNode("Test2");
				return (testNode == null);
			}
		}; 
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
	}

}
