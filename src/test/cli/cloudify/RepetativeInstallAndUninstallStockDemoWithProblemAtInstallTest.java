package test.cli.cloudify;

import java.io.IOException;

import junit.framework.Assert;

import org.testng.annotations.Test;

public class RepetativeInstallAndUninstallStockDemoWithProblemAtInstallTest extends AbstractLocalCloudTest {

	
	private final int repetitions = 10;
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testInstallAndUninstall() throws IOException, InterruptedException {
		for(int i=0 ; i < repetitions ; i++)
			doTest();
	}

	private void doTest() throws IOException, InterruptedException {
		final String applicationDir = CommandTestUtils.getPath("apps/USM/usm/applications/stockdemo");	
		
		String failOutput = CommandTestUtils.runCommand("connect " + restUrl + ";install-application --verbose -timeout 4 " + applicationDir, true, true);		
		if(!failOutput.toLowerCase().contains("operation failed"))
			return;
		
		runCommand("connect " + restUrl + ";uninstall-application --verbose stockdemo");
				
		String successOutput = CommandTestUtils.runCommand("connect " + restUrl + ";install-application --verbose " + applicationDir, true, false);
		Assert.assertTrue("stockdemo was not installed sucessfully the second time", successOutput.toLowerCase().contains("successfully installed"));
		
	}
}
