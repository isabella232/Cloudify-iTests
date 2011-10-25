package test.webui.dashboard.services;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AssertUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.ServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.DataReplicationGrid;
import test.webui.objects.dashboard.ServicesGrid.Icon;
import test.webui.objects.dashboard.ServicesGrid.InfrastructureServicesGrid;

public class ServicesGridTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit pu;
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
		
		// deploy a pu
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 1).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void applicationsGrid() throws InterruptedException {
		 
		 final int machines = admin.getMachines().getMachines().length;
		
		 int waitingTime = 20000;
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		DashboardTab dashboardTab = loginPage.login().switchToDashboard();
		
		ServicesGrid appGrid = dashboardTab.getServicesGrid();
		
		final InfrastructureServicesGrid resourceGrid = appGrid.getInfrastructureGrid();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (resourceGrid.getHosts().getCount() == machines);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (resourceGrid.getGSAInst().getCount() == machines);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(resourceGrid.getGSAInst().getIcon().equals(Icon.OK));
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (resourceGrid.getGSMInst().getCount() == 1);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(resourceGrid.getGSMInst().getIcon().equals(Icon.OK));
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (resourceGrid.getGSCInst().getCount() == 2);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(resourceGrid.getGSCInst().getIcon().equals(Icon.OK));
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (resourceGrid.getLUSInst().getCount() == machines);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(resourceGrid.getLUSInst().getIcon().equals(Icon.OK));
		
		final ApplicationServicesGrid modulesGrid = appGrid.getApplicationServicesGrid();
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (modulesGrid.getStatefullModule().getCount() == 1);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(modulesGrid.getStatefullModule().getIcon().equals(Icon.OK));
		
		// kill a gsc
		admin.getGridServiceContainers().getContainers()[0].kill();
		admin.getGridServiceContainers().waitFor(1);
		pu.waitFor(1);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (resourceGrid.getHosts().getCount() == machines);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (resourceGrid.getHosts().getIcon().equals(Icon.ALERT));
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (resourceGrid.getGSAInst().getCount() == machines);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(resourceGrid.getGSAInst().getIcon().equals(Icon.OK));
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (resourceGrid.getGSMInst().getCount() == 1);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(resourceGrid.getGSMInst().getIcon().equals(Icon.OK));
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (resourceGrid.getGSCInst().getCount() == 1);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (resourceGrid.getGSCInst().getIcon().equals(Icon.ALERT));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (resourceGrid.getLUSInst().getCount() == machines);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(resourceGrid.getLUSInst().getIcon().equals(Icon.OK));
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (modulesGrid.getStatefullModule().getCount() == 1);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		assertTrue(modulesGrid.getStatefullModule().getIcon().equals(Icon.CRITICAL));
		
		DataReplicationGrid hAvGrid = appGrid.getDataReplicationGrid();
		
		assertTrue(hAvGrid.getBytesPerSecond().getIcon().equals(Icon.CRITICAL));
		assertTrue(hAvGrid.getPacketsPerSecond().getIcon().equals(Icon.CRITICAL));
	}

}
