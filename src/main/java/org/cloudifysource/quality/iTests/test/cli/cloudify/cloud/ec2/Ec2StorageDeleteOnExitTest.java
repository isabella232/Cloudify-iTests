package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.StorageUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Set;

/**
 * Author: nirb
 * Date: 20/02/13
 */
public class Ec2StorageDeleteOnExitTest extends NewAbstractCloudTest {

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
        Set<String> machinesIps = StorageUtils.getServicesToMachines().get(SERVICE_NAME);

        AssertUtils.assertTrue("volume not started", !StorageUtils.getVolumes(SERVICE_NAME).isEmpty());

        uninstallServiceIfFound(SERVICE_NAME);

        AssertUtils.assertTrue("volume was deleted", !StorageUtils.getVolumes(machinesIps).isEmpty());

    }

    @Override
    protected String getCloudName() {
        return "ec2";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }

    protected void customizeCloud() throws Exception {
        ((Ec2CloudService)getService()).getAdditionalPropsToReplace().put("deleteOnExit true", "deleteOnExit false");
    }

}
