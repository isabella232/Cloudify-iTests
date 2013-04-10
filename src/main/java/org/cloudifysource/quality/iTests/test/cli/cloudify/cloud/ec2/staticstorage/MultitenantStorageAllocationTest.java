package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.staticstorage;

import org.cloudifysource.quality.iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.AbstractEc2StorageAllocationTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.jclouds.ec2.domain.Volume;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Set;
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
public class MultitenantStorageAllocationTest extends AbstractEc2StorageAllocationTest {

    private static final String PATH_TO_APPLICATION = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/groovyApp-multitenant");

    private ApplicationInstaller installer;

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap();
    }

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    protected void testMultitenantStorageAllocation() throws IOException, InterruptedException {

        installer = new ApplicationInstaller(getRestUrl(), "groovy-App");
        installer.recipePath(PATH_TO_APPLICATION);
        installer.install();

        final String groovy1VolumePrefix = getService().getCloud().getCloudStorage().getTemplates().get("GROOVY1").getNamePrefix();
        LogUtils.log("Volume prefix for groovy1 is " + groovy1VolumePrefix);
        final String groovy2VolumePrefix = getService().getCloud().getCloudStorage().getTemplates().get("GROOVY2").getNamePrefix();
        LogUtils.log("Volume prefix for groovy2 is " + groovy2VolumePrefix);

        LogUtils.log("Retrieving volumes for groovy1");
        Set<Volume> groovy1Volumes = storageHelper.getVolumesByName(groovy1VolumePrefix);
        AssertUtils.assertEquals("Wrong number of volumes detected after installation for service groovy1", 1, groovy1Volumes.size());

        LogUtils.log("Retrieving volumes for groovy2");
        Set<Volume> groovy2Volumes = storageHelper.getVolumesByName(groovy2VolumePrefix);
        AssertUtils.assertEquals("Wrong number of volumes detected after installation for service groovy2", 1, groovy2Volumes.size());

        AssertUtils.assertEquals("Both volumes should be attached to the same instance", groovy1Volumes.iterator().next().getAttachments(), groovy2Volumes.iterator().next().getAttachments());

        installer.uninstall();

    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        File customCloudFile = new File(SGTestHelper.getCustomCloudConfigDir(getCloudName()) + "/storage-two-templates-multitenant/ec2-cloud.groovy");
        ((Ec2CloudService)getService()).setCloudGroovy(customCloudFile);
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }

    @AfterMethod
    public void scanForLeakesFromGroovy1() throws TimeoutException {
        super.scanForLeakedVolumesCreatedViaTemplate("GROOVY1");
    }

    @AfterMethod
    public void scanForLeakesFromGroovy2() throws TimeoutException {
        super.scanForLeakedVolumesCreatedViaTemplate("GROOVY2");
    }


    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        super.teardown();
    }
}
