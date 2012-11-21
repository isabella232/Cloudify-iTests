package test.cli.cloudify.recipes;

import java.io.IOException;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.cloudifysource.dsl.utils.ServiceUtils;

import framework.utils.LogUtils;

import test.cli.cloudify.AbstractLocalCloudTest;
import test.cli.cloudify.CommandTestUtils;

public class CyclicDependencyTest extends AbstractLocalCloudTest{

    private void dependencyTest(String ApplicationName) throws IOException, InterruptedException {
        String path = CommandTestUtils.getPath("apps/cloudify/recipes/" + ApplicationName);
        CommandTestUtils.runCommandExpectedFail("connect " + this.restUrl + ";install-application " + path + ";exit");

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

    //TODO: Ask Sagi what his intention was. 
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void diamondTest() throws IOException, InterruptedException {

        String path = CommandTestUtils.getPath("apps/cloudify/recipes/diamond");

        //this app is only a mock so the command to install it is not successful
        CommandTestUtils.runCommand("connect " + this.restUrl + ";install-application " + path + ";exit");

        admin.getZones().waitFor(ServiceUtils.getAbsolutePUName("diamond", "D")).getGridServiceContainers().waitFor(1);

        GridServiceContainer[] gscs = admin.getGridServiceContainers().getContainers();
        for (GridServiceContainer gsc : gscs) {
            if (gsc.getZones().containsKey("diamond.D"))
            	LogUtils.scanContainerLogsFor(gsc, "D1 PreStart completed successfully");
        }
    }
}
