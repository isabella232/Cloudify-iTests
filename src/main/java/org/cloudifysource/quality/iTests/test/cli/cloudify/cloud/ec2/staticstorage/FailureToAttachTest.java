package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.staticstorage;

import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.jclouds.ec2.domain.Volume;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 *
 * Allocate static storage to a service but fail the attachment phase. (by using a faulty device name).
 * this will cause the USM to be stuck in an endless restart loop.
 * make sure that no un-necessary volumes are created during this loop.
 *
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 4/9/13
 * Time: 5:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class FailureToAttachTest extends AbstractEc2OneServiceStaticStorageTest {

    private static final String FOLDER_NAME = "simple-storage";
    private ServiceInstaller installer;

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap();
    }

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testLinux() throws Exception {
        super.testLinux();
    }

    @Override
    public void doTest() throws Exception {

        installer = new ServiceInstaller(getRestUrl(), getServiceName());
        installer.recipePath(FOLDER_NAME);
        installer.timeoutInMinutes(3);
        installer.expectToFail(true);
        installer.install();

        LogUtils.log("Searching for volumes created by the service installation");
        // the install should have created and attached a volume with a name prefix of the class name. see customizeCloud below.
        Set<Volume> ourVolumes = storageHelper.getVolumesByName(System.getProperty("user.name") + "-" + this.getClass().getSimpleName().toLowerCase());

        AssertUtils.assertEquals("Found leaking volumes created by failed installation", 0, ourVolumes.size());

        installer.expectToFail(false);
        installer.uninstall();

    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        ((Ec2CloudService)getService()).getAdditionalPropsToReplace().put("/dev/sdc", "foo");
    }

    @AfterMethod
    public void scanForLeakes() throws TimeoutException {
        super.scanForLeakedVolumesCreatedViaTemplate("SMALL_BLOCK");
    }

    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        super.teardown();
    }


    @Override
    public String getServiceFolder() {
        return FOLDER_NAME;
    }


    @Override
    protected boolean isReusableCloud() {
        return false;
    }
}
