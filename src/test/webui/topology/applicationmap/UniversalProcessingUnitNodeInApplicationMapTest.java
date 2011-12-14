package test.webui.topology.applicationmap;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import java.io.IOException;
import java.util.List;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.usm.USMTestUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.services.PuTreeGrid.WebUIProcessingUnit;
import test.webui.objects.services.ServicesTab;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.applicationmap.ApplicationMap;
import test.webui.objects.topology.applicationmap.ApplicationNode;
import test.webui.objects.topology.healthpanel.HealthPanel;

import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;

import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.ProcessingUnitUtils;

public class UniversalProcessingUnitNodeInApplicationMapTest extends AbstractSeleniumTest {

	Machine machineA;
	private String processName = "cassandra";
	Service cassandra;
	
    @BeforeMethod(alwaysRun = true)
    public void setup() throws IOException, PackagingException {
        //1 GSM and 1 GSC at 1 machines
        log("waiting for 1 machine");
        admin.getMachines().waitFor(1);

        log("waiting for 1 GSA");
        admin.getGridServiceAgents().waitFor(1);

        GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
        GridServiceAgent gsaA = agents[0];

        machineA = gsaA.getMachine();

        //Start GSM A, GSC A
        log("starting: 1 GSM and 1 GSC at 1 machines");
        loadGSM(machineA); //GSM A
        loadGSCs(machineA, 1); //GSC A
        
		String testGroups = AdminUtils.getTestGroups();
		System.setProperty("com.gs.jini_lus.groups", testGroups);
        
		cassandra = USMTestUtils.usmDeploy(processName);

        ProcessingUnit pu = admin.getProcessingUnits().waitFor(cassandra.getName());
        pu.waitFor(pu.getTotalNumberOfInstances());
        ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);

    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void universalPuTest() throws InterruptedException {
		
    	int waitingTime = 5000;
    	
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		ServicesTab servicesTab = loginPage.login().switchToServices();
		
		WebUIProcessingUnit usmPu = servicesTab.getPuTreeGrid().getProcessingUnit(cassandra.getName());
		
		assertTrue(usmPu != null);
		
		// get new topology tab
		TopologyTab topologyTab = servicesTab.switchToTopology();
		
		final ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		ApplicationNode testNode = applicationMap.getApplicationNode(cassandra.getName());
		
		assertNotNull(testNode);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				ApplicationNode testNode = applicationMap.getApplicationNode(cassandra.getName());
				return (testNode.getStatus().equals(DeploymentStatus.INTACT));
			}
		}; 
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		assertTrue(testNode.getPlannedInstances() == 1);
		assertTrue(testNode.getPlannedInstances() == testNode.getActualInstances());
		assertTrue(testNode.getNodeType().equals("PROCESSING_UNIT"));
		assertTrue(testNode.getPuType().equals("UNIVERSAL"));
		List<String> components = testNode.getComponents();
		assertTrue(components.contains("custom"));
		assertTrue(components.size() == 1);
		assertTrue(testNode.getxPosition() == 4);
		
		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();
		
		// check the correct metrics are shown
		assertTrue(healthPanel.getMetric("Total Process Virtual Memory") != null);
		assertTrue(healthPanel.getMetric("Process Cpu Usage") != null);
		assertTrue(healthPanel.getMetric("Completed Tasks") != null);
		assertTrue(healthPanel.getMetric("Pending Tasks") != null);
		assertTrue(healthPanel.getMetric("Column Family In Progress") != null);
	}

}
