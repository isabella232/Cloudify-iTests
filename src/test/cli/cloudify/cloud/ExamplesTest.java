package test.cli.cloudify.cloud;

import java.io.IOException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;

import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class ExamplesTest extends AbstractCloudTest {
	
	public ExamplesTest() {
		LogUtils.log("Instansiated " + ExamplesTest.class.getName());
	}
	
	private String appName;

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true, dataProvider = "supportedClouds")
	public void testTravel(String cloudName) throws IOException, InterruptedException {
		doTest(cloudName, "travel");
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true, dataProvider = "supportedCloudsWithoutByon")
	public void testPetclinic(String cloudName) throws IOException, InterruptedException {
		doTest(cloudName, "petclinic");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true, dataProvider = "supportedClouds")
	public void testPetclinicSimple(String cloudName) throws IOException, InterruptedException {
		doTest(cloudName, "petclinic-simple");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true, dataProvider = "supportedClouds")
	public void testPetclinicSimpleScalingRules(String cloudName) throws IOException, InterruptedException {
		doTest(cloudName, "petclinic-simple-scalingRules");
	}
	
	private void doTest(String cloudName, String applicationName) throws IOException, InterruptedException {
		this.appName = applicationName;
		LogUtils.log("installing application travel on " + cloudName);
		setCloudToUse(cloudName);
		String applicationPath = ScriptUtils.getBuildPath() + "/examples/" + appName;
		installApplicationAndWait(applicationPath, appName);	
	}
	
	@AfterMethod
	public void cleanup() throws IOException, InterruptedException {
		
		if ((getService() != null) && (getService().getRestUrls() != null)) {
			String command = "connect " + getService().getRestUrls()[0] + ";list-applications";
			String output = CommandTestUtils.runCommandAndWait(command);
			if (output.contains(appName)) {
				uninstallApplicationAndWait(appName);			
			}
		}
	}
}
