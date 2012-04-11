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
		appName = "travel";
		LogUtils.log("installing application travel on " + cloudName);
		setCloudToUse(cloudName);
		doTest("travel");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true, dataProvider = "supportedCloudsWithoutByon")
	public void testPetclinic(String cloudName) throws IOException, InterruptedException {
		appName = "petclinic";
		LogUtils.log("installing application petclinic on " + cloudName);
		setCloudToUse(cloudName);
		doTest("petclinic");
	}
	

	private void doTest(String applicationName) throws IOException, InterruptedException {
		
		String applicationPath = ScriptUtils.getBuildPath() + "/examples/" + applicationName;
		installApplicationAndWait(applicationPath, applicationName);
		
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
