package test.cli.security.space;

import static framework.utils.LogUtils.log;

import java.io.IOException;
import java.util.Random;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.CliUtils;

import test.cli.common.SpaceMessage;

/***
 * 
 * Topology: 1 GSA on 1 Machine, 1 GSM, 2 GSC's. (secured)
 * Deploy secure space 'A' partitioned 1,1 
 * fill 'A' with 1000 'SpaceMessage' objects
 *
 * Test: In the CLI (Interactive) verify that user with insufficient privileges cannot
 * query a a secured space (by url) after it has been queried by a user with sufficient
 * privileges in the same interactive session.
 * 
 * @author Dan Kilman
 *
 */
public class SwitchFromPrivilegedUserToUnprivilegedUserTest extends CliSecurityAbstractSpaceTest {

    ProcessingUnit datagrid;

    @Override
    @BeforeMethod
    public void beforeTest() {

        super.beforeTest();
        
        log("Deploying space 'A' partitioned 1,1");
        datagrid = gsm.deploy(new SpaceDeployment("A").partitioned(1, 1)
                .maxInstancesPerVM(1).secured(true));

        assertTrue(datagrid.waitFor(datagrid.getTotalNumberOfInstances()));

        GigaSpace gigaSpace = datagrid.getSpaces()[0].getGigaSpace();

        log("Filling space 'A' with 1000 SpaceMessage objects");
        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            gigaSpace.write(new SpaceMessage("SpaceMessage", random
                    .nextInt(1000)));
        }

    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public static void test() {
        try {
            String mainOutput = CliUtils
                    .invokeGSMainWithResourceAsInputStreamOn(
                            "test/cli/resources/SwitchFromPrivilegedUserInput.txt",
                            true, new String[] {});

            String regex = "1000";
            assertEquals("Testing: " + regex, 1, CliUtils.patternCounter(regex, mainOutput));

            regex = "User \\[Writer\\] lacks \\[Read\\] privileges";
            assertEquals("Testing: " + regex, 1, CliUtils.patternCounter(regex, mainOutput));
            
            
        } catch (IOException e) {
            Assert.fail("While running ", e);
        }
    }

}
