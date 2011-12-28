package test.webui.recipes;

import java.io.IOException;

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
	
	@BeforeSuite
	public void bootstrap() throws IOException, InterruptedException {
		assertTrue(bootstrapLocalCloud());
		bootstraped = true;
	}
	
	@Override
	@BeforeMethod
	public void beforeTest() {
		LogUtils.log("Test Configuration Started : " + this.getClass());
	}
	
	@Override
	@AfterMethod
	public void afterTest() {
		LogUtils.log("Test Finished : " + this.getClass());
	}
	
	@AfterSuite(alwaysRun = true)
	public void teardown() throws IOException, InterruptedException {
		assertTrue(tearDownLocalCloud());
		bootstraped = false;
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
