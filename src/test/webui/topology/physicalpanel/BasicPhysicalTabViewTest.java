package test.webui.topology.physicalpanel;

import java.net.MalformedURLException;

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
import test.webui.objects.dashboard.ServicesGrid.Icon;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.physicalpanel.HostData;
import test.webui.objects.topology.physicalpanel.PhysicalPanel;
import framework.utils.AdminUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

public class BasicPhysicalTabViewTest extends AbstractSeleniumTest {
	
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
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 0).maxInstancesPerVM(1);
		test = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(test, DeploymentStatus.INTACT);
		
		LogUtils.log("deploying processing unit...");
		deployment = new SpaceDeployment("Test2").partitioned(1, 0).maxInstancesPerVM(1);
		test2 = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(test2, DeploymentStatus.INTACT);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void basicApplicationMapTest() throws InterruptedException, MalformedURLException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();

		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();

		PhysicalPanel physicalPanel = topologyTab.getTopologySubPanel().switchToPhysicalPanel();
		
		HostData me = physicalPanel.getHostData(machineA.getHostName());
		
		assertTrue(me != null);
		assertTrue(me.getGSACount() == 1);
		assertTrue(me.getGSCCount() == 2);
		assertTrue(me.getGSMCount() == 1);
		assertTrue(me.getNumberOfCores() == 2);
		assertTrue(me.getPUIs().getBoxes().size() == 2);
		assertTrue(me.getIcon().equals(Icon.OK));
	}

}
