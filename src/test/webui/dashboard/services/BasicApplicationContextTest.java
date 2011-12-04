package test.webui.dashboard.services;

import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.openspaces.admin.application.Application;
import org.openspaces.admin.application.ApplicationDeployment;
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
import framework.utils.DBUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.ToStringUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.ServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationsMenuPanel;
import test.webui.objects.dashboard.ServicesGrid.DataReplicationGrid;
import test.webui.objects.dashboard.ServicesGrid.EDSGrid;
import test.webui.objects.dashboard.ServicesGrid.Icon;
import test.webui.objects.dashboard.ServicesGrid.InfrastructureServicesGrid;

public class BasicApplicationContextTest extends AbstractSeleniumTest {
	
	private static final int HSQL_DB_PORT = 9876;
	private Machine machine;
	private int hsqlId;
	private GridServiceAgent gsa;
	private ProcessingUnit mirror;
	ProcessingUnit loader;
	ProcessingUnit runtime;
	ProcessingUnit puSessionTest;
	ProcessingUnit mySpacePu;
	private String mirrorAppName = "MirrorApp";
	private String webAppName = "WebRemoteSpaceApp";
	GridServiceManager gsm;
	
	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {
		
		log("waiting for 1 GSA");
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading GSM");
		gsm = loadGSM(machine);
		
		log("loading 1 GSC on 1 machine");
		AdminUtils.loadGSCs(machine, 4);
		
        log("load HSQL DB on machine - "+ToStringUtils.machineToString(machine));
        hsqlId = DBUtils.loadHSQLDB(machine, "MirrorPersistFailureAlertTest", HSQL_DB_PORT);
        LogUtils.log("Loaded HSQL successfully, id ["+hsqlId+"]");
        
        LogUtils.log("Deploying application with mirror : " + mirrorAppName);
		
        DeploymentUtils.prepareApp("MHEDS");
		
        Application mirrorApp = 
        gsm.deploy(new ApplicationDeployment(mirrorAppName,
		
        		new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "mirror"))
       				.setContextProperty("port", String.valueOf(HSQL_DB_PORT))
       				.setContextProperty("host", machine.getHostAddress()),
        				
        		new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "runtime"))
       				.numberOfInstances(1)
       				.numberOfBackups(1)
       				.maxInstancesPerVM(0)
        			.setContextProperty("port", String.valueOf(HSQL_DB_PORT))
        			.setContextProperty("host", machine.getHostAddress())
        			.addDependency("mirror"),
        				
        		new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "loader"))
        			.setContextProperty("accounts", String.valueOf(10000))
        			.setContextProperty("delay", "100")
        			.addDependency("runtime")
		));
		
        LogUtils.log("Deploying web application with remote space : " + webAppName);
        
        Application webApp = 
        gsm.deploy(new ApplicationDeployment(webAppName,
        		
        		new SpaceDeployment("mySpace")
        		.partitioned(1, 1)
        		.maxInstancesPerVM(1),
        		
        		new ProcessingUnitDeployment(DeploymentUtils.getArchive("session-test-remote.war"))
        		.addDependency("mySpace")
   		));
        		
        				
		mirror = mirrorApp.getProcessingUnits().getProcessingUnit("mirror");
        runtime = mirrorApp.getProcessingUnits().getProcessingUnit("runtime");
        loader = mirrorApp.getProcessingUnits().getProcessingUnit("loader");
        
        mirror.waitFor(mirror.getTotalNumberOfInstances());
        runtime.waitFor(runtime.getTotalNumberOfInstances());
        ProcessingUnitUtils.waitForDeploymentStatus(runtime, DeploymentStatus.INTACT);
        loader.waitFor(loader.getTotalNumberOfInstances());
        
        
        mySpacePu = webApp.getProcessingUnits().getProcessingUnit("mySpace");
		puSessionTest = webApp.getProcessingUnits().getProcessingUnit("session-test-remote");
		ProcessingUnitUtils.waitForDeploymentStatus(mySpacePu, DeploymentStatus.INTACT);
    	ProcessingUnitUtils.waitForDeploymentStatus(puSessionTest, DeploymentStatus.INTACT);

	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void applicationsTest() throws InterruptedException {
		
		 final int machines = admin.getMachines().getMachines().length;
		 
		 int waitingTime = 20000;
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		DashboardTab dashboardTab = loginPage.login().switchToDashboard();
		
		ServicesGrid appGrid = dashboardTab.getServicesGrid();
		
		final ApplicationServicesGrid applicationServices = appGrid.getApplicationServicesGrid();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (applicationServices.getMirrorModule().getCount() == 1);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(applicationServices.getMirrorModule().getIcon().equals(Icon.OK));
		assertTrue(applicationServices.getStatefullModule().getCount() == 2);
		assertTrue(applicationServices.getStatefullModule().getIcon().equals(Icon.OK));
		assertTrue(applicationServices.getStatelessModule().getCount() == 1);
		assertTrue(applicationServices.getStatelessModule().getIcon().equals(Icon.OK));
		assertTrue(applicationServices.getWebModule().getCount() == 1);
		assertTrue(applicationServices.getWebModule().getIcon().equals(Icon.OK));
		
		final InfrastructureServicesGrid infrastructure = appGrid.getInfrastructureGrid();
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getHosts().getCount() == machines);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getGSAInst().getCount() == machines);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getGSAInst().getIcon().equals(Icon.OK));
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getGSMInst().getCount() == 1);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getGSMInst().getIcon().equals(Icon.OK));
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getLUSInst().getCount() == machines);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getLUSInst().getIcon().equals(Icon.OK));
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getGSCInst().getCount() == 4);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructure.getGSCInst().getIcon().equals(Icon.OK));
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);

		ApplicationsMenuPanel applicationsPanel = appGrid.getApplicationsMenuPanel();
		
		// select application and verify modules for specific application
		applicationsPanel.selectApplication(mirrorAppName);
		
		assertTrue(applicationServices.getMirrorModule().getCount() == 1);
		assertTrue(applicationServices.getMirrorModule().getIcon().equals(Icon.OK));
		
		assertTrue(applicationServices.getStatefullModule().getCount() == 1);
		assertTrue(applicationServices.getMirrorModule().getIcon().equals(Icon.OK));
		
		assertTrue(applicationServices.getStatelessModule().getCount() == 1);
		assertTrue(applicationServices.getMirrorModule().getIcon().equals(Icon.OK));
		
		final DataReplicationGrid replicationGrid = appGrid.getDataReplicationGrid();
				
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (replicationGrid.getBytesPerSecond().getCount() > 0);
			}
		};	
		AssertUtils.repetitiveAssertTrue(null, condition,10000);
		assertTrue(replicationGrid.getBytesPerSecond().getIcon().equals(Icon.OK));
		assertTrue(replicationGrid.getPacketsPerSecond().getCount() > 0);
		assertTrue(replicationGrid.getPacketsPerSecond().getIcon().equals(Icon.OK));
		
		final EDSGrid edsGrid = appGrid.getEdsGrid();
		
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (edsGrid.getBytesPerSecond().getCount() > 0);
			}
		};	
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		assertTrue(edsGrid.getBytesPerSecond().getIcon().equals(Icon.OK));	
				
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (edsGrid.getOpPerSecond().getCount() > 0);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(edsGrid.getOpPerSecond().getIcon().equals(Icon.OK));
		assertTrue(edsGrid.getPacketsPerSecond().getCount() > 0);
		assertTrue(edsGrid.getPacketsPerSecond().getIcon().equals(Icon.OK));
        	
		// select application and verify modules for specific application
		applicationsPanel.selectApplication(webAppName);
		
		assertTrue(applicationServices.getWebModule().getCount() == 1);
		assertTrue(applicationServices.getWebModule().getIcon().equals(Icon.OK));
		assertTrue(applicationServices.getStatefullModule().getCount() == 1);
		assertTrue(applicationServices.getStatefullModule().getIcon().equals(Icon.OK));
		
	}

}
