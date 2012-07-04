package test.webui.recipes.applications;

import java.io.IOException;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.MainNavigation;
import com.gigaspaces.webuitf.WebConstants;
import com.gigaspaces.webuitf.topology.TopologyTab;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationMap;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationNode;
import com.gigaspaces.webuitf.topology.logspanel.LogsMachine;
import com.gigaspaces.webuitf.topology.logspanel.LogsPanel;
import com.gigaspaces.webuitf.topology.logspanel.PuLogsPanelService;

import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.ProcessingUnitUtils;

public class TerminateServiceContainerLogsPanelTest extends AbstractSeleniumApplicationRecipeTest {
	
	@Override
	@BeforeMethod
	public void install() throws IOException, InterruptedException {
		setBrowser(WebConstants.CHROME);
		setCurrentApplication("travel");
		super.install();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2 , enabled = true)
	public void terminateContainerTest() throws InterruptedException, IOException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		MainNavigation mainNav = loginPage.login();

		TopologyTab topology = mainNav.switchToTopology();
		
		ApplicationMap appMap = topology.getApplicationMap();
		
		appMap.selectApplication("travel");
		
		ApplicationNode travelNode = appMap.getApplicationNode("tomcat");
		
		travelNode.select();
		
		ProcessingUnit travelPu = admin.getProcessingUnits().getProcessingUnit("travel.tomcat");
		
		final GridServiceContainer travelContainer = travelPu.getInstances()[0].getGridServiceContainer();
		
		LogsPanel logsPanel = topology.getTopologySubPanel().switchToLogsPanel();
		
		PuLogsPanelService travelLogsService = logsPanel.getPuLogsPanelService("travel.tomcat");
		
		Machine localHost = travelContainer.getMachine();
		
		final LogsMachine logsLocalHost = travelLogsService.getMachine(localHost,travelPu);
		
		assertTrue(logsLocalHost.containsGridServiceContainer(travelContainer));
		
		int gscAgentId = travelContainer.getAgentId();
		
		travelContainer.kill();
		
		mainNav.switchToTopology();
		
		travelNode.select();
		
		ProcessingUnitUtils.waitForDeploymentStatus(travelPu, DeploymentStatus.SCHEDULED);
		ProcessingUnitUtils.waitForDeploymentStatus(travelPu, DeploymentStatus.INTACT);
		
		appMap.deselectAllNodes();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				return (!logsLocalHost.containsGridServiceContainer(travelContainer));
			}
		};
		
		AssertUtils.repetitiveAssertTrue("Container" + gscAgentId + "is still present", condition, waitingTime);
		uninstallApplication("travel", true);
		
	}

}
