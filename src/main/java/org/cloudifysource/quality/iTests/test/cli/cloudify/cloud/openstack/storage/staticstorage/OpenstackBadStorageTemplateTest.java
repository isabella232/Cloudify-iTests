package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack.storage.staticstorage;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageAllocationTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * User: nirb
 * Date: 1/14/14
 */
public class OpenstackBadStorageTemplateTest extends AbstractStorageAllocationTest {

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap();
    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        getService().getAdditionalPropsToReplace().put("deviceName \"/dev/vdc\"", "deviceName \"/dev/dc\"");
    }

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testLinux() throws Exception {
        String folderName = "simple-storage";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        storageAllocationTester.setTemplate(servicePath, "SMALL_LINUX", false);

        String output = installServiceAndWait(servicePath, "groovy", SERVICE_INSTALLATION_TIMEOUT_IN_MINUTES, true, true, 0);
        String expectedOutput = "Unable to open /dev/dc";
        AssertUtils.assertTrue(output.contains(expectedOutput));
    }

    @AfterMethod
    public void cleanup() throws Exception {
        uninstallServiceIfFound("groovy");
    }

    @AfterClass(alwaysRun = true)
    public void scanForLeakes() throws Exception {
    	try {
    		super.scanForLeakedVolumesCreatedViaTemplate("SMALL_BLOCK");
    	} finally {
    		try {
				super.teardown();
			} catch (final Exception e) {
				LogUtils.log("WARNING!! failed tearing down cloud. Leaked resources may be found. Reason: " + e.getMessage(), e);
			}
    	}
    }

    @Override
    protected String getCloudName() {
        return "hp-grizzly";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }
}
