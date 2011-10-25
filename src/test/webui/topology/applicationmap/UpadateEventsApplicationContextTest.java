package test.webui.topology.applicationmap;

import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

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
import test.utils.AssertUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.DeploymentUtils;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.TopologyTab;

public class UpadateEventsApplicationContextTest extends AbstractSeleniumTest {
	
	private Machine machine;
	private GridServiceAgent gsa;
	ProcessingUnit loader;
	ProcessingUnit runtime;
	ProcessingUnit puSessionTest;
	ProcessingUnit mySpacePu;
	ProcessingUnit test;
	GridServiceManager gsm;
	private String webApp = "WebRemoteSpaceApp";
	
	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {
		
		log("waiting for 1 GSA");
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading GSM");
		gsm = loadGSM(machine);
		
		log("loading 2 GSC on 1 machine");
		AdminUtils.loadGSCs(machine, 2);
        
        LogUtils.log("Deploying web application with remote space : " + webApp);
		LogUtils.log("deploying web app embedded");
		puSessionTest = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("session-test-embedded.war")).setContextProperty("com.gs.application", webApp));
		ProcessingUnitUtils.waitForDeploymentStatus(puSessionTest, DeploymentStatus.INTACT);

	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void updateEventsTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		applicationMap.selectApplication(webApp);
		
        LogUtils.log("deploying mySpace");
		SpaceDeployment deployment = new SpaceDeployment("mySpace").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", webApp);
		mySpacePu = gsm.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(mySpacePu, DeploymentStatus.INTACT);
					
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				ApplicationNode node = applicationMap.getApplicationNode("mySpace");
				return ((node != null)
						&& (node.getStatus().equals(DeploymentStatus.INTACT))
						&& (node.getActualInstances() == 2)) ;
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition, 10000);
		
	}

}
