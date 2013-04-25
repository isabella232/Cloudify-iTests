package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.storage.staticstorage;

import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;
import iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.framework.utils.RecipeInstaller;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageAllocationTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.concurrent.TimeoutException;

/**
 * CLOUDIFY-1595
 *
 * <br>
 *     1. Installs Multi-tenant application with two services on the same machine.
 * </br>
 * <br>
 *     2. Each service defines a different static storage template
 * </br>
 * <br>
 *     3. Check both volumes are created and attached to the proper machine.
 * </br>
 * <br>
 *     4. Un-install the application and check that all storage volumes are de-allocated.
 * </br>
 *
 *
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 4/10/13
 * Time: 12:13 PM
 */
public class MultitenantStorageAllocationTest extends AbstractStorageAllocationTest {

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap();
    }

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testMultitenantStorageAllocation() throws Exception {
        storageAllocationTester.testMultitenantStorageAllocation();

    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        File customCloudFile = new File(SGTestHelper.getCustomCloudConfigDir(getCloudName()) + "/storage-two-templates-multitenant/ec2-cloud.groovy");
        ((Ec2CloudService)getService()).setCloudGroovy(customCloudFile);
    }

    @Override
    protected String getCloudName() {
        return "ec2";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
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

    @AfterClass
    public void scanForLeakesFromGroovy1() throws TimeoutException, StorageProvisioningException {
        super.scanForLeakedVolumesCreatedViaTemplate("GROOVY1");
    }

    @AfterClass
    public void scanForLeakesFromGroovy2() throws TimeoutException, StorageProvisioningException {
        super.scanForLeakedVolumesCreatedViaTemplate("GROOVY2");
    }

    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        super.teardown();
    }
}
