package org.cloudifysource.quality.iTests.test.cli.cloudify.recipes;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractLocalCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;

public class CyclicDependencyTest extends AbstractLocalCloudTest{

    private void dependencyTest(String ApplicationName) throws IOException, InterruptedException {
        String path = CommandTestUtils.getPath("src/main/resources/apps/cloudify/recipes/" + ApplicationName);
        CommandTestUtils.runCommandExpectedFail("connect " + restUrl + ";install-application " + path + ";exit");

        GridServiceContainer[] gscs = admin.getGridServiceContainers().getContainers();
        boolean foundStringInGscsLogs = false;

        for (int i = 0; i < gscs.length && !foundStringInGscsLogs; i++) {
            try {
                LogUtils.repetativeScanContainerLogsFor(gscs[i], "contains one or more cycles", DEFAULT_TEST_TIMEOUT / gscs.length);
                foundStringInGscsLogs = true;
            } catch (AssertionError e) {
                //do nothing could be found in the next gsc log
            }
        }
        if (!foundStringInGscsLogs)
            Assert.fail("none of the gscs has the right exception");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void complexTest() throws IOException, InterruptedException {
        dependencyTest("complexCycle");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void simpleTest() throws IOException, InterruptedException {
        dependencyTest("cycle");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void diamondTest() throws IOException, InterruptedException {

        String path = CommandTestUtils.getPath("src/main/resources/apps/cloudify/recipes/diamond");
        
        ApplicationInstaller installer = new ApplicationInstaller(restUrl, "diamond");
        installer.recipePath(path);
        installer.install();

        AssertUtils.assertTrue("Failed to discover D service after installation", admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("diamond", "D"), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS) != null);

        ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit(ServiceUtils.getAbsolutePUName("diamond", "D"));
        AssertUtils.assertTrue("Failed to discover 1 instance of service D", processingUnit.waitFor(1, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));        
    }
}
