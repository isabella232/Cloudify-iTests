package test.webui.topology.healthpanel;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.data.Person;
import test.utils.AdminUtils;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.HealthPanel;
import test.webui.objects.topology.TopologyTab;

public class MemoryMetricBalanceTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit pu;
	
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
		SpaceDeployment deployment = new SpaceDeployment("Test").numberOfInstances(2).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void memoryMetricTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		GigaSpace space0 = pu.getInstances()[0].getSpaceInstance().getGigaSpace();
		System.out.println(pu.getPartitions().length);
		GigaSpace space1 = pu.getInstances()[1].getSpaceInstance().getGigaSpace();
		for (int i = 0 ; i < 50000 ; i++) {
			space0.write(new Person(new Long(i)));
		}
		for (int i = 0 ; i < 500 ; i++) {
			space1.write(new Person(new Long(i)));
		}
		
		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();
		
		assertTrue(healthPanel.getMetric("Memory").getBalance() != 0);
	}

}
