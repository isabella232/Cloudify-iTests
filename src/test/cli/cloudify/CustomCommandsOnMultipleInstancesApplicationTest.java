package test.cli.cloudify;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.Assert;
import org.testng.annotations.Test;

import test.usm.USMTestUtils;
import framework.utils.LogUtils;

public class CustomCommandsOnMultipleInstancesApplicationTest extends AbstractLocalCloudTest {

    private static final String APPLICATION_NAME = "simpleCustomCommandsMultipleInstances";
	private static final int TOTAL_INSTANCES_SERVICE = 2;

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void testPrintCommandOnApp() throws Exception {
    	installApplication();
    	LogUtils.log("Checking print command on all instances");
        checkPrintCommandOnapp();

        LogUtils.log("Starting to check print command by instance id");
        for (int i = 1; i <= TOTAL_INSTANCES_SERVICE; i++) {
            checkPrintCommandOnApp(i);
        }
        uninstallApplication();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void testParamsCommand() throws Exception {
    	installApplication();
    	LogUtils.log("Checking params command on all instances");
        checkParamsCommand();

        LogUtils.log("Starting to check params command by instance id");
        for (int i = 1; i <= TOTAL_INSTANCES_SERVICE; i++) {
            checkParamsCommand(i);
        }
        uninstallApplication();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void testExceptionCommand() throws Exception {
    	installApplication();
    	LogUtils.log("Checking exception command on all instances");
        checkExceptionCommand();

        LogUtils.log("Starting to check exception command by instance id");
        for (int i = 1; i <= TOTAL_INSTANCES_SERVICE; i++) {
            checkExceptionCommand(i);
        }
        uninstallApplication();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void testRunScriptCommand() throws Exception {
    	installApplication();
    	LogUtils.log("Checking runScript command on all instances");
        checkRunScriptCommand();

        LogUtils.log("Starting to check runScript command by instance id");
        for (int i = 1; i <= TOTAL_INSTANCES_SERVICE; i++) {
            checkRunScriptCommand(i);
        }
        uninstallApplication();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void testContextCommand() throws Exception {
    	installApplication();
    	LogUtils.log("Checking context command on all instances");
        checkContextCommandOnApp();

        LogUtils.log("Starting to check context command by instance id");
        for (int i = 1; i <= TOTAL_INSTANCES_SERVICE; i++) {
            checkContextCommandOnApp(i);
        }
        uninstallApplication();
    }

    private void checkPrintCommandOnapp() throws IOException, InterruptedException {
        String invokePrintResult = runCommand("connect " + restUrl + ";use-application simpleCustomCommandsMultipleInstances"
                + "; invoke simpleCustomCommandsMultipleInstances-2 print");

        assertTrue("Custom command 'print' returned unexpected result from instance #1: " + invokePrintResult
                , invokePrintResult.contains("OK from instance #1"));
        assertTrue("Custom command 'print' returned unexpected result from instance #2: " + invokePrintResult
                , invokePrintResult.contains("OK from instance #2"));
    }

    private void checkPrintCommandOnApp(int instanceId) throws IOException, InterruptedException {
        String invokePrintResult = runCommand("connect " + restUrl + ";use-application simpleCustomCommandsMultipleInstances"
                + "; invoke -instanceid " + instanceId + " simpleCustomCommandsMultipleInstances-2 print");

        assertTrue("Custom command 'print' returned unexpected result from instance #" + instanceId + ": " + invokePrintResult
                , invokePrintResult.contains("OK from instance #" + instanceId));

        for (int i = 1; i <= TOTAL_INSTANCES_SERVICE; i++) {
            if (i == instanceId)
                continue;
            Assert.assertFalse(invokePrintResult.contains("instance #" + i), "should not recive any output from instance" + i);
        }
    }

    private void checkParamsCommand() throws IOException, InterruptedException {
        String invokeParamsResult = runCommand("connect " + restUrl + ";use-application simpleCustomCommandsMultipleInstances"
                + "; invoke simpleCustomCommandsMultipleInstances-2 params 2 3");

        for (int i = 1; i <= TOTAL_INSTANCES_SERVICE; i++) {
            assertTrue("Custom command 'params' returned unexpected result from instance #" + i + ": " + invokeParamsResult
                    , invokeParamsResult.contains("OK from instance #" + i) && invokeParamsResult.contains("Result: this is the custom parameters command. expecting 123: 123"));
        }
    }

    private void checkParamsCommand(int instanceId) throws IOException, InterruptedException {
        String invokeParamsResult = runCommand("connect " + restUrl + ";use-application simpleCustomCommandsMultipleInstances"
                + "; invoke -instanceid " + instanceId + " simpleCustomCommandsMultipleInstances-2 params 2 3");

        assertTrue("Custom command 'params' returned unexpected result from instance #" + instanceId + ": " + invokeParamsResult
                , invokeParamsResult.contains("OK from instance #" + instanceId) && invokeParamsResult.contains("Result: this is the custom parameters command. expecting 123: 123"));

        for (int i = 1; i <= TOTAL_INSTANCES_SERVICE; i++) {
            if (i == instanceId)
                continue;
            Assert.assertFalse(invokeParamsResult.contains("instance #" + i), "should not recive any output from instance" + i);
        }
    }

    private void checkExceptionCommand() throws IOException, InterruptedException {
        String invokeExceptionResult = CommandTestUtils.runCommandExpectedFail("connect " + restUrl + ";use-application simpleCustomCommandsMultipleInstances"
                + "; invoke simpleCustomCommandsMultipleInstances-2 exception");

        for (int i = 1; i <= TOTAL_INSTANCES_SERVICE; i++) {
            assertTrue("Custom command 'exception' returned unexpected result from instance #" + i + ": " + invokeExceptionResult
                    , invokeExceptionResult.contains("FAILED from instance #" + i) && invokeExceptionResult.contains("This is an error test"));
        }
    }

    private void checkExceptionCommand(int instanceId) throws IOException, InterruptedException {
        String invokeExceptionResult = CommandTestUtils.runCommandExpectedFail("connect " + restUrl + ";use-application simpleCustomCommandsMultipleInstances"
                + "; invoke -instanceid " + instanceId + " simpleCustomCommandsMultipleInstances-2 exception");

        assertTrue("Custom command 'exception' returned unexpected result from instance #" + instanceId + ": " + invokeExceptionResult
                , invokeExceptionResult.contains("FAILED from instance #" + instanceId) && invokeExceptionResult.contains("This is an error test"));

        for (int i = 1; i <= TOTAL_INSTANCES_SERVICE; i++) {
            if (i == instanceId)
                continue;
            Assert.assertFalse(invokeExceptionResult.contains("instance #" + i), "should not recive any output from instance" + i);
        }
    }

    private void checkRunScriptCommand() throws IOException, InterruptedException {
        String invokeRunScriptResult = runCommand("connect " + restUrl + ";use-application simpleCustomCommandsMultipleInstances"
                + "; invoke simpleCustomCommandsMultipleInstances-2 runScript");

        for (int i = 1; i <= TOTAL_INSTANCES_SERVICE; i++) {
            assertTrue("Custom command 'runScript' returned unexpected result from instance #" + i + ": " + invokeRunScriptResult
                    , invokeRunScriptResult.contains("OK from instance #" + i) && invokeRunScriptResult.contains("Result: 2"));
        }
    }

    private void checkRunScriptCommand(int instanceId) throws IOException, InterruptedException {
        String invokeRunScriptResult = runCommand("connect " + restUrl + ";use-application simpleCustomCommandsMultipleInstances"
                + "; invoke -instanceid " + instanceId + " simpleCustomCommandsMultipleInstances-2 runScript");

        assertTrue("Custom command 'exception' returned unexpected result from instance #" + instanceId + ": " + invokeRunScriptResult
                , invokeRunScriptResult.contains("OK from instance #" + instanceId) && invokeRunScriptResult.contains("Result: 2"));

        for (int i = 1; i <= TOTAL_INSTANCES_SERVICE; i++) {
            if (i == instanceId)
                continue;
            Assert.assertFalse(invokeRunScriptResult.contains("instance #" + i), "should not recive any output from instance" + i);
        }
    }

    private void checkContextCommandOnApp() throws IOException, InterruptedException {
        String invokeContextResult = runCommand("connect " + restUrl + ";use-application simpleCustomCommandsMultipleInstances"
                + "; invoke simpleCustomCommandsMultipleInstances-2 context");

        for (int i = 1; i <= TOTAL_INSTANCES_SERVICE; i++) {
            assertTrue("Custom command 'runScript' returned unexpected result from instance #" + i + ": " + invokeContextResult
                    , invokeContextResult.contains("OK from instance #" + i) && invokeContextResult.contains("Service Dir is:"));
        }
    }

    private void checkContextCommandOnApp(int instanceId) throws IOException, InterruptedException {
        String invokeContextResult = runCommand("connect " + restUrl + ";use-application simpleCustomCommandsMultipleInstances"
                + "; invoke -instanceid " + instanceId + " simpleCustomCommandsMultipleInstances-2 context");

        assertTrue("Custom command 'exception' returned unexpected result from instance #" + instanceId + ": " + invokeContextResult
                , invokeContextResult.contains("OK from instance #" + instanceId) && invokeContextResult.contains("Service Dir is:"));

        for (int i = 1; i <= TOTAL_INSTANCES_SERVICE; i++) {
            if (i == instanceId)
                continue;
            Assert.assertFalse(invokeContextResult.contains("instance #" + i), "should not recive any output from instance" + i);
        }
    }
    
	private void installApplication() {
		installApplication(APPLICATION_NAME);
                
        final String absolutePUNameSimple1 = ServiceUtils.getAbsolutePUName(APPLICATION_NAME, "simpleCustomCommandsMultipleInstances-1");
        final String absolutePUNameSimple2 = ServiceUtils.getAbsolutePUName(APPLICATION_NAME, "simpleCustomCommandsMultipleInstances-2");
        final ProcessingUnit pu1 = admin.getProcessingUnits().waitFor(absolutePUNameSimple1, WAIT_FOR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        final ProcessingUnit pu2 = admin.getProcessingUnits().waitFor(absolutePUNameSimple2, WAIT_FOR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertNotNull(pu1);
        assertNotNull(pu2);
        assertTrue("applications was not installed", pu1.waitFor(TOTAL_INSTANCES_SERVICE, WAIT_FOR_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue("applications was not installed", pu2.waitFor(TOTAL_INSTANCES_SERVICE, WAIT_FOR_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertNotNull("applications was not installed", admin.getApplications().getApplication(APPLICATION_NAME));
        assertTrue("USM Service State is NOT RUNNING", USMTestUtils.waitForPuRunningState(absolutePUNameSimple1, 60, TimeUnit.SECONDS, admin));
        assertTrue("USM Service State is NOT RUNNING", USMTestUtils.waitForPuRunningState(absolutePUNameSimple2, 60, TimeUnit.SECONDS, admin));
	}
	
	private void uninstallApplication() {
		uninstallApplication(APPLICATION_NAME);
	}

}
