package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.custom;

import java.io.File;

import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils.ProcessResult;
import org.codehaus.plexus.util.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * 
 * @author adaml
 *
 */
public class NetworkCustomGroovyCloudDriverTest extends AbstractCustomCloudDriverTest{
	
	final private static String GROOVY_CLOUD_DRIVER = 
			CommandTestUtils.getPath("src/main/resources/custom-cloud-drivers/CustomEc2NetworkSupportTest/ProvisioningDriverClass.groovy");
	final private static String SHELL_SERVICE_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/shell");
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installShellService() throws Exception {
		installServiceAndWait(SHELL_SERVICE_PATH, "shell");
		final String command = "connect " + this.cloudService.getRestUrls()[0] + "; invoke shell env";
		ProcessResult commandOutput = CommandTestUtils.runCloudifyCommandAndWait(command);
		Assert.assertEquals(commandOutput.getExitcode(), 0);
		Assert.assertTrue(commandOutput.getOutput().contains("NETWORK_TEST_MARKER"));
	}
	
	@Override
	protected void customizeCloud() throws Exception {
        super.customizeCloud();
        final File libFolder = new File(getService().getPathToCloudFolder(), "lib");
        final File groovyCloudDriver = new File(GROOVY_CLOUD_DRIVER);
        final File groovyDest = new File(libFolder, "ProvisioningDriverClass.groovy");
        FileUtils.copyFile(groovyCloudDriver, groovyDest);
        // Set the cloud driver class name to the groovy class name.
        super.getService().getAdditionalPropsToReplace().put("org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver",
        														"ProvisioningDriverClass");
	}
	
	@AfterClass
	public void teardown() throws Exception {		
		super.teardown();					
	}

}
