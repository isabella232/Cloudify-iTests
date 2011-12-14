package test.webui.topology.applicationmap;

import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import java.util.concurrent.TimeUnit;

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
import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

public class IncermentWebPuNodeInstanceTest extends AbstractSeleniumTest {
	
	private Machine machineA;
	ProcessingUnit puSessionTest;
	ProcessingUnit mySpacePu;
	GridServiceManager gsm;
	private String webApp = "WebRemoteSpaceApp";
	
	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {
		
		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitForAtLeastOne(30, TimeUnit.SECONDS);
		
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		
		GridServiceAgent gsaA = agents[0];
		
		machineA = gsaA.getMachine();
		
		log("loading 1 GSM on 1 machine");
		gsm = loadGSM(machineA);
		
		log("loading 3 GSC on 1 machine");
		AdminUtils.loadGSCs(machineA, 3);
        
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
	public void inceremntInst() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();

		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();

		final ApplicationMap appMap = topologyTab.getApplicationMap();
		
		appMap.selectApplication(webApp);
		
		ApplicationNode session = appMap.getApplicationNode(puSessionTest.getName());
		
		assertTrue(session.getPlannedInstances() == 2);
		assertTrue(session.getActualInstances() == session.getPlannedInstances());
		
		puSessionTest.incrementInstance();
		puSessionTest.waitFor(3);
		ProcessingUnitUtils.waitForDeploymentStatus(puSessionTest, DeploymentStatus.INTACT);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				ApplicationNode session = appMap.getApplicationNode(puSessionTest.getName());
				return (session.getPlannedInstances() == 3);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, 5000);
	}

}
