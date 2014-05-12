package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack.storage.staticstorage;

import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.framework.utils.RecipeInstaller;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageAllocationTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Author: nirb
 * Date: 25/02/13
 */
public class OpenstackFailedToInstallTest extends AbstractStorageAllocationTest {

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap();
    }

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testLinux() throws Exception {
        storageAllocationTester.testFaultyInstallLinux("SMALL_LINUX", "openstack-faulty-install");
    }

    @AfterMethod
    public void cleanup() {
        RecipeInstaller installer = storageAllocationTester.getInstaller();
        if (installer instanceof ServiceInstaller) {
            ((ServiceInstaller) installer).uninstallIfFound();
        } else {
            ((ApplicationInstaller) installer).uninstallIfFound();
        }
    }

    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
    	try {
    		super.scanForLeakedVolumesCreatedViaTemplate("SMALL_BLOCK");
    	} finally {
    		super.teardown();
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
