package test.cli.cloudify.cloud;

import java.io.IOException;

import org.testng.annotations.Test;

import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class ExamplesTest extends AbstractCloudTest {

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true, dataProvider = "supportedClouds")
	public void testTravel(String cloudName) throws IOException, InterruptedException {
		LogUtils.log("installing application travel on " + cloudName);
		setCloudToUse(cloudName);
		doTest("travel");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true, dataProvider = "supportedClouds")
	public void testPetclinic(String cloudName) throws IOException, InterruptedException {
		LogUtils.log("installing application petclinic on " + cloudName);
		setCloudToUse(cloudName);
		doTest("petclinic");
	}
	

	private void doTest(String applicationName) throws IOException, InterruptedException {
		
		String applicationPath = ScriptUtils.getBuildPath() + "/examples/" + applicationName;
		
		installApplicationAndWait(applicationPath, applicationName);
		uninstallApplicationAndWait(applicationName);
		
	}
}
