package test.webui.topology.applicationmap;

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
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.MainNavigation;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.ServicesGrid.InfrastructureServicesGrid;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.TopologyTab;

public class FirstApplicationSelectedTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit pu;
	GridServiceManager gsmA;
	
    @BeforeMethod(alwaysRun = true)
    public void setup() {
        //1 GSM and 1 GSC at 1 machines
        log("waiting for 1 machine");
        admin.getMachines().waitFor(1);

        log("waiting for 1 GSA");
        admin.getGridServiceAgents().waitFor(1);

        GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
        GridServiceAgent gsaA = agents[0];

        machineA = gsaA.getMachine();

        log("starting: 1 GSM and 1 GSC at 1 machines");
        gsmA = loadGSM(machineA); 
        loadGSCs(machineA, 2);
		
		LogUtils.log("deploying processing unit...");
		SpaceDeployment  deployment = new SpaceDeployment("Test2").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", "MyApp");
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
    public void firstApplicationTest() throws InterruptedException {
    	
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		
		MainNavigation mainView = loginPage.login();
		
		DashboardTab dashboardTab = mainView.switchToDashboard();
		
		final InfrastructureServicesGrid infrastructureGrid = dashboardTab.getServicesGrid().getInfrastructureGrid();
		
		final int machines = admin.getGridServiceAgents().getAgents().length;
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				return ((infrastructureGrid.getGSAInst().getCount() == machines)
						&& (infrastructureGrid.getHosts().getCount() == machines));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		TopologyTab topologyTab = mainView.switchToTopology();
		
		final ApplicationMap appMap = topologyTab.getApplicationMap();
		
		condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				ApplicationNode node = appMap.getApplicationNode("Test2");
				return (node != null);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, 5000);
    	
    }
    

}
