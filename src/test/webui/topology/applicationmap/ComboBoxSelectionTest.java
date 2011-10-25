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
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.TopologySubPanel;
import test.webui.objects.topology.TopologyTab;

public class ComboBoxSelectionTest extends AbstractSeleniumTest {
	
	@BeforeMethod(alwaysRun = true)
	public void startSetup() {
		
		log("waiting for 1 GSA");
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		Machine machine = gsa.getMachine();
		
		log("loading GSM");
		GridServiceManager gsm = loadGSM(machine);
		
		log("loading 1 GSC on 1 machine");
		AdminUtils.loadGSCs(machine, 2);
		
        LogUtils.log("deploying spaces onto 4 different applications");
		SpaceDeployment deployment = new SpaceDeployment("space1").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", "App1");
		ProcessingUnit mySpacePu = gsm.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(mySpacePu, DeploymentStatus.INTACT);
		
		deployment = new SpaceDeployment("space2").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", "App2");
		mySpacePu = gsm.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(mySpacePu, DeploymentStatus.INTACT);
		
		deployment = new SpaceDeployment("space3").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", "App3");
		mySpacePu = gsm.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(mySpacePu, DeploymentStatus.INTACT);

		deployment = new SpaceDeployment("space4").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", "App4");
		mySpacePu = gsm.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(mySpacePu, DeploymentStatus.INTACT);
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void testCombo() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		TopologySubPanel tpSub = topologyTab.getTopologySubPanel();
		
		for (int i = 0 ; i < 15 ; i++) {
			applicationMap.selectApplication("App1");
			assertTrue(tpSub.getAssociatedPuName().equals("space1"));
			applicationMap.selectApplication("App2");
			assertTrue(tpSub.getAssociatedPuName().equals("space2"));
			applicationMap.selectApplication("App3");
			assertTrue(tpSub.getAssociatedPuName().equals("space3"));
			applicationMap.selectApplication("App4");
			assertTrue(tpSub.getAssociatedPuName().equals("space4"));
		}
	}
	

}
