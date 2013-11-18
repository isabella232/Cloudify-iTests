package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.custom;

import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.IOException;

import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.codehaus.plexus.util.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * 
 * @author adaml
 *
 */
public class CustomGroovyNetworkDriverTest extends AbstractCustomCloudDriverTest{
	
	final private static String GROOVY_NETWORK_DRIVER = 
			CommandTestUtils.getPath("src/main/resources/custom-network-drivers/NetworkProvisioningDriverClass.groovy");
	private static final String CUSTOM_CLOUD_DRIVER_PATH = 
			CommandTestUtils.getPath("src/main/resources/custom-network-drivers/ec2-cloud.groovy");
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installServiceUsingGroovyCloudDriverTest() throws Exception {
		super.installNetworkService();
		assertGroovyNetworkUsed();
	}
	
	private void assertGroovyNetworkUsed() throws IOException, InterruptedException {
		final String invokeCommand = "connect " + this.getRestUrl() + ";" 
    			+ " invoke " + SIMPLE_NETWORK_SERVICE_NAME + " is";
		LogUtils.log("invoking command: " + invokeCommand);
    	final String output = CommandTestUtils.runCommandAndWait(invokeCommand);
    	LogUtils.log("command output was: " + output);
    	assertTrue("invocation failed", output.contains("invocation completed successfully.")
    			&& output.contains("it works"));
	}

	@Override
	protected void customizeCloud() throws Exception {
        super.customizeCloud();
        //add the network groovy
        final File libFolder = new File(getService().getPathToCloudFolder(), "lib");
        final File groovyCloudDriver = new File(GROOVY_NETWORK_DRIVER);
        final File groovyDest = new File(libFolder, "NetworkProvisioningDriverClass.groovy");
        FileUtils.copyFile(groovyCloudDriver, groovyDest);
        
        //set a custom cloud for this bootstrap
        final File customCloudFile = new File(CUSTOM_CLOUD_DRIVER_PATH);
        ((Ec2CloudService)getService()).setCloudGroovy(customCloudFile);
	}
	
	@AfterClass
	public void teardown() throws Exception {
		super.teardown();					
	}

}
