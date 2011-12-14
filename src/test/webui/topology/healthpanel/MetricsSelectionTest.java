package test.webui.topology.healthpanel;

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
import test.webui.objects.topology.Metric;
import test.webui.objects.topology.MetricType;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.healthpanel.HealthPanel;
import test.webui.resources.WebConstants;
import framework.utils.AdminUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

public class MetricsSelectionTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit pu;
	
	@Override
	@BeforeMethod(alwaysRun = true)
	public void beforeTest() {	
		
		setBrowser(WebConstants.CHROME);
		super.beforeTest();
		
		LogUtils.log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		LogUtils.log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		machineA = gsaA.getMachine();

		LogUtils.log("starting: 1 GSM and 2 GSC's on 1 machine");
		GridServiceManager gsmA = AdminUtils.loadGSM(machineA); 
		AdminUtils.loadGSCs(machineA, 1);
		
		LogUtils.log("deploying processing unit...");
		SpaceDeployment deployment = new SpaceDeployment("Test").numberOfInstances(1).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void selectionTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();
		
		Metric cpuMetric = healthPanel.getMetric("CPU");
		
		MetricType takeCountMetric = new MetricType("SPACE", "take count");
		
		cpuMetric.swithToMetric(takeCountMetric);
		
		assertTrue(cpuMetric.getName().equals(takeCountMetric.getName()));
	}
}
