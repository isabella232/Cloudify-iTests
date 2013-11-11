package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.custom;

import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.IOException;

import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.codehaus.plexus.util.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * 
 * @author adaml
 *
 */
public class CustomGroovyCloudDriverTest extends AbstractCustomCloudDriverTest{
	
	final private static String GROOVY_CLOUD_DRIVER = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/customCloudDriver/ProvisioningDriverClass.groovy");
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installServiceUsingGroovyCloudDriverTest() throws Exception {
		super.installService();
		// check key file is not being copied to agent machines.
		assertKeyFileNotCopiedToAgentMachine();
	}
	
	private void assertKeyFileNotCopiedToAgentMachine() throws IOException, InterruptedException {
		final String output = invokeListRemoteFilesCommand();
		assertTrue("Pem file was copied to agent machine. remote dir content is: " + output,
				!output.contains(getPemFile().getName()));
		
	}
	
	private String invokeListRemoteFilesCommand() throws IOException, InterruptedException {
		final String invokeCommand = "connect " + this.getRestUrl() + ";" 
    			+ " invoke " + SIMPLE_SERVICE_NAME + " listRemoteFiles";
		LogUtils.log("invoking command: " + invokeCommand);
    	final String output = CommandTestUtils.runCommandAndWait(invokeCommand);
    	LogUtils.log("command output was: " + output);
    	assertTrue("invocation failed", output.contains("invocation completed successfully."));
    	return output;
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
