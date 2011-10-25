package test.webui.loadbalancer;

import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.MainNavigation;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.ServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationsMenuPanel;
import test.webui.objects.dashboard.ServicesGrid.Icon;
import test.webui.objects.dashboard.ServicesGrid.InfrastructureServicesGrid;
import test.webui.objects.services.PuTreeGrid.WebUIProcessingUnit;
import test.webui.objects.services.ServicesTab;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.TopologyTab;
import framework.tools.SGTestHelper;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.ScriptUtils;
import framework.utils.WebUiUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

public class WebServerFailOverTest extends AbstractSeleniumTest {
	
	@Override
    @BeforeMethod(alwaysRun = true)
    public void beforeTest() {
		
		// replace balancer-template in build
		String oldFile = ScriptUtils.getBuildPath() + "/tools/apache/balancer-template.vm";
		String newFile = SGTestHelper.getSGTestRootDir() + "/src/test/webui/resources/balancer-template.vm";
		
		super.createAdmin();
		
		try {
			WebUiUtils.replaceFile(oldFile, newFile);
			startLoadBalancerWebServer();
			Thread.sleep(30000);
			startWebBrowser(baseUrlApache);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void failoverTest() throws InterruptedException {
		
		String methodName = "failoverTest";
		
		int waitingTime = 10000;
		
		LoginPage loginPage = getLoginPage();
		
		MainNavigation main = loginPage.login();
		
		loginPage.assertLoginPerformed();
		
		DashboardTab dashboardTab = main.switchToDashboard();
		
		ServicesGrid appGrid = dashboardTab.getServicesGrid();
		
		ApplicationsMenuPanel applicationPanel = appGrid.getApplicationsMenuPanel();
		
		applicationPanel.selectApplication("gs-webui");
		
		LogUtils.log("killing a gsc");
		admin.getGridServiceContainers().getContainers()[0].kill();
		admin.getGridServiceContainers().waitFor(1);
		
		final InfrastructureServicesGrid infrastructureGrid = appGrid.getInfrastructureGrid();
			
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructureGrid.getGSCInst().getCount() == 1);
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), methodName);
		
		final ApplicationServicesGrid applicationServices = appGrid.getApplicationServicesGrid();
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (applicationServices.getStatefullModule().getIcon().equals(Icon.CRITICAL));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), methodName);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (applicationServices.getWebModule().getIcon().equals(Icon.CRITICAL));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), methodName);
		
		TopologyTab topologyTab = dashboardTab.switchToTopology();
		
		final ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		applicationMap.selectApplication("gs-webui");
		
		takeScreenShot(this.getClass(), methodName);
		
		ApplicationNode gswebuiNode = applicationMap.getApplicationNode("gs-webui");
		
		assertTrue(gswebuiNode.getStatus().equals(DeploymentStatus.COMPROMISED));
		assertTrue(gswebuiNode.getActualInstances() == 1);
		assertTrue(gswebuiNode.getPlannedInstances() == 2);
		
		ApplicationNode spaceNode = applicationMap.getApplicationNode("webSpace");
		
		assertTrue(spaceNode.getStatus().equals(DeploymentStatus.COMPROMISED));
		assertTrue(spaceNode.getActualInstances() == 1);
		assertTrue(spaceNode.getPlannedInstances() == 2);
		
		LogUtils.log("deploying processing unit...");
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 0).maxInstancesPerVM(1)
			.setContextProperty("com.gs.application", "gs-webui");
		ProcessingUnit space = webUIGSM.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(space, DeploymentStatus.INTACT);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				ApplicationNode spaceNode = applicationMap.getApplicationNode("Test");
				DeploymentStatus status = spaceNode.getStatus();
				return ( (status != null) && (status.equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), methodName);
		
		takeScreenShot(this.getClass(), methodName);

		ServicesTab servicesTab = topologyTab.switchToServices();
		
		WebUIProcessingUnit testPu = servicesTab.getPuTreeGrid().getProcessingUnit("Test");
		assertTrue(testPu != null);
		assertTrue(testPu.getStatus().equals(DeploymentStatus.INTACT));
		assertTrue(testPu.getPlannedInstances() == 1);
		assertTrue(testPu.getActualInstances() == 1);
		
	}
	
	@Override
	@AfterMethod(alwaysRun = true)
	public void afterTest() {
		try {
			stopLoadBalancerWebServer();
			stopWebBrowser();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		super.closeAdmin();
	}

}
