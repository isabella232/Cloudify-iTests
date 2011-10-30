package test.webui.topology.applicationmap;

import static framework.utils.AdminUtils.loadGSCs;
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

import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.TopologyTab;

public class NodeSelectionDelayTest extends AbstractSeleniumTest {
	
	private Machine machineA;
	private ProcessingUnit pu;
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
		
		LogUtils.log("deploying processing unit...");
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 0).maxInstancesPerVM(1).setContextProperty(APPLICATION_CONTEXT_PROPERY, "App");
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
		LogUtils.log("deploying processing unit...");
		deployment = new SpaceDeployment("Test2").partitioned(2, 1).maxInstancesPerVM(1).setContextProperty(APPLICATION_CONTEXT_PROPERY, "App");
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
		LogUtils.log("deploying processing unit...");
		deployment = new SpaceDeployment("Test3").partitioned(1, 0).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void nodeSelectionDelayTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap appMap = topologyTab.getApplicationMap();
		
		LogUtils.log("Selecting application");
		
		long currentTimeMillisBeforeClick = System.currentTimeMillis();
		LogUtils.log("Before click time was :" + currentTimeMillisBeforeClick);
		appMap.selectApplication("App");
		long currentTimeMillisAfterClick = System.currentTimeMillis();
		LogUtils.log("After click time is : " + currentTimeMillisAfterClick);	
		LogUtils.log("Difference is :" + (currentTimeMillisAfterClick - currentTimeMillisBeforeClick));

		
		ApplicationNode testNode = appMap.getApplicationNode("Test");
		
		testNode.select();
		
		ApplicationNode testNode2 = appMap.getApplicationNode("Test2");
		
		LogUtils.log("Selecting node");
		
		currentTimeMillisBeforeClick = System.currentTimeMillis();
		LogUtils.log("Before click time was :" + currentTimeMillisBeforeClick);
		testNode2.select();
		currentTimeMillisAfterClick = System.currentTimeMillis();
		LogUtils.log("After click time is : " + currentTimeMillisAfterClick);	
		LogUtils.log("Difference is :" + (currentTimeMillisAfterClick - currentTimeMillisBeforeClick));
	}

}
