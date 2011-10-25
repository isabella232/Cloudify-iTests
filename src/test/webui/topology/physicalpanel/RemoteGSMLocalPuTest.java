package test.webui.topology.physicalpanel;

import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

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

import test.utils.AdminUtils;
import test.utils.DeploymentUtils;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.PhysicalPanel;
import test.webui.objects.topology.PhysicalPanel.Host;
import test.webui.objects.topology.PhysicalPanel.Host.PuIBox;
import test.webui.objects.topology.PhysicalPanel.OS;
import test.webui.objects.topology.TopologyTab;

public class RemoteGSMLocalPuTest extends AbstractSeleniumTest {
	
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
		
		log("loading GSM on a remote machine");
		GridServiceManager remoteGSM = loadGSM(machineB);
		
		log("loading 2 GSC on local machine");
		AdminUtils.loadGSCs(machineA, 2);
        
		LogUtils.log("deploying application using the remote GSM");
		
        LogUtils.log("Deploying web application with remote space : " + webApp);
        
        LogUtils.log("deploying mySpace");
		SpaceDeployment deployment = new SpaceDeployment("mySpace").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", webApp);
		mySpacePu = remoteGSM.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(mySpacePu, DeploymentStatus.INTACT);
    	
		LogUtils.log("deploying web app remote");
		puSessionTest = remoteGSM.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("session-test-remote.war")).setContextProperty("com.gs.application", webApp));
		ProcessingUnitUtils.waitForDeploymentStatus(puSessionTest, DeploymentStatus.INTACT);

	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
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
		for (PuIBox p : puis) {
			assertTrue(p.getNumberOfInstances() == appMap.getApplicationNode(p.getAssociatedProcessingUnitName()).getActualInstances()); 
		}
		assertTrue(hostA.getOS().equals(OS.WINDOWS32));
		
		Host hostB = physicalPanel.getHost(hostBName);
		assertTrue(hostB != null);
		assertTrue(hostB.getGSACount() == machineB.getGridServiceAgents().getAgents().length);
		assertTrue(hostB.getGSCCount() == machineB.getGridServiceContainers().getContainers().length);
		assertTrue(hostB.getGSMCount() == machineB.getGridServiceManagers().getManagers().length);
		assertTrue(hostB.getPUIs().getBoxes().size() == 0);
		
	}
}
