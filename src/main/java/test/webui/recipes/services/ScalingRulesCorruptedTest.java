package test.webui.recipes.services;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.internal.pu.InternalProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.MainNavigation;
import com.gigaspaces.webuitf.dashboard.DashboardTab;
import com.gigaspaces.webuitf.dashboard.alerts.AlertsPanel;

import framework.utils.IOUtils;
import framework.utils.LogUtils;

public class ScalingRulesCorruptedTest extends AbstractSeleniumServiceRecipeTest {

	private final static String SERVICE_NAME = "customServiceMonitor";
	
	private static final String SERVICE_RELATIVE_PATH = "apps/cloudify/recipes/" + SERVICE_NAME;
	
	private File customServiceMonitorOriginalDSLFile;

	private File customServiceMonitorBackupDSLFile;
	
	@BeforeClass
	public void setApplicationPath() {
		super.setPathToServiceRelativeToSGTestRootDir(SERVICE_NAME, SERVICE_RELATIVE_PATH);
	}

	@Override
	@BeforeMethod
	public void install() throws IOException, InterruptedException {
		
	}
	
	@AfterMethod(alwaysRun = true)
	public void restoreDslFile() throws IOException {
		FileUtils.copyFile(customServiceMonitorBackupDSLFile, customServiceMonitorOriginalDSLFile);
		FileUtils.deleteQuietly(customServiceMonitorBackupDSLFile);
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void testValueIsAString() throws IOException, InterruptedException {
		
		replaceInServiceFile("value 90", "value \"a\"");
	
		super.install();
		
		// get new login page
		
		LoginPage loginPage = getLoginPage();

		MainNavigation mainNav = loginPage.login();

		DashboardTab dashboardTab = mainNav.switchToDashboard();
		
		AlertsPanel alertsPanel = dashboardTab.getDashboardSubPanel().switchToAlertsPanel();
		
		final InternalProcessingUnit pu = (InternalProcessingUnit) admin.getProcessingUnits().waitFor(DEFAULT_APPLICATION_NAME + "." + SERVICE_NAME,OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
		
		repetitiveAssertNumberOfInstances(pu, 2);
		
		setStatistics(pu, 2, 100);
		
		alertsPanel.waitForAlerts(AlertStatus.RAISED, "Automatic Scaling Alert", 1);
		
	}
	
	/**
	 * Disabled becuase of CLOUDIFY-737
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void testIncreaseIsNegetive() throws IOException, InterruptedException {
		
		replaceInServiceFile("instancesIncrease 1", "instancesIncrease -1");
	
		super.install();
		
		// get new login page
		
		LoginPage loginPage = getLoginPage();

		MainNavigation mainNav = loginPage.login();

		DashboardTab dashboardTab = mainNav.switchToDashboard();
		
		AlertsPanel alertsPanel = dashboardTab.getDashboardSubPanel().switchToAlertsPanel();
		
		final InternalProcessingUnit pu = (InternalProcessingUnit) admin.getProcessingUnits().waitFor(DEFAULT_APPLICATION_NAME + "." + SERVICE_NAME,OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
		
		repetitiveAssertNumberOfInstances(pu, 2);
		
		setStatistics(pu, 2, 100);
		
		alertsPanel.waitForAlerts(AlertStatus.RAISED, "Automatic Scaling Alert", 1);
		
	}

	
	
	
	
	private void setStatistics(final InternalProcessingUnit pu, final int expectedNumberOfInstances, long value) throws IOException, InterruptedException {
		
		ProcessingUnitInstance[] instances = repetitiveAssertNumberOfInstances(pu, expectedNumberOfInstances);
		
		for (ProcessingUnitInstance instance : instances) {
			String command = "connect localhost;invoke -instanceid " + instance.getInstanceId() + " --verbose " + SERVICE_NAME + " set " + value;
			String output = CommandTestUtils.runCommandAndWait(command);
			LogUtils.log(output);
		}
		
	}
	
	private void replaceInServiceFile(String whatToReplace, String whatToReplaceTo) throws IOException {

		String customServiceMonitorOriginalDSLPath = CommandTestUtils.getPath("apps/cloudify/recipes/" + SERVICE_NAME);
		customServiceMonitorOriginalDSLFile = new File(customServiceMonitorOriginalDSLPath + "/" + SERVICE_NAME + "-service.groovy");
		customServiceMonitorBackupDSLFile = new File(customServiceMonitorOriginalDSLPath + "/" + SERVICE_NAME + "-backup.groovy");

		FileUtils.copyFile(customServiceMonitorOriginalDSLFile, customServiceMonitorBackupDSLFile);

		Map<String, String> props = new HashMap<String, String>();
		props.put(whatToReplace, whatToReplaceTo);
		IOUtils.replaceTextInFile(customServiceMonitorOriginalDSLFile, props);
	}


}
