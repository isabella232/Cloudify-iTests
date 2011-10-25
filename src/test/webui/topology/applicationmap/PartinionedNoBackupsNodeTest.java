package test.webui.topology.applicationmap;

import java.net.MalformedURLException;
import java.util.List;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;

import test.utils.AdminUtils;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.services.PuTreeGrid.WebUIProcessingUnit;
import test.webui.objects.services.ServicesTab;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.TopologyTab;

public class PartinionedNoBackupsNodeTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit Pu;
	
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
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(2, 0).maxInstancesPerVM(1);
		Pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(Pu, DeploymentStatus.INTACT);
	}
	
	 //@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void basicApplicationMapTest() throws InterruptedException, MalformedURLException {
	
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		ApplicationNode testNode = applicationMap.getApplicationNode("Test");
		
		/* assert node was initially loaded correctly */
		assertNotNull(testNode);
		assertTrue(testNode.getStatus().equals(DeploymentStatus.INTACT));
		assertTrue(testNode.getPlannedInstances() == 2);
		assertTrue(testNode.getPlannedInstances() == testNode.getActualInstances());
		assertTrue(testNode.getPuType().equals("STATEFUL"));
		assertTrue(testNode.getNodeType().equals("PROCESSING_UNIT"));
		List<String> components = testNode.getComponents();
		assertTrue(components.contains("processing"));
		assertTrue(components.contains("partition"));
		assertTrue(components.size() == 2);
		assertTrue(testNode.getxPosition() == 2);
		
		ServicesTab servicesTab = topologyTab.switchToServices();
		
		WebUIProcessingUnit test = servicesTab.getPuTreeGrid().getProcessingUnit("Test");
		assertTrue(test.isPartitioned());

	}

}
