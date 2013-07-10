package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.custom;

import java.io.File;

import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.codehaus.plexus.util.FileUtils;
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

}
