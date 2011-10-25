package test.webui.dashboard.services;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.io.File;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.usm.USMTestUtils;
import test.utils.AssertUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.ProcessingUnitUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.MainNavigation;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.ServicesGrid.ApplicationServicesGrid;
import test.webui.objects.topology.ApplicationMap.ServiceTypes;

public class USMTierTypeInfrastructureServicesGridTest extends AbstractSeleniumTest {

	Machine machine;
	ProcessingUnit simple;
	File usmJar;
	GridServiceManager gsm;
	
    @BeforeMethod(alwaysRun = true)
    public void setup() {

        log("waiting for 1 machine");
        admin.getMachines().waitFor(1);

        log("waiting for 1 GSA");
        admin.getGridServiceAgents().waitFor(1);

        GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
        GridServiceAgent gsa = agents[0];

        machine = gsa.getMachine();

        log("starting: 1 GSM and 1 GSC at 1 machines");
        gsm = loadGSM(machine);
        loadGSCs(machine, 1);

		usmJar = USMTestUtils.usmCreateJar("/apps/USM/usm/cassandra");
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify"})
	public void universalPuTest() throws InterruptedException {
		
    	int waitingTime = 5000;
    	
		LoginPage loginPage = getLoginPage();
		
		MainNavigation mainNavigation = loginPage.login();
		DashboardTab dashboardTab = mainNavigation.switchToDashboard();
		final ApplicationServicesGrid applicationServicesGrid = dashboardTab.getServicesGrid().getApplicationServicesGrid();

/////
		ProcessingUnit deployment = gsm.deploy(new ProcessingUnitDeployment(
				usmJar).setContextProperty(SUB_TYPE_CONTEXT_PROPERTY,
				ServiceTypes.LOAD_BALANCER.toString()));        
		simple = admin.getProcessingUnits().waitFor(deployment.getName());
        ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.INTACT);

        RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getLoadBalancerModule().getCount() == 1;
			}
		}; 
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

		simple.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.UNDEPLOYED);
/////		
		deployment = gsm.deploy(new ProcessingUnitDeployment(
				usmJar).setContextProperty(SUB_TYPE_CONTEXT_PROPERTY,
				ServiceTypes.WEB_SERVER.toString()));        
		simple = admin.getProcessingUnits().waitFor(deployment.getName());
        ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.INTACT);
		
        condition = new RepetitiveConditionProvider() {	
        	@Override
        	public boolean getCondition() {
        		return applicationServicesGrid.getWebServerModule().getCount() == 1;
        	}
        }; 
        AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
        
		simple.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.UNDEPLOYED);
/////		
		deployment = gsm.deploy(new ProcessingUnitDeployment(
				usmJar).setContextProperty(SUB_TYPE_CONTEXT_PROPERTY,
						ServiceTypes.SECURITY_SERVER.toString()));        
		simple = admin.getProcessingUnits().waitFor(deployment.getName());
		ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.INTACT);
		
		condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getSecurityServerModule().getCount() == 1;
			}
		}; 
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		simple.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.UNDEPLOYED);
/////		
		deployment = gsm.deploy(new ProcessingUnitDeployment(
				usmJar).setContextProperty(SUB_TYPE_CONTEXT_PROPERTY,
				ServiceTypes.APP_SERVER.toString()));        
		simple = admin.getProcessingUnits().waitFor(deployment.getName());
        ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.INTACT);
		
        condition = new RepetitiveConditionProvider() {	
        	@Override
        	public boolean getCondition() {
        		return applicationServicesGrid.getAppServerModule().getCount() == 1;
        	}
        }; 
        AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

        simple.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.UNDEPLOYED);
/////		
		deployment = gsm.deploy(new ProcessingUnitDeployment(
				usmJar).setContextProperty(SUB_TYPE_CONTEXT_PROPERTY,
				ServiceTypes.ESB_SERVER.toString()));        
		simple = admin.getProcessingUnits().waitFor(deployment.getName());
        ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.INTACT);
		
        condition = new RepetitiveConditionProvider() {	
        	@Override
        	public boolean getCondition() {
        		return applicationServicesGrid.getEsbServerModule().getCount() == 1;
        	}
        }; 
        AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
        
		simple.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.UNDEPLOYED);
/////		
		deployment = gsm.deploy(new ProcessingUnitDeployment(
				usmJar).setContextProperty(SUB_TYPE_CONTEXT_PROPERTY,
						ServiceTypes.MESSAGE_BUS.toString()));        
		simple = admin.getProcessingUnits().waitFor(deployment.getName());
		ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.INTACT);
		
		condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getMessageBusModule().getCount() == 1;
			}
		}; 
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		simple.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.UNDEPLOYED);
/////		
		deployment = gsm.deploy(new ProcessingUnitDeployment(
				usmJar).setContextProperty(SUB_TYPE_CONTEXT_PROPERTY,
						ServiceTypes.DATABASE.toString()));        
		simple = admin.getProcessingUnits().waitFor(deployment.getName());
		ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.INTACT);
		
		condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getDatabaseModule().getCount() == 1;
			}
		}; 
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		simple.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.UNDEPLOYED);
/////
		deployment = gsm.deploy(new ProcessingUnitDeployment(
				usmJar).setContextProperty(SUB_TYPE_CONTEXT_PROPERTY,
				ServiceTypes.NOSQL_DB.toString()));        
		simple = admin.getProcessingUnits().waitFor(deployment.getName());
        ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.INTACT);
		
        condition = new RepetitiveConditionProvider() {	
        	@Override
        	public boolean getCondition() {
        		return applicationServicesGrid.getNoSqlDbModule().getCount() == 1;
        	}
        }; 
        AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

		simple.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.UNDEPLOYED);

    }

}
