package test.webui.dashboard.services;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.util.concurrent.CountDownLatch;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.space.SpaceDeployment;
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
import test.webui.objects.dashboard.ServicesGrid.ApplicationsMenuPanel;
import test.webui.objects.dashboard.ServicesGrid.DataReplicationGrid;
import test.webui.objects.dashboard.ServicesGrid.EDSGrid;
import test.webui.objects.dashboard.ServicesGrid.Icon;

public class ParallelApplicationDeploymentTest extends AbstractSeleniumTest {
	
	private static final int HSQL_DB_PORT = 9876;
	private Machine machine;
	private int hsqlId;
	private ProcessingUnit mirror;
	ProcessingUnit loader;
	ProcessingUnit runtime;
	ProcessingUnit puSessionTest;
	ProcessingUnit mySpacePu;
	private String mirrorApp = "MirrorApp";
	private String webApp = "WebRemoteSpaceApp";
	GridServiceManager gsm;
	CountDownLatch mirrorLatch = new CountDownLatch(1);
	CountDownLatch webLatch = new CountDownLatch(1);
	private int waitingTime = 10000;
	
	@BeforeMethod(alwaysRun = true)
	public void startSetup() {
		log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		machine = gsaA.getMachine();

		log("starting: 1 GSM and 2 GSC's on 1 machine");
		gsm = loadGSM(machine); 
		loadGSCs(machine, 4);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void parallelDeploymentTest() throws InterruptedException {
		
		LoginPage loginPage = getLoginPage();
		
		DashboardTab dashboardTab = loginPage.login().switchToDashboard();
		
		Thread mirrorAppThread = new Thread(new mirrorAppDeployer());
		Thread webAppThread = new Thread(new webAppDeployer());
		
		/* starting simultaneous deployment of two application onto the grid */
		
		mirrorAppThread.start();
		webAppThread.start();
		
		/* waiting for both applications to be deployed */
		mirrorLatch.await();
		webLatch.await();
		
		/* asserting web-ui recieved all events and is showing correct information about them */
		
		ServicesGrid serviceGrid = dashboardTab.getServicesGrid();
		
		ApplicationsMenuPanel applicationsMenuPanel = serviceGrid.getApplicationsMenuPanel();
		
		assertTrue(applicationsMenuPanel.isApplicationPresent(mirrorApp));
		assertTrue(applicationsMenuPanel.isApplicationPresent(webApp));
		
		applicationsMenuPanel.showAllApplications();
		
		final ApplicationServicesGrid applicationServicesGridAll = serviceGrid.getApplicationServicesGrid();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((applicationServicesGridAll.getMirrorModule().getCount() == 1)
					&& (applicationServicesGridAll.getMirrorModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((applicationServicesGridAll.getStatefullModule().getCount() == 2)
					&& (applicationServicesGridAll.getStatefullModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((applicationServicesGridAll.getStatelessModule().getCount() == 1)
					&& (applicationServicesGridAll.getStatelessModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((applicationServicesGridAll.getWebModule().getCount() == 1)
					&& (applicationServicesGridAll.getWebModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);	
		
		applicationsMenuPanel.selectApplication(mirrorApp);
		
		final ApplicationServicesGrid applicationServicesGridMirrorApp = serviceGrid.getApplicationServicesGrid();
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((applicationServicesGridMirrorApp.getMirrorModule().getCount() == 1)
					&& (applicationServicesGridMirrorApp.getMirrorModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((applicationServicesGridMirrorApp.getStatefullModule().getCount() == 1)
					&& (applicationServicesGridMirrorApp.getStatefullModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((applicationServicesGridMirrorApp.getStatelessModule().getCount() == 1)
					&& (applicationServicesGridMirrorApp.getStatelessModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		final DataReplicationGrid dataReplicationGridMirrorApp = serviceGrid.getDataReplicationGrid();
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((dataReplicationGridMirrorApp.getBytesPerSecond().getCount() > 0)
					&& (dataReplicationGridMirrorApp.getBytesPerSecond().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((dataReplicationGridMirrorApp.getPacketsPerSecond().getCount() > 0)
					&& (dataReplicationGridMirrorApp.getPacketsPerSecond().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		final EDSGrid edsGridMirrorApp = serviceGrid.getEdsGrid();
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((edsGridMirrorApp.getBytesPerSecond().getCount() > 0)
					&& (edsGridMirrorApp.getBytesPerSecond().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((edsGridMirrorApp.getPacketsPerSecond().getCount() > 0)
					&& (edsGridMirrorApp.getPacketsPerSecond().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((edsGridMirrorApp.getOpPerSecond().getCount() > 0)
					&& (edsGridMirrorApp.getOpPerSecond().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		
		applicationsMenuPanel.selectApplication(webApp);
		
		final ApplicationServicesGrid applicationServicesGridWebApp = serviceGrid.getApplicationServicesGrid();
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((applicationServicesGridWebApp.getStatefullModule().getCount() == 1)
					&& (applicationServicesGridWebApp.getStatefullModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((applicationServicesGridWebApp.getWebModule().getCount() == 1)
					&& (applicationServicesGridWebApp.getWebModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		final DataReplicationGrid dataReplicationGridWebApp = serviceGrid.getDataReplicationGrid();
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((dataReplicationGridWebApp.getBytesPerSecond().getCount() == 0)
					&& (dataReplicationGridWebApp.getBytesPerSecond().getIcon().equals(Icon.NA)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((dataReplicationGridWebApp.getPacketsPerSecond().getCount() == 0)
					&& (dataReplicationGridWebApp.getPacketsPerSecond().getIcon().equals(Icon.NA)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		final EDSGrid edsGridWebApp = serviceGrid.getEdsGrid();
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((edsGridWebApp.getBytesPerSecond().getCount() == 0)
					&& (edsGridWebApp.getBytesPerSecond().getIcon().equals(Icon.NA)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((edsGridWebApp.getPacketsPerSecond().getCount() == 0)
					&& (edsGridWebApp.getPacketsPerSecond().getIcon().equals(Icon.NA)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return ((edsGridWebApp.getOpPerSecond().getCount() == 0)
					&& (edsGridWebApp.getOpPerSecond().getIcon().equals(Icon.NA)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
	}
	
	private class mirrorAppDeployer implements Runnable {

		@Override
		public void run() {
			
			LogUtils.log("Deploying application with mirror : " + mirrorApp);
			
	        log("load HSQL DB on machine - "+ToStringUtils.machineToString(machine));
	        hsqlId = DBUtils.loadHSQLDB(machine, "ParallelDeplymentTest", HSQL_DB_PORT);
	        LogUtils.log("Loaded HSQL successfully, id ["+hsqlId+"]");
	        
	        log("deploy mirror via GSM");
	        DeploymentUtils.prepareApp("MHEDS");
			mirror = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "mirror")).
	                setContextProperty("port", String.valueOf(HSQL_DB_PORT)).setContextProperty("host", machine.getHostAddress()).setContextProperty("com.gs.application", mirrorApp));
			ProcessingUnitUtils.waitForDeploymentStatus(mirror, DeploymentStatus.INTACT);

	        log("deploy runtime(1,1) via GSM");
	        runtime = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "runtime")).
	                numberOfInstances(1).numberOfBackups(1).maxInstancesPerVM(0).setContextProperty("port", String.valueOf(HSQL_DB_PORT)).
	                setContextProperty("host", machine.getHostAddress()).setContextProperty("com.gs.application", mirrorApp));
	        runtime.waitFor(runtime.getTotalNumberOfInstances());
	        ProcessingUnitUtils.waitForDeploymentStatus(runtime, DeploymentStatus.INTACT);
	        
	        log("deploy loader via GSM");
	        loader = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "loader"))
	        .setContextProperty("accounts", String.valueOf(10000)).setContextProperty("delay", "100").setContextProperty("com.gs.application", mirrorApp));
	        ProcessingUnitUtils.waitForDeploymentStatus(loader, DeploymentStatus.INTACT);
	        mirrorLatch.countDown();
			
		}
		
	}
	
	private class webAppDeployer implements Runnable {

		@Override
		public void run() {
			
	        LogUtils.log("Deploying web application with remote space : " + webApp);
	        
	        LogUtils.log("deploying mySpace");
			SpaceDeployment deployment = new SpaceDeployment("mySpace").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", webApp);
			mySpacePu = gsm.deploy(deployment);
			ProcessingUnitUtils.waitForDeploymentStatus(mySpacePu, DeploymentStatus.INTACT);
	    	
			LogUtils.log("deploying web app remote");
			puSessionTest = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("session-test-remote.war")).setContextProperty("com.gs.application", webApp));
			ProcessingUnitUtils.waitForDeploymentStatus(puSessionTest, DeploymentStatus.INTACT);
			webLatch.countDown();
			
		}
		
	}

}
