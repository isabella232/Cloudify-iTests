package test.webui.recipes.services.autoscaling;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.internal.pu.InternalProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.IOUtils;
import framework.utils.LogUtils;

import test.cli.cloudify.CommandTestUtils;
import test.webui.objects.LoginPage;
import test.webui.objects.MainNavigation;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.alerts.AlertsPanel;
import test.webui.recipes.services.AbstractSeleniumServiceRecipeTest;

public class ScalingRulesCorruptedTest extends AbstractSeleniumServiceRecipeTest {

	private final static String SERVICE_NAME = "customServiceMonitor";
	
	private static final String SERVICE_RELATIVE_PATH = "apps/cloudify/recipes/" + SERVICE_NAME;
	
	private final static String APPLICATION_NAME = "default";

	@Override
	@BeforeMethod
	public void install() throws IOException, InterruptedException {
		
		File customServiceMonitorNewDSLFile = null;
		try {
			String customServiceMonitorOriginalDSLPath = CommandTestUtils.getPath("apps/cloudify/recipes/" + SERVICE_NAME);
			File customServiceMonitorOriginalDSLFile = new File(customServiceMonitorOriginalDSLPath + "/" + SERVICE_NAME + "-service.groovy");
			customServiceMonitorNewDSLFile = new File(customServiceMonitorOriginalDSLPath + "/" + SERVICE_NAME + "-backup.groovy");

			FileUtils.copyFile(customServiceMonitorOriginalDSLFile, customServiceMonitorNewDSLFile);

			IOUtils.replaceTextInFile(customServiceMonitorOriginalDSLFile.getAbsolutePath(), "value 90", "value \"a\"");

			super.setPathToServiceRelativeToSGTestRootDir(SERVICE_NAME, SERVICE_RELATIVE_PATH);
			super.install();
		}
		finally {
			if ((customServiceMonitorNewDSLFile != null) && (customServiceMonitorNewDSLFile.exists())) {
				customServiceMonitorNewDSLFile.deleteOnExit();
			}
		}
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void testValueIsAString() throws IOException, InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();

		MainNavigation mainNav = loginPage.login();

		DashboardTab dashboardTab = mainNav.switchToDashboard();
		
		AlertsPanel alertsPanel = dashboardTab.getDashboardSubPanel().switchToAlertsPanel();
		
		final InternalProcessingUnit pu = (InternalProcessingUnit) admin.getProcessingUnits().waitFor(APPLICATION_NAME + "." + SERVICE_NAME,OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
		
		repetitiveAssertNumberOfInstances(pu, 2);
		
		setStatistics(pu, 2, 100);
		
		alertsPanel.waitForAlerts(AlertStatus.RAISED, AlertsPanel.AUTOMATIC_SCALING, 1);
		
	}
	
	
	
	
	private void setStatistics(final InternalProcessingUnit pu, final int expectedNumberOfInstances, long value) throws IOException, InterruptedException {
		
		ProcessingUnitInstance[] instances = repetitiveAssertNumberOfInstances(pu, expectedNumberOfInstances);
		
		for (ProcessingUnitInstance instance : instances) {
			String command = "connect localhost;invoke -instanceid " + instance.getInstanceId() + " --verbose " + SERVICE_NAME + " set " + value;
			String output = CommandTestUtils.runCommandAndWait(command);
			LogUtils.log(output);
		}
		
	}


}
