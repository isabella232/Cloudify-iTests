package test.webui.dashboard.services;

import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.ServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationsMenuPanel;
import test.webui.objects.dashboard.ServicesGrid.DataReplicationGrid;
import test.webui.objects.dashboard.ServicesGrid.EDSGrid;
import test.webui.objects.dashboard.ServicesGrid.Icon;
import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.DBUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.ToStringUtils;

public class DataReplicationAndEDSGridTest extends AbstractSeleniumTest {
	
	private static final int HSQL_DB_PORT = 9876;
	private Machine machine;
	private int hsqlId;
	private GridServiceAgent gsa;
	private ProcessingUnit mirror;
	ProcessingUnit loader;
	ProcessingUnit runtime;
	ProcessingUnit puSessionTest;
	ProcessingUnit mySpacePu;
	private String mirrorApp = "MirrorApp";
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
		
		LogUtils.log("Deploying application with mirror : " + mirrorApp);
		
        log("load HSQL DB on machine - "+ToStringUtils.machineToString(machine));
        hsqlId = DBUtils.loadHSQLDB(machine, "MirrorPersistFailureAlertTest", HSQL_DB_PORT);
        LogUtils.log("Loaded HSQL successfully, id ["+hsqlId+"]");
        
        log("deploy mirror via GSM");
        DeploymentUtils.prepareApp("MHEDS");
		mirror = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "mirror")).
                setContextProperty("port", String.valueOf(HSQL_DB_PORT)).setContextProperty("host", machine.getHostAddress()).setContextProperty("com.gs.application", mirrorApp));
		mirror.waitFor(mirror.getTotalNumberOfInstances());

        log("deploy runtime(1,1) via GSM");
        runtime = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "runtime")).
                numberOfInstances(1).numberOfBackups(1).maxInstancesPerVM(0).setContextProperty("port", String.valueOf(HSQL_DB_PORT)).
                setContextProperty("host", machine.getHostAddress()).setContextProperty("com.gs.application", mirrorApp));
        runtime.waitFor(runtime.getTotalNumberOfInstances());
        ProcessingUnitUtils.waitForDeploymentStatus(runtime, DeploymentStatus.INTACT);
        
        log("deploy loader via GSM");
        loader = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "loader"))
        .setContextProperty("accounts", String.valueOf(10000)).setContextProperty("delay", "100").setContextProperty("com.gs.application", mirrorApp));
        loader.waitFor(loader.getTotalNumberOfInstances());

	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void loaderUndeployTest() throws InterruptedException {
		
		int waitingTime = 10000;
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		DashboardTab dashboardTab = loginPage.login().switchToDashboard();
		
		ServicesGrid appGrid = dashboardTab.getServicesGrid();
		
		ApplicationsMenuPanel applicationsPanel = appGrid.getApplicationsMenuPanel();
		
		applicationsPanel.selectApplication(mirrorApp);
		
		final EDSGrid edsGrid = appGrid.getEdsGrid();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (edsGrid.getBytesPerSecond().getCount() > 0);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		assertTrue(edsGrid.getBytesPerSecond().getIcon().equals(Icon.OK));
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (edsGrid.getOpPerSecond().getCount() > 0);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		assertTrue(edsGrid.getOpPerSecond().getIcon().equals(Icon.OK));
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (edsGrid.getPacketsPerSecond().getCount() > 0);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		assertTrue(edsGrid.getPacketsPerSecond().getIcon().equals(Icon.OK));
		
		final DataReplicationGrid replicationGrid = appGrid.getDataReplicationGrid();
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (replicationGrid.getBytesPerSecond().getCount() > 0);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		assertTrue(replicationGrid.getBytesPerSecond().getIcon().equals(Icon.OK));
		assertTrue(replicationGrid.getPacketsPerSecond().getCount() > 0);
		assertTrue(replicationGrid.getPacketsPerSecond().getIcon().equals(Icon.OK));
		
		// execute failover
		log("undeplyoing loader...");
		loader.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(loader, DeploymentStatus.UNDEPLOYED);
		
		runtime.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(runtime, DeploymentStatus.UNDEPLOYED);
				
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (replicationGrid.getBytesPerSecond().getCount() == 0);
			}
		};	
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(replicationGrid.getBytesPerSecond().getIcon().equals(Icon.NA));
		assertTrue(replicationGrid.getPacketsPerSecond().getCount() == 0);
		assertTrue(replicationGrid.getPacketsPerSecond().getIcon().equals(Icon.NA));

		assertTrue(edsGrid.getBytesPerSecond().getCount() == 0);
				
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (edsGrid.getOpPerSecond().getCount() == 0);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(edsGrid.getPacketsPerSecond().getCount() == 0);
	}

}
