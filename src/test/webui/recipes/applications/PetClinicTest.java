package test.webui.recipes.applications;

import java.util.List;

import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.objects.LoginPage;
import test.webui.objects.MainNavigation;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.ServicesGrid.ApplicationServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationsMenuPanel;
import test.webui.objects.dashboard.ServicesGrid.Icon;
import test.webui.objects.services.PuTreeGrid;
import test.webui.objects.services.ServicesTab;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.ApplicationMap.ApplicationNode.Connector;
import test.webui.recipes.services.AbstractSeleniumServiceRecipeTest;

public class PetClinicTest extends AbstractSeleniumApplicationRecipeTest {
	
	private Machine machineA;
	private ProcessingUnit pu;

	@Override
	@BeforeMethod(alwaysRun = true)
	public void beforeTest() {
		setCurrentRecipe("petclinic");
		super.beforeTest();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4)
	public void petClinicDemoTest() throws Exception {
		
		// get new login page
		LoginPage loginPage = getLoginPage();

		MainNavigation mainNav = loginPage.login();

		DashboardTab dashboardTab = mainNav.switchToDashboard();

		ApplicationsMenuPanel appMenuPanel = dashboardTab.getServicesGrid().getApplicationsMenuPanel();

		appMenuPanel.selectApplication(AbstractSeleniumServiceRecipeTest.MANAGEMENT);

		final ApplicationServicesGrid applicationServices = dashboardTab.getServicesGrid().getApplicationServicesGrid();

		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				return ((applicationServices.getWebModule().getCount() == 2)
						&& (applicationServices.getWebModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		appMenuPanel.selectApplication("petclinic-mongo");
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				return ((applicationServices.getWebModule().getCount() == 1)
						&& (applicationServices.getWebModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				return ((applicationServices.getNoSqlDbModule().getCount() == 3)
						&& (applicationServices.getNoSqlDbModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		TopologyTab topologyTab = mainNav.switchToTopology();

		final ApplicationMap appMap = topologyTab.getApplicationMap();

		appMap.selectApplication(AbstractSeleniumServiceRecipeTest.MANAGEMENT);

		takeScreenShot(this.getClass(), "management-application");

		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode restNode = appMap.getApplicationNode("rest");
				return ((restNode != null) && (restNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");

		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode webuiNode = appMap.getApplicationNode("webui");
				return ((webuiNode != null) && (webuiNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");
		
		takeScreenShot(this.getClass(), "passed-topology");
		
		appMap.selectApplication("petclinic-mongo");
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode mongodNode = appMap.getApplicationNode("mongod");
				return ((mongodNode != null) && (mongodNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode mongosNode = appMap.getApplicationNode("mongos");
				return ((mongosNode != null) && (mongosNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode mongocfgNode = appMap.getApplicationNode("mongo-cfg");
				return ((mongocfgNode != null) && (mongocfgNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");	
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode tomcatNode = appMap.getApplicationNode("tomcat");
				return ((tomcatNode != null) && (tomcatNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");
		
		
		List<Connector> tomcatConnectors = appMap.getApplicationNode("tomcat").getConnectors();
		assertTrue(tomcatConnectors.size() == 1);
		assertTrue(tomcatConnectors.get(0).getTarget().getName().equals("mongos"));

		List<Connector> mongosConnectors = appMap.getApplicationNode("mongos").getConnectors();
		assertTrue(mongosConnectors.size() == 2);
		for (Connector c : mongosConnectors) {
			String name = c.getTarget().getName();
			assertTrue(name.equals("mongod") ||name.equals("mongo-cfg"));
		}

		ServicesTab servicesTab = mainNav.switchToServices();
		
		PuTreeGrid puTreeGrid = servicesTab.getPuTreeGrid();
		
		assertTrue(puTreeGrid.getProcessingUnit("tomcat") != null);
		assertTrue(puTreeGrid.getProcessingUnit("mongod") != null);
		assertTrue(puTreeGrid.getProcessingUnit("mongos") != null);
		assertTrue(puTreeGrid.getProcessingUnit("mongo-cfg") != null);
		
		takeScreenShot(this.getClass(), "passed-services");

	}

}
