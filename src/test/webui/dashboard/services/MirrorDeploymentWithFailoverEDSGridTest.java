package test.webui.dashboard.services;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.AssertUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.DBUtils;
import test.utils.DeploymentUtils;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;
import test.utils.ToStringUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.ServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.DataReplicationGrid;
import test.webui.objects.dashboard.ServicesGrid.EDSGrid;
import test.webui.objects.dashboard.ServicesGrid.Icon;

public class MirrorDeploymentWithFailoverEDSGridTest extends AbstractSeleniumTest {
	
	private static final int HSQL_DB_PORT = 9876;
	private Machine machine;
	private int hsqlId;
	private GridServiceAgent gsa;
	private ProcessingUnit mirror;
	private ProcessingUnit runtime;
	private ProcessingUnit loader;
	
	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {
		
		log("waiting for 1 GSA");
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading GSM");
		GridServiceManager gsm = loadGSM(machine);
		
		log("loading 1 GSC on 1 machine");
		loadGSC(machine);
		
		
        log("load HSQL DB on machine - "+ToStringUtils.machineToString(machine));
        hsqlId = DBUtils.loadHSQLDB(machine, "MirrorDeploymentWithFailoverEDSGridTest", HSQL_DB_PORT);
        LogUtils.log("Loaded HSQL successfully, id ["+hsqlId+"]");
        
        log("deploy mirror via GSM");
        DeploymentUtils.prepareApp("MHEDS");
		mirror = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "mirror")).
                setContextProperty("port", String.valueOf(HSQL_DB_PORT)).setContextProperty("host", machine.getHostAddress()));
		mirror.waitFor(mirror.getTotalNumberOfInstances());
		
        log("deploy runtime(1,1) via GSM");
        runtime = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "runtime")).
                numberOfInstances(1).numberOfBackups(1).maxInstancesPerVM(0).setContextProperty("port", String.valueOf(HSQL_DB_PORT)).
                setContextProperty("host", machine.getHostAddress()));
        runtime.waitFor(runtime.getTotalNumberOfInstances());
        ProcessingUnitUtils.waitForDeploymentStatus(runtime, DeploymentStatus.INTACT);
        
        log("deploy loader via GSM");
        loader = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "loader"))
        .setContextProperty("accounts", String.valueOf(10000)).setContextProperty("delay", "100"));
        loader.waitFor(loader.getTotalNumberOfInstances());
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void mirrorTest() throws InterruptedException {
		
		int waitingTime = 20000;
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		DashboardTab dashboardTab = loginPage.login().switchToDashboard();
		
		ServicesGrid appGrid = dashboardTab.getServicesGrid();
		
		final ApplicationServicesGrid modulesGrid = appGrid.getApplicationServicesGrid();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (modulesGrid.getMirrorModule().getCount() == 1);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (modulesGrid.getMirrorModule().getIcon().equals(Icon.OK));
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);

		final EDSGrid edsGrid = appGrid.getEdsGrid();
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (edsGrid.getBytesPerSecond().getCount() > 0);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (edsGrid.getBytesPerSecond().getIcon().equals(Icon.OK));
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (edsGrid.getPacketsPerSecond().getCount() > 0);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (edsGrid.getPacketsPerSecond().getIcon().equals(Icon.OK));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (edsGrid.getOpPerSecond().getCount() > 0);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (edsGrid.getOpPerSecond().getIcon().equals(Icon.OK));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		final DataReplicationGrid hiAv = appGrid.getDataReplicationGrid();
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (hiAv.getBytesPerSecond().getCount() > 0);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (hiAv.getBytesPerSecond().getIcon().equals(Icon.OK));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (hiAv.getPacketsPerSecond().getCount() > 0);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (hiAv.getPacketsPerSecond().getIcon().equals(Icon.OK));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		// execute failover
		GridServiceContainer gsc = admin.getGridServiceContainers().getContainers()[0];
		gsc.restart();
		admin.getGridServiceContainers().waitFor(0);
		admin.getGridServiceContainers().waitFor(1);
		ProcessingUnitUtils.waitForDeploymentStatus(mirror, DeploymentStatus.INTACT);
		ProcessingUnitUtils.waitForDeploymentStatus(loader, DeploymentStatus.INTACT);
		ProcessingUnitUtils.waitForDeploymentStatus(runtime, DeploymentStatus.INTACT);	

		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (modulesGrid.getMirrorModule().getIcon().equals(Icon.OK));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (edsGrid.getBytesPerSecond().getIcon().equals(Icon.OK));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (edsGrid.getBytesPerSecond().getCount() > 0);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (edsGrid.getOpPerSecond().getIcon().equals(Icon.OK));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (edsGrid.getOpPerSecond().getCount() > 0);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (edsGrid.getPacketsPerSecond().getIcon().equals(Icon.OK));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (edsGrid.getPacketsPerSecond().getCount() > 0);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);

	}
}
