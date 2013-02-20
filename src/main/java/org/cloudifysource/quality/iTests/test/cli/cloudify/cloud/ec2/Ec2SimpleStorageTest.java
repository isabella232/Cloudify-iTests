package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.StorageUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

public class Ec2SimpleStorageTest extends NewAbstractCloudTest {

    private final static String SERVICE_NAME = "simpleStorage";
    private final static String SERVICE_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/" + SERVICE_NAME);
    private final static String SERVICE_FILE_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/" + SERVICE_NAME + "/simple-service.groovy");

    @BeforeClass(alwaysRun = true)
         public void init() throws Exception{

        super.bootstrap();

        File serviceFile = new File(SERVICE_FILE_PATH);
        Service service = ServiceReader.readService(serviceFile);

        StorageUtils.init(getService().getCloud(), service.getCompute().getTemplate(), service.getStorage().getTemplate(), getService());
    }

    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception{

        super.teardown();
//		StorageUtils.scanAndDeleteLeakedVolumes();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testInstallWithStorage() throws Exception{

        installServiceAndWait(SERVICE_PATH, SERVICE_NAME);

        AssertUtils.assertTrue(StorageUtils.verifyVolumeConfiguration(SERVICE_FILE_PATH));

        uninstallServiceIfFound(SERVICE_NAME);
    }

    @Override
    protected String getCloudName() {
        return "ec2";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }

}
