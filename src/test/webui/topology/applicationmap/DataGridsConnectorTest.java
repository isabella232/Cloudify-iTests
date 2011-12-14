package test.webui.topology.applicationmap;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import java.util.List;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.applicationmap.ApplicationMap;
import test.webui.objects.topology.applicationmap.ApplicationNode;
import test.webui.objects.topology.applicationmap.Connector;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

public class DataGridsConnectorTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit pu;
	private String dataGridConnectorsList = "[Test2]";
	private String DEPENDS_ON_PROPERTY = "com.gs.application.dependsOn";
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
		
		LogUtils.log("deploying processing unit...");
		SpaceDeployment  deployment = new SpaceDeployment("Test2").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", "MyApp");
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void dataGridConnectorTest() throws InterruptedException {
    	
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		applicationMap.selectApplication("MyApp");
		
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", "MyApp").setContextProperty(DEPENDS_ON_PROPERTY, dataGridConnectorsList);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
		ApplicationNode test = applicationMap.getApplicationNode("Test");
		
		List<Connector> connectors = test.getConnectors();
		
		assertTrue(connectors.size() == 1);
		
		Connector connector = connectors.get(0);
		
		assertTrue(connector.getTarget().getName().equals("Test2"));
		assertTrue(connector.getSource().getName().equals(test.getName()));
    	
    }

}
