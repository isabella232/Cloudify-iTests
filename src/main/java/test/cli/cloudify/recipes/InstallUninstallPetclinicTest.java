package test.cli.cloudify.recipes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.testng.annotations.Test;

import test.cli.cloudify.AbstractLocalCloudTest;
import framework.utils.AssertUtils;
import framework.utils.ScriptUtils;
import framework.utils.WebUtils;

public class InstallUninstallPetclinicTest extends AbstractLocalCloudTest {
	
	private static int ITERATIONS = 3;
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = true)
	public void multipleInstallUninstallTest() throws Exception {
		
		for (int i = 0 ; i < ITERATIONS ; i++) {
			assertTrue(installPetclinic());
			assertPetclinicPageExists();
			assertTrue(uninstallPetclinic());
		}
		
	}
	
	private boolean installPetclinic() throws IOException, InterruptedException {
		String petclinicPath = ScriptUtils.getBuildPath() + "/recipes/apps/petclinic";
		String cliOutput = runCommand("connect " + restUrl + ";install-application --verbose " + petclinicPath);
		return cliOutput.toLowerCase().contains("application petclinic installed successfully");
	}
	
	private boolean uninstallPetclinic() throws IOException, InterruptedException {
		String cliOutput = runCommand("connect " + restUrl + ";uninstall-application --verbose petclinic");
		return cliOutput.toLowerCase().contains("application petclinic uninstalled successfully");
	}
	
	private void assertPetclinicPageExists() throws MalformedURLException, Exception {		
		AssertUtils.assertTrue("petclinic page is not available even though the application was installed succesfully", 
					WebUtils.isURLAvailable(new URL("http://127.0.0.1:8080/petclinic/")));		
	}

}
