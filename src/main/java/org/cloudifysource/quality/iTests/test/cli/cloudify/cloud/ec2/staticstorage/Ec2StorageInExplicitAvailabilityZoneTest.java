package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.staticstorage;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudServiceManager;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.TimeoutException;

public class Ec2StorageInExplicitAvailabilityZoneTest extends AbstractEc2OneServiceStaticStorageTest {

    private static final String FOLDER_NAME = "simple-storage";

    @Override
    protected String getCloudName() {
        return "ec2";
    }

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        Ec2CloudService cloudService = (Ec2CloudService) CloudServiceManager.getInstance().getCloudService(getCloudName());
        cloudService.setAvailabilityZone("c");
        super.bootstrap(cloudService);
    }


    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testLinux() throws Exception {
        super.testLinux();
    }


    @Override
    public void doTest() throws Exception {
        super.testInstallWithStorage(FOLDER_NAME);
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
    protected boolean isReusableCloud() {
        return false;
    }

    @Override
    public String getServiceFolder() {
        return FOLDER_NAME;
    }
}
