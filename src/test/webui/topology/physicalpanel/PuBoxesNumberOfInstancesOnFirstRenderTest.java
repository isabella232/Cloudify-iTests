package test.webui.topology.physicalpanel;

import java.util.List;

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
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.PhysicalPanel;
import test.webui.objects.topology.PhysicalPanel.Host;
import test.webui.objects.topology.PhysicalPanel.Host.PuIBox;
import test.webui.objects.topology.TopologyTab;

public class PuBoxesNumberOfInstancesOnFirstRenderTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit test;
	ProcessingUnit test2;
	
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
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 0).maxInstancesPerVM(1).setContextProperty(APPLICATION_CONTEXT_PROPERY, "App");
		test = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(test, DeploymentStatus.INTACT);
		
		LogUtils.log("deploying processing unit...");
		deployment = new SpaceDeployment("Test2").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty(APPLICATION_CONTEXT_PROPERY, "App");
		test2 = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(test2, DeploymentStatus.INTACT);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void testNumberOfInstances() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();

		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();

		ApplicationMap appMap = topologyTab.getApplicationMap();
		
		appMap.selectApplication("App");
		
		ApplicationNode testNode = appMap.getApplicationNode("Test");
		
		ApplicationNode test2Node = appMap.getApplicationNode("Test2");
		
		PhysicalPanel physicalPanel = topologyTab.getTopologySubPanel().switchToPhysicalPanel();
		
		Host me = physicalPanel.getHost(machineA.getHostName());
		
		List<PuIBox> puBoxes = me.getPUIs().getBoxes();
		
		for (PuIBox pbox : puBoxes) {
			if (pbox.getName().equals(testNode.getName())) {
				assertTrue(pbox.getNumberOfInstances() == testNode.getActualInstances());
			}
			if (pbox.getName().equals(test2Node.getName())) {
				assertTrue(pbox.getNumberOfInstances() == test2Node.getActualInstances());
			}
		}
		
		
	}

}
