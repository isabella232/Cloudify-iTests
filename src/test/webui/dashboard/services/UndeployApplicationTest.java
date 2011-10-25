package test.webui.dashboard.services;

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

import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.ServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationsMenuPanel;
import test.webui.objects.dashboard.ServicesGrid.Icon;
import test.webui.objects.dashboard.ServicesGrid.InfrastructureServicesGrid;

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
		
		final int machines = admin.getMachines().getMachines().length;
		
		int waitingTime = 10000;
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		DashboardTab dashboardTab = loginPage.login().switchToDashboard();
		
		ServicesGrid appGrid = dashboardTab.getServicesGrid();
		
		final ApplicationServicesGrid applicationServices = appGrid.getApplicationServicesGrid();
		final ApplicationsMenuPanel applicationsPanel = appGrid.getApplicationsMenuPanel();
		
		applicationsPanel.selectApplication("All Apps");
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (applicationServices.getStatefullModule().getCount() == 1);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(applicationServices.getStatefullModule().getIcon().equals(Icon.OK));
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (applicationServices.getWebModule().getCount() == 1);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(applicationServices.getWebModule().getIcon().equals(Icon.OK));
		
		final InfrastructureServicesGrid infrastructure = appGrid.getInfrastructureGrid();
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getHosts().getCount() == machines);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,10000);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getGSAInst().getCount() == machines);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,10000);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getGSAInst().getIcon().equals(Icon.OK));
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,10000);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getGSMInst().getCount() == 1);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,10000);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getGSMInst().getIcon().equals(Icon.OK));
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,10000);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getLUSInst().getCount() == machines);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,10000);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getLUSInst().getIcon().equals(Icon.OK));
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,10000);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getGSCInst().getCount() == 2);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,10000);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getGSCInst().getIcon().equals(Icon.OK));
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,10000);
		
		applicationsPanel.selectApplication(webApp);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (applicationServices.getWebModule().getCount() == 1);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(applicationServices.getWebModule().getIcon().equals(Icon.OK));
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (applicationServices.getStatefullModule().getCount() == 1);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(applicationServices.getStatefullModule().getIcon().equals(Icon.OK));
		
		puSessionTest.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(puSessionTest, DeploymentStatus.UNDEPLOYED);
		mySpacePu.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(mySpacePu, DeploymentStatus.UNDEPLOYED);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (applicationsPanel.isApplicationPresent(webApp) == false);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);	
		
	}
	

}
