package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack.storage.dynamicstorage;

import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.framework.utils.RecipeInstaller;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageAllocationTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Author: noak
 */

public class OpenstackDynamicStorageExternalGroovyTest extends AbstractStorageAllocationTest{

    @Override
    protected String getCloudName() {
        return "hp-grizzly";
    }

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap();
    }
    
    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
    }


    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testStorageOnExternalGroovy() throws Exception {
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/dynamicstorage/openstack/detach-on-external-groovy");
        storageAllocationTester.testInstallWithDynamicStorageLinux("SMALL_LINUX", servicePath, "detach-on-external-groovy");
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
    public void scanForLeakes() throws Exception {
    	try {
    		super.scanForLeakedVolumesCreatedViaTemplate("SMALL_BLOCK");
    	} finally {
    		super.teardown();
    	}
    }

    
    @Override
    protected boolean isReusableCloud() {
        return false;
    }
}
