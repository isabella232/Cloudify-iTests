package test.webui.topology.applicationmap;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

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
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.applicationmap.ApplicationMap;
import test.webui.objects.topology.applicationmap.ApplicationMap.ServiceTypes;
import test.webui.objects.topology.applicationmap.ApplicationNode;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.ProcessingUnitUtils;

public class BasicUniversalSubtypeNodeInApplicationMapTest extends AbstractSeleniumTest {

	Machine machineA;
	ProcessingUnit simple;
	File usmJar;
	GridServiceManager gsmA;
	
    @BeforeMethod(alwaysRun = true)
    public void setup() {
    	
        log("waiting for 1 machine");
        admin.getMachines().waitFor(1);

        log("waiting for 1 GSA");
        admin.getGridServiceAgents().waitFor(1);

        GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
        GridServiceAgent gsaA = agents[0];

        machineA = gsaA.getMachine();

        log("starting: 1 GSM and 1 GSC at 1 machines");
        gsmA = loadGSM(machineA); //GSM A
        loadGSCs(machineA, 1);
        
		usmJar = USMTestUtils.usmCreateJar("/apps/USM/usm/cassandra");
		// TODO ask barak why no other app is working besides cassandra
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void universalPuTest() throws InterruptedException {
		
    	int waitingTime = 5000;
    	
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		ProcessingUnit deployment = gsmA.deploy(new ProcessingUnitDeployment(
				usmJar).setContextProperty(SUB_TYPE_CONTEXT_PROPERTY,
				ServiceTypes.LOAD_BALANCER.toString()));        
		simple = admin.getProcessingUnits().waitFor(deployment.getName());
        ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.INTACT);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				ApplicationNode testNode = applicationMap.getApplicationNode(simple.getName());
				return (testNode.getStatus().equals(DeploymentStatus.INTACT));
			}
		}; 
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		ApplicationNode simpleNode = applicationMap.getApplicationNode(simple.getName());
		assertTrue(simpleNode.getNodeType().equals("PROCESSING_UNIT"));
		assertTrue(simpleNode.getPuType().equals("LOAD_BALANCER"));
		assertTrue(simpleNode.getxPosition() == 0);
		
		simple.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.UNDEPLOYED);
		
		condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				ApplicationNode testNode = applicationMap.getApplicationNode(simple.getName());
				return (testNode == null);
			}
		}; 
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		
		deployment = gsmA.deploy(new ProcessingUnitDeployment(
				usmJar).setContextProperty(SUB_TYPE_CONTEXT_PROPERTY,
				ServiceTypes.WEB_SERVER.toString()));        
		simple = admin.getProcessingUnits().waitFor(deployment.getName());
        ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.INTACT);
		
		condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				ApplicationNode testNode = applicationMap.getApplicationNode(simple.getName());
				return (testNode.getStatus().equals(DeploymentStatus.INTACT));
			}
		}; 
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		simpleNode = applicationMap.getApplicationNode(simple.getName());
		assertTrue(simpleNode.getNodeType().equals("PROCESSING_UNIT"));
		assertTrue(simpleNode.getPuType().equals("WEB_SERVER"));
		assertTrue(simpleNode.getxPosition() == 1);
		
		simple.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.UNDEPLOYED);
		
		condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				ApplicationNode testNode = applicationMap.getApplicationNode(simple.getName());
				return (testNode == null);
			}
		}; 
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		deployment = gsmA.deploy(new ProcessingUnitDeployment(
				usmJar).setContextProperty(SUB_TYPE_CONTEXT_PROPERTY,
				ServiceTypes.APP_SERVER.toString()));        
		simple = admin.getProcessingUnits().waitFor(deployment.getName());
        ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.INTACT);
		
		condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				ApplicationNode testNode = applicationMap.getApplicationNode(simple.getName());
				return (testNode.getStatus().equals(DeploymentStatus.INTACT));
			}
		}; 
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		simpleNode = applicationMap.getApplicationNode(simple.getName());
		assertTrue(simpleNode.getNodeType().equals("PROCESSING_UNIT"));
		assertTrue(simpleNode.getPuType().equals("APP_SERVER"));
		assertTrue(simpleNode.getxPosition() == 2);
		
		simple.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.UNDEPLOYED);
		
		condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				ApplicationNode testNode = applicationMap.getApplicationNode(simple.getName());
				return (testNode == null);
			}
		}; 
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		deployment = gsmA.deploy(new ProcessingUnitDeployment(
				usmJar).setContextProperty(SUB_TYPE_CONTEXT_PROPERTY,
				ServiceTypes.ESB_SERVER.toString()));        
		simple = admin.getProcessingUnits().waitFor(deployment.getName());
        ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.INTACT);
		
		condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				ApplicationNode testNode = applicationMap.getApplicationNode(simple.getName());
				return (testNode.getStatus().equals(DeploymentStatus.INTACT));
			}
		}; 
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		simpleNode = applicationMap.getApplicationNode(simple.getName());
		assertTrue(simpleNode.getNodeType().equals("PROCESSING_UNIT"));
		assertTrue(simpleNode.getPuType().equals("ESB_SERVER"));
		assertTrue(simpleNode.getxPosition() == 3);
		
		simple.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.UNDEPLOYED);
		
		condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				ApplicationNode testNode = applicationMap.getApplicationNode(simple.getName());
				return (testNode == null);
			}
		}; 
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		deployment = gsmA.deploy(new ProcessingUnitDeployment(
				usmJar).setContextProperty(SUB_TYPE_CONTEXT_PROPERTY,
				ServiceTypes.NOSQL_DB.toString()));        
		simple = admin.getProcessingUnits().waitFor(deployment.getName());
        ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.INTACT);
		
		condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				ApplicationNode testNode = applicationMap.getApplicationNode(simple.getName());
				return (testNode.getStatus().equals(DeploymentStatus.INTACT));
			}
		}; 
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		simpleNode = applicationMap.getApplicationNode(simple.getName());
		assertTrue(simpleNode.getNodeType().equals("PROCESSING_UNIT"));
		assertTrue(simpleNode.getPuType().equals("NOSQL_DB"));
		assertTrue(simpleNode.getxPosition() == 4);
		
		simple.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.UNDEPLOYED);
		
		condition = new RepetitiveConditionProvider() {	
			@Override
			public boolean getCondition() {
				ApplicationNode testNode = applicationMap.getApplicationNode(simple.getName());
				return (testNode == null);
			}
		}; 
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
	}

}
