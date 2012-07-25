package test.webui.recipes.services;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.testng.annotations.Test;

import test.webui.AbstractWebUILocalCloudTest;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.MainNavigation;
import com.gigaspaces.webuitf.dashboard.DashboardTab;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.Icon;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.InfrastructureServicesGrid;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.InfrastructureServicesGrid.ESMInst;
import com.gigaspaces.webuitf.services.HostsAndServicesGrid;
import com.gigaspaces.webuitf.services.ServicesTab;
import com.gigaspaces.webuitf.topology.TopologyTab;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationMap;
import com.gigaspaces.webuitf.topology.logspanel.LogsMachine;
import com.gigaspaces.webuitf.topology.logspanel.LogsPanel;
import com.gigaspaces.webuitf.topology.logspanel.PuLogsPanelService;

import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.LogUtils;

public class GridServiceNamesTest extends AbstractWebUILocalCloudTest {
		

	private final String SERVICE_NAME = "rest";
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void nameTest() throws InterruptedException, IOException {

		// get new login page
		LoginPage loginPage = getLoginPage();

		MainNavigation mainNav = loginPage.login();

		DashboardTab dashboardTab = mainNav.switchToDashboard();
		
		final InfrastructureServicesGrid infrastructureServicesGrid = dashboardTab.getServicesGrid().getInfrastructureGrid();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				
				ESMInst esmInst = infrastructureServicesGrid.getESMInst();
				LogUtils.log( "> ESM count:" + esmInst.getCount() + ", status:" + 
								infrastructureServicesGrid.getESMInst().getIcon() );
				
				return esmInst.getCount() == 1 && 
							infrastructureServicesGrid.getESMInst().getIcon().equals( Icon.OK );
			}
		};
		
		AssertUtils.repetitiveAssertTrue("No esm in showing in the dashboard", condition, waitingTime);
		
		//end of grid service name testing in Dashboard  
		/////////////////////////////////////////
		



		//start checking grid service names in Topology/Logs 
		TopologyTab topologyTab = mainNav.switchToTopology();

		final ApplicationMap appMap = topologyTab.getApplicationMap();

//		appMap.selectApplication(MANAGEMENT_APPLICATION_NAME);

		ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit(SERVICE_NAME);
		ProcessingUnitInstance[] instances = processingUnit.getInstances();
		ProcessingUnitInstance processingUnitInstance = instances[ 0 ];
		Machine machine = processingUnitInstance.getMachine();
		
		appMap.selectApplication(MANAGEMENT_APPLICATION_NAME);
		LogsPanel logsPanel = topologyTab.getTopologySubPanel().switchToLogsPanel();
		PuLogsPanelService puLogsPanelService = logsPanel.getPuLogsPanelService( SERVICE_NAME );
		LogsMachine machineTreeNode = puLogsPanelService.getMachine( machine, processingUnit );
		List<String> services = machineTreeNode.getServices();
		LogUtils.log( Arrays.toString( services.toArray( new String[ 0 ] ) ) );

		//navigate to Services and check names of displayed grid services
		
		ServicesTab servicesTab = mainNav.switchToServices();

		HostsAndServicesGrid hostAndServicesGrid = servicesTab.getHostAndServicesGrid();
		String hostAddress = machine.getHostAddress();
		hostAndServicesGrid.clickOnHost( hostAddress );
		//gsa
		int countNumberAgents = hostAndServicesGrid.countNumberOf( "agent" );
		//gsm
		int countNumberDeployers = hostAndServicesGrid.countNumberOf( "deployer" );
		//gsc
		int countNumberUsm = hostAndServicesGrid.countNumberOf( "usm" );
		//lus
		int countNumberDiscoveryServices = hostAndServicesGrid.countNumberOf( "discovery service" );
		//esm
		int countNumberOrchestrators = hostAndServicesGrid.countNumberOf( "orchestrator" );		

//		assertTrue(puTreeGrid.getProcessingUnit("webui") != null);
//		assertTrue(puTreeGrid.getProcessingUnit("rest") != null);
//		assertTrue(puTreeGrid.getProcessingUnit(DEFAULT_HSQLDB_FULL_SERVICE_NAME) != null);

	}
}
