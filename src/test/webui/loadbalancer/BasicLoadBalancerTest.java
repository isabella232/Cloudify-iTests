package test.webui.loadbalancer;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.AssertUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.ScriptUtils;
import test.utils.WebUiUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.MainNavigation;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.ServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationsMenuPanel;
import test.webui.objects.dashboard.ServicesGrid.InfrastructureServicesGrid;
import framework.tools.SGTestHelper;

public class BasicLoadBalancerTest extends AbstractSeleniumTest {
	
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
	public void testLoadBalancer() throws InterruptedException {
		
		final int machines = admin.getMachines().getMachines().length;
		
		int waitingTime = 10000;
		
		LoginPage loginPage = getLoginPage();
		
		MainNavigation main = loginPage.login();
		
		loginPage.assertLoginPerformed();
		
		DashboardTab dashboardTab = main.switchToDashboard();
		
		ServicesGrid appGrid = dashboardTab.getServicesGrid();
		
		ApplicationsMenuPanel applicationPanel = appGrid.getApplicationsMenuPanel();
		
		applicationPanel.selectApplication("gs-webui");
		
		final InfrastructureServicesGrid infrastructureGrid = appGrid.getInfrastructureGrid();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructureGrid.getHosts().getCount() == 1);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructureGrid.getLUSInst().getCount() == machines);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructureGrid.getGSCInst().getCount() == 2);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructureGrid.getGSMInst().getCount() == 1);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (infrastructureGrid.getGSAInst().getCount() == 1);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		final ApplicationServicesGrid applicationServices = appGrid.getApplicationServicesGrid();
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (applicationServices.getStatefullModule().getCount() == 1);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {			
			public boolean getCondition() {
				return (applicationServices.getWebModule().getCount() == 1);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
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
