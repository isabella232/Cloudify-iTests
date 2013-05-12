package org.cloudifysource.quality.iTests.framework.utils;

import java.util.concurrent.TimeUnit;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.IRepetitiveRunnable;
import iTests.framework.utils.LogUtils;
import iTests.framework.utils.SSHUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudService;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 3/13/13
 * Time: 3:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class DisconnectionUtils {

    public static void restartMachineAndWait(final String machine, final CloudService service) throws Exception {
        restartMachine(machine, service);
        LogUtils.log("Sleeping for 30 seconds...");
        Thread.sleep(30 * 1000);
        LogUtils.log("Waiting for SSH service to restore...");
        AssertUtils.repetitive(new IRepetitiveRunnable() {
            @Override
            public void run() throws Exception {
                SSHUtils.validateSSHUp(machine, service.getUser(), service.getApiKey());
            }
        }, (int) AbstractTestSupport.OPERATION_TIMEOUT);
        LogUtils.log("SSH restored. Sleeping for 30 seconds...");
        Thread.sleep(30 * 1000);
    }

    public static void restartMachine(String toKill, final CloudService service) {
        SSHUtils.runCommand(toKill, TimeUnit.SECONDS.toMillis(30),
                "sudo shutdown now -r", service.getUser(), service.getApiKey());
    }
}
