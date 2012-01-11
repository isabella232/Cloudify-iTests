package test.cli.cloudify;

import java.io.IOException;

import org.openspaces.admin.machine.Machine;
import org.testng.annotations.Test;

import org.cloudifysource.dsl.utils.ServiceUtils;

import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

public class BadUSMServiceDownAfterUninstallApplicationTest extends AbstractLocalCloudTest {

    @SuppressWarnings("unused")
    private Machine machineA;

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
    public void badUsmServiceDownTest() throws IOException, InterruptedException {
        final String serviceDir = CommandTestUtils.getPath("apps/USM/badUsmServices/simpleApplication");
        String command = "connect " + restUrl + ";install-application --verbose " + serviceDir + ";exit";

        CommandTestUtils.runCommandExpectedFail(command);
        machineA = admin.getMachines().getMachines()[0];

        command = "connect " + restUrl + ";" + "uninstall-application simple;exit";
        runCommand(command);

        RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
                return admin.getProcessingUnits().getProcessingUnit(ServiceUtils.getAbsolutePUName("simple", "simple")) == null;
            }
        };
        AssertUtils.repetitiveAssertTrue(null, condition, 10000);
    }

}
