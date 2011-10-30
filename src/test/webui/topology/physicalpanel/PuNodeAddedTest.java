package test.webui.topology.physicalpanel;

import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import java.util.List;
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

import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.PhysicalPanel;
import test.webui.objects.topology.PhysicalPanel.Host;
import test.webui.objects.topology.PhysicalPanel.Host.PuIBox;
import test.webui.objects.topology.TopologyTab;

public class PuNodeAddedTest extends AbstractSeleniumTest {
	
	private Machine machineA;
	private Machine machineB;
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
		
		log("loading 2 GSC on 1 machine");
		AdminUtils.loadGSCs(machineA, 2);
        
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
	public void additionEventsTest() throws InterruptedException {
		
		int waitingTime = 10000;
		
		// get new login page
		LoginPage loginPage = getLoginPage();

		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();

		ApplicationMap appMap = topologyTab.getApplicationMap();
		
		appMap.selectApplication(webApp);
		
		final PhysicalPanel physical = topologyTab.getTopologySubPanel().switchToPhysicalPanel();
		
		Host hostA = physical.getHost(machineA.getHostName());
		
		List<PuIBox> puis = hostA.getPUIs().getBoxes();
		assertTrue(puis.size() == 2);
		
        LogUtils.log("deploying mySpace");
		SpaceDeployment deployment = new SpaceDeployment("mySpace2").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", webApp);
		mySpacePu = gsm.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(mySpacePu, DeploymentStatus.INTACT);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				Host hostA = physical.getHost(machineA.getHostName());
				List<PuIBox> puis = hostA.getPUIs().getBoxes();
				return (puis.size() == 3);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		AdminUtils.loadGSCs(machineA, 1);
		admin.getGridServiceContainers().waitFor(3);
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				Host hostA = physical.getHost(machineA.getHostName());
				return (hostA.getGSCCount() == admin.getGridServiceContainers().getContainers().length);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		
		puSessionTest.incrementInstance();
		puSessionTest.waitFor(3);
		ProcessingUnitUtils.waitForDeploymentStatus(puSessionTest, DeploymentStatus.INTACT);
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				Host hostA = physical.getHost(machineA.getHostName());
				List<PuIBox> puis = hostA.getPUIs().getBoxes();
				for (PuIBox p : puis) {
					if (p.getAssociatedProcessingUnitName().equals("session-test-remote")) {
						 return (p.getNumberOfInstances() == puSessionTest.getNumberOfInstances());
					}
				}
				return false;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		
	}

}
