package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;
import iTests.framework.utils.SSHUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import iTests.framework.utils.WebUtils;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.j_spaces.kernel.PlatformVersion;

/**
 * User: nirb
 * Date: 9/12/13
 */
public class Ec2RecipeDebuggerTest extends NewAbstractCloudTest {

    private String SERVICE_NAME = "tomcat";
    private static int TIMEOUT_IN_MILLISECONDS = 1000 * 60 * 10;

    @Override
    protected String getCloudName() {
        return "ec2";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap();
    }

    @Override
    public void beforeBootstrap() throws IOException {

        File debuggerSrcFolder = new File(SGTestHelper.getSGTestRootDir() + "/src/main/resources/scripts/debugger");
        File debuggerTestFolder = new File(getService().getPathToCloudFolder() + "/upload/cloudify-overrides");
        FileUtils.copyDirectoryToDirectory(debuggerSrcFolder, debuggerTestFolder);
    }

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testDebugger() throws Exception {
        ServiceInstaller installer = new ServiceInstaller(getRestUrl(), SERVICE_NAME);
        String command = installer.recipePath(SGTestHelper.getBuildDir() + "/recipes/services/" + SERVICE_NAME).debugEvent("install").timeoutInMinutes(20).buildCommand(true).install();

        AtomicReference<CommandTestUtils.ThreadSignal> outputReadingThreadSignal;
        outputReadingThreadSignal = new AtomicReference<CommandTestUtils.ThreadSignal>();
        outputReadingThreadSignal.set(CommandTestUtils.ThreadSignal.STOPPED);

        outputReadingThreadSignal.set(CommandTestUtils.ThreadSignal.RUN_SIGNAL);
        String debugSearchString = "A debug environment will be waiting for you on";
        String debugSearchString2 = "after the instance has launched";
        CommandTestUtils.ProcessOutputPair processOutputPair = CommandTestUtils.runCommand(command, OPERATION_TIMEOUT*3, true, false, false, outputReadingThreadSignal, null, debugSearchString);
        killOutputReadingThread(outputReadingThreadSignal);

        String output = processOutputPair.getOutput();
        AssertUtils.assertTrue("service was installed although installation was paused in debug mode", !output.contains("successfully installed"));
        LogUtils.log("entering debugging mode");

        int startIndex = output.indexOf(debugSearchString) + debugSearchString.length() + 1;
        int endIndex = output.indexOf(debugSearchString2) - 1;
        if(!output.contains(debugSearchString) || !output.contains(debugSearchString2)){
            AssertUtils.assertFail("unexpected output format. startIndex = " + startIndex + ", endIndex = " + endIndex);
        }
        String serviceHostIp = output.substring(startIndex, endIndex);
        LogUtils.log("service host ip: " + serviceHostIp);

        final GSRestClient client = new GSRestClient("", "", new URL(getRestUrl()), PlatformVersion.getVersionNumber());
        String templateUrl = "ProcessingUnits/Names/default.tomcat/Instances/0/JVMDetails/EnvironmentVariables/GIGASPACES_CLOUD_TEMPLATE_NAME";
        String serviceTemplate = (String)client.getAdminData(templateUrl).get("GIGASPACES_CLOUD_TEMPLATE_NAME");

        String remoteDirectory = getService().getCloud().getCloudCompute().getTemplates().get(serviceTemplate).getRemoteDirectory();
        String debuggerFolderPath = remoteDirectory + "/cloudify-overrides/debugger";

        //installing "expect" package
        SSHUtils.runCommand(serviceHostIp, TIMEOUT_IN_MILLISECONDS, "sudo yum -y install expect", "ec2-user", getPemFile());

        //enabling execution
        SSHUtils.runCommand(serviceHostIp, TIMEOUT_IN_MILLISECONDS, "cd " + debuggerFolderPath + "; chmod +x debugger.sh", "ec2-user", getPemFile());

        //running
        output = SSHUtils.runCommand(serviceHostIp, TIMEOUT_IN_MILLISECONDS, "cd " + debuggerFolderPath + "; expect run-script.exp", "ec2-user", getPemFile());

        final String statusUrl = "ProcessingUnits/Names/default." + SERVICE_NAME + "/Instances/0/ProcessingUnit";

        AssertUtils.repetitiveAssertTrue("service was not installed after debug ended", new AssertUtils.RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
                try {
                    return WebUtils.isURLAvailable(new URL(getRestUrl() + "/admin/" + statusUrl));
                } catch (Exception e) {
                    LogUtils.log("still no such url: " + getRestUrl() + "/admin/" + statusUrl);
                }
                return false;
            }
        }, OPERATION_TIMEOUT * 2);

        final AtomicReference<String> status = new AtomicReference<String>((String)client.getAdminData(statusUrl).get("Status-Enumerator"));

        AssertUtils.repetitiveAssertTrue("service has not become intact after debugging", new AssertUtils.RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
                try {
                    status.set((String)client.getAdminData(statusUrl).get("Status-Enumerator"));
                } catch (RestException e) {
                    e.printStackTrace();
                }
                return status.get().equalsIgnoreCase("intact");
            }
        }, OPERATION_TIMEOUT * 2);

    }

    private void killOutputReadingThread(AtomicReference<CommandTestUtils.ThreadSignal> threadSignal) {

        if(threadSignal.get() == CommandTestUtils.ThreadSignal.STOPPED){
            //In case the thread is stopped return immediately
            return;
        }
        else{
            //In case the thread isn't stopped send a stop signal and wait for the thread to signal it's
            threadSignal.set(CommandTestUtils.ThreadSignal.STOP_SIGNAL);
            while(threadSignal.get() != CommandTestUtils.ThreadSignal.STOPPED){
                sleep(100);
            }
        }
    }

    @Override
    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        super.teardown();
    }

}
