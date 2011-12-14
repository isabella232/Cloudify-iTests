package test.webui.topology.applicationmap;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.applicationmap.ApplicationMap;
import test.webui.objects.topology.applicationmap.ApplicationNode;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

/**
 * when focused on one application and deloying into it a pu of a type that exists in another application
 * the pu is placed not in the correct row, taking into account pu's of a different application
 * @author elip
 *
 */

public class ApplicationMapLayouterYPositionTest extends AbstractSeleniumTest {
	
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
		gsmA.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("session-test-embedded.war"))
			.setContextProperty(APPLICATION_CONTEXT_PROPERY, "App1"));
		
		LogUtils.log("deploying processing unit...");
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 0).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment.setContextProperty(APPLICATION_CONTEXT_PROPERY, "App2"));
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void layouteryPositionTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap appMap = topologyTab.getApplicationMap();
		
		appMap.selectApplication("App1");
		
		appMap.selectApplication("App2");
		
		LogUtils.log("deploying processing unit...");
		final ProcessingUnit puSessionTest2 = gsmA.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("session-test-embedded.war"))
			.name("session2").setContextProperty(APPLICATION_CONTEXT_PROPERY, "App2"));
		ProcessingUnitUtils.waitForDeploymentStatus(puSessionTest2, DeploymentStatus.INTACT);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				ApplicationNode session2 = appMap.getApplicationNode(puSessionTest2.getName());
				ApplicationNode test = appMap.getApplicationNode("Test");
				return (session2.getyPosition() == test.getyPosition());
			}
		};
		takeScreenShot(this.getClass(), "layouteryPositionTest");
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
	}

}
