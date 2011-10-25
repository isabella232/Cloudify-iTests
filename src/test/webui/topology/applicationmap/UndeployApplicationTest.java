package test.webui.topology.applicationmap;

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
import framework.utils.AssertUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.TopologyTab;

public class UndeployApplicationTest extends AbstractSeleniumTest {
	
	private Machine machine;
	private GridServiceAgent gsa;
	ProcessingUnit puSessionTest;
	ProcessingUnit mySpacePu;
	private String webApp = "WebRemoteSpaceApp";
	
	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {
		
		log("waiting for 1 GSA");
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading GSM");
		GridServiceManager gsm = loadGSM(machine);
		
		log("loading 1 GSC on 1 machine");
		AdminUtils.loadGSCs(machine, 2);
        
        LogUtils.log("Deploying web application with remote space : " + webApp);
        
        LogUtils.log("deploying mySpace");
		SpaceDeployment deployment = new SpaceDeployment("mySpace").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", webApp);
		mySpacePu = gsm.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(mySpacePu, DeploymentStatus.INTACT);
    	
		LogUtils.log("deploying web app remote");
		puSessionTest = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("session-test-remote.war")).setContextProperty("com.gs.application", webApp));
		ProcessingUnitUtils.waitForDeploymentStatus(puSessionTest, DeploymentStatus.INTACT);

	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void undeployApplicationTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		applicationMap.selectApplication(webApp);
		
		final ApplicationNode mySpaceNode = applicationMap.getApplicationNode("mySpace");
		assertTrue(mySpaceNode != null);
		assertTrue(mySpaceNode.isDisplayed());
		List<String> components = mySpaceNode.getComponents();
		assertTrue(components.contains("processing"));
		assertTrue(components.contains("partition-ha"));
		assertTrue(components.size() == 2);
		
		ApplicationNode sessionNode = applicationMap.getApplicationNode("session-test-remote");
		assertTrue(sessionNode != null);
		assertTrue(sessionNode.isDisplayed());
		
		ProcessingUnit[] processingUnits = puSessionTest.getApplication().getProcessingUnits().getProcessingUnits();
		for (ProcessingUnit pu : processingUnits) {
			pu.undeploy();
			ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.UNDEPLOYED);
		}
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				ApplicationNode mySpaceNode = applicationMap.getApplicationNode("mySpace");
				return (mySpaceNode == null);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,10000);
		sessionNode = applicationMap.getApplicationNode("session-test-remote");
		assertTrue(sessionNode == null);
	}
}
