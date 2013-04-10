package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.staticstorage;

import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.TimeoutException;

/**
 *
 * Check that there are no leaking volumes after uninstall of a failed installation.

 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 4/9/13
 * Time: 7:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class FailedToInstallTest extends AbstractEc2OneServiceStaticStorageTest {

    private static final String FOLDER_NAME = "faulty-install";
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

        // this installation will fail at install event.
        // causing the USM to shutdown and de-allocate the storage.
        installer.install();

        installer.expectToFail(false);
        installer.uninstall();

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
