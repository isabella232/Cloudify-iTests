package test.cli.security.space;

import static test.utils.LogUtils.log;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.common.SpaceMessage;
import test.utils.CliUtils;

/***
 * 
 * Topology: 1 GSA on 1 Machine, 1 GSM, 2 GSC's. (secured)
 * Deploy secure space 'A' partitioned 2,1 
 * fill 'A' with 1000 'SpaceMessage' objects
 *
 * Tests: test 'space list' output with sufficient privileges / insufficient privileges.
 * a. verify hidden object/template count in case of insufficient privileges
 * b. verify correct object/template count in case of sufficient privileges
 * 
 * @author Dan Kilman
 *
 */
public class CliSecuritySpaceListTest extends CliSecurityAbstractSpaceTest {

    @Override
    @BeforeMethod
    public void beforeTest() {
        
        super.beforeTest();
        
        ProcessingUnit pu = gsm.deploy(new SpaceDeployment("A")
            .partitioned(2, 1).maxInstancesPerVM(1).secured(true));
        
        assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));

        Space space = pu.waitForSpace(30, TimeUnit.SECONDS);
        assertNotNull(space);
        
        GigaSpace gigaSpace = space.getGigaSpace();
        
        log("Filling space 'A' with 1000 SpaceMessage objects");
        Random random = new Random();
        for (int i=0; i<1000; i++) {
            gigaSpace.write(new SpaceMessage("SpaceMessage", random.nextInt(1000)));
        }
    }
    
    // cli is invoked without credendtials, thus we expect ojbect count to be hidden
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testHiddenObjectCountWithInsufficientPrivileges() {
        String mainOutput = CliUtils.invokeGSMainOn("space", "list");
        String regex = "(?:A[ ]+)([^ ]+)(?:[ ]+)([^ ]+)(?:[ ]+Yes[ ]+Yes[ ]+)(?:Yes|No)(?:[ ]+No[ ]+[0-9]+[ ]+default)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(mainOutput);
        
        int count = 0;
        while (matcher.find()) {
            for (int i = 1; i <= 2; i++) {
                String grp = matcher.group(i);
                assertEquals("****", grp);
                count++;
            }
        }
        // 4 for object count, 4 for template count
        assertEquals(8, count);
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testObjectCountWithSufficientPrivileges() {
        String[] args = {
                "-user", "Master",
                "-password", "master",
                "space", "list"
        };
        String mainOutput = CliUtils.invokeGSMainOn(args);
        String regex = "(?:A[ ]+)([^ ]+)(?:[ ]+)([^ ]+)(?:[ ]+Yes[ ]+Yes[ ]+)(?:Yes|No)(?:[ ]+No[ ]+[0-9]+[ ]+default)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(mainOutput);
        
        int count = 0;
        while (matcher.find()) {
            try {
                // group 1 is object count
                int num = Integer.parseInt(matcher.group(1));
                count += num;
                
                // group 2 is template count 
                num = Integer.parseInt(matcher.group(2));
                assertEquals(0, num);
            } catch (NumberFormatException e) {
                Assert.fail("Expected number format", e);
            }
        }
        // we expect 2000 object to be in all space instances (1000 in primaries, 1000 in backups)
        assertEquals(count, 2000);
    }
    
}
