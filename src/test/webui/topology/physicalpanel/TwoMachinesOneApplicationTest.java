package test.webui.topology.physicalpanel;

import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import java.util.List;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AdminUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.PhysicalPanel;
import test.webui.objects.topology.PhysicalPanel.Host;
import test.webui.objects.topology.PhysicalPanel.Host.PuIBox;
import test.webui.objects.topology.PhysicalPanel.OS;
import test.webui.objects.topology.TopologyTab;

public class TwoMachinesOneApplicationTest extends AbstractSeleniumTest {
	
	private Machine machineA;
	private Machine machineB;
	ProcessingUnit puSessionTest;
	ProcessingUnit mySpacePu;
	private String webApp = "WebRemoteSpaceApp";
	
	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {
		
		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(2);
		
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		
		GridServiceAgent gsaA = agents[0];
		GridServiceAgent gsaB = agents[1];
		
		machineA = gsaA.getMachine();
		machineB = gsaB.getMachine();
		
		log("loading 1 GSM on 1 machine");
		GridServiceManager gsm = loadGSM(machineB);
		
		log("loading 2 GSC on 2 different machines");
		AdminUtils.loadGSCs(machineA, 1);
		AdminUtils.loadGSCs(machineB, 1);
        
        LogUtils.log("Deploying web application with remote space : " + webApp);
        
        LogUtils.log("deploying mySpace");
		SpaceDeployment deployment = new SpaceDeployment("mySpace").partitioned(2, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", webApp);
		mySpacePu = gsm.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(mySpacePu, DeploymentStatus.INTACT);
    	
		LogUtils.log("deploying web app remote");
		puSessionTest = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("session-test-remote.war")).setContextProperty("com.gs.application", webApp).numberOfInstances(2).maxInstancesPerVM(1));
		ProcessingUnitUtils.waitForDeploymentStatus(puSessionTest, DeploymentStatus.INTACT);
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void test() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();

		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		ApplicationMap appMap = topologyTab.getApplicationMap();
		
		appMap.selectApplication(webApp);
		
		PhysicalPanel physicalPanel = topologyTab.getTopologySubPanel().switchToPhysicalPanel();
		
		String hostAName = machineA.getHostName();
		String hostBName = machineB.getHostName();
		
		Host hostA = physicalPanel.getHost(hostAName);
		assertTrue(hostA != null);
		assertTrue(hostA.getGSACount() == machineA.getGridServiceAgents().getAgents().length);
		assertTrue(hostA.getGSCCount() == machineA.getGridServiceContainers().getContainers().length);
		assertTrue(hostA.getGSMCount() == machineA.getGridServiceManagers().getManagers().length);
		assertTrue(hostA.getPUIs().getBoxes().size() == 2);
		
		List<PuIBox> puis = hostA.getPUIs().getBoxes();
		assertTrue(puis.size() == 2);
		assertTrue(hostA.getOS().equals(OS.WINDOWS32));
		
		Host hostB = physicalPanel.getHost(hostBName);
		assertTrue(hostB != null);
		assertTrue(hostB.getGSACount() == machineB.getGridServiceAgents().getAgents().length);
		assertTrue(hostB.getGSCCount() == machineB.getGridServiceContainers().getContainers().length);
		assertTrue(hostB.getGSMCount() == machineB.getGridServiceManagers().getManagers().length);
		puis = hostB.getPUIs().getBoxes();
		assertTrue(puis.size() == 2); // TODO eli - make better assertions
		
	}
	

}
