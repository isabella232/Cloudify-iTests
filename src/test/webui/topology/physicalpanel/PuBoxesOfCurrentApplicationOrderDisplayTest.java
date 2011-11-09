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
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.PhysicalPanel.HostData;
import test.webui.objects.topology.PhysicalPanel.HostData.PuIBox;

public class PuBoxesOfCurrentApplicationOrderDisplayTest extends AbstractSeleniumTest {

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
		SpaceDeployment deployment = new SpaceDeployment("Test1").partitioned(1, 0).maxInstancesPerVM(1).setContextProperty("com.gs.application", "MyApp");
		test = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(test, DeploymentStatus.INTACT);
		
		LogUtils.log("deploying processing unit...");
		deployment = new SpaceDeployment("Test2").partitioned(1, 0).maxInstancesPerVM(1).setContextProperty("com.gs.application", "MyApp");
		test2 = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(test2, DeploymentStatus.INTACT);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void orderTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();

		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();

		ApplicationMap appMap = topologyTab.getApplicationMap();
		
		appMap.selectApplication("MyApp");
		
		ApplicationNode test2 = appMap.getApplicationNode("Test2");
		
		test2.select();
		
		// wait for the animation to stop
		Thread.sleep(1000);
		
		PhysicalPanel physical = topologyTab.getTopologySubPanel().switchToPhysicalPanel();
		
		HostData hostA = physical.getHostData(machineA.getHostName());
		
		List<PuIBox> puis = hostA.getPUIs().getBoxes();
		
		assertTrue(puis.size() == 2);
		
		for (PuIBox p : puis) {
			if (p.getIndex() == 1) assertTrue(p.getAssociatedProcessingUnitName().equals("Test2"));
			if (p.getIndex() == 2) assertTrue(p.getAssociatedProcessingUnitName().equals("Test1"));
		}
		
		ApplicationNode test1 = appMap.getApplicationNode("Test1");
		
		test1.select();
		
		hostA = physical.getHostData(machineA.getHostName());
		
		puis = hostA.getPUIs().getBoxes();
		
		assertTrue(puis.size() == 2);
		
		for (PuIBox p : puis) {
			if (p.getIndex() == 1) assertTrue(p.getAssociatedProcessingUnitName().equals("Test1"));
			if (p.getIndex() == 2) assertTrue(p.getAssociatedProcessingUnitName().equals("Test2"));
		}
		
	}
}
