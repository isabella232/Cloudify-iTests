package test.webui.recipes;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import test.cli.cloudify.CommandTestUtils;
import test.webui.AbstractSeleniumTest;
import framework.utils.LogUtils;

public class AbstractSeleniumRecipeTest extends AbstractSeleniumTest {
	
	public static boolean bootstraped;
	public static String METRICS_ASSERTION_SUFFIX = " metric that is defined in the dsl is not displayed in the metrics panel";
	
	@BeforeSuite(alwaysRun = true)
	public void bootstrap() throws IOException, InterruptedException {
		assertTrue(bootstrapLocalCloud());
		bootstraped = true;
	}
	
	@Override
	@BeforeMethod(alwaysRun = true)
	public void beforeTest() {
		LogUtils.log("Test Configuration Started : " + this.getClass());
	}
	
	@Override
	@AfterMethod(alwaysRun = true)
	public void afterTest() {
		restorePreviousBrowser();
		LogUtils.log("Test Finished : " + this.getClass());
	}
	
	@AfterSuite(alwaysRun = true)
	public void teardown() throws IOException, InterruptedException {
		assertTrue(tearDownLocalCloud());
		bootstraped = false;
	}
	
	public void undeployNonManagementServices() {
		for (ProcessingUnit pu : admin.getProcessingUnits().getProcessingUnits()) {
			if (!pu.getName().equals("webui") && !pu.getName().equals("rest") && !pu.getName().equals("cloudifyManagementSpace")) {
				if (!pu.undeployAndWait(30, TimeUnit.SECONDS)) {
					LogUtils.log("Failed to uninstall " + pu.getName());
				}
				else {
					LogUtils.log("Uninstalled service: " + pu.getName());
				}
			}
		}
	}
	
	public boolean isBootstraped() {
		return bootstraped;
	}
	
	private boolean bootstrapLocalCloud() throws IOException, InterruptedException {
		String command = "bootstrap-localcloud --verbose";
		String output = CommandTestUtils.runCommandAndWait(command);
		return output.contains("Local-cloud started successfully");
	}
	
	private boolean tearDownLocalCloud() throws IOException, InterruptedException {
		String command = "teardown-localcloud --verbose";
		String output = CommandTestUtils.runCommandAndWait(command);
		return output.contains("Completed local-cloud teardown");
	}
}
