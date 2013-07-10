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
public class CustomJarCloudDriverTest extends AbstractCustomCloudDriverTest {
	
	final private static String JAR_EXT_CLOUD_DRIVER = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/customCloudDriver/ExtCloudDriver.jar");
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installServiceUsingExternalCloudDriverTest() throws Exception {
		super.installService();
	}
	
	@Override
	protected void customizeCloud() throws Exception {
        super.customizeCloud();
        final File libFolder = new File(getService().getPathToCloudFolder(), "lib");
        final File jarDest = new File(libFolder, "ExtCloudDriver.jar");
        final File extJarCloudDriver = new File(JAR_EXT_CLOUD_DRIVER);
        FileUtils.copyFile(extJarCloudDriver, jarDest);
        // Set the cloud driver class name to the external cloud class name inside the jar.
        super.getService().getAdditionalPropsToReplace().put("org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver", 
        													"external.provisioning.driver.ExternalProvisioningDriver");
	}
}
