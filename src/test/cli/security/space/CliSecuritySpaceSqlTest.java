package test.cli.security.space;


import static framework.utils.LogUtils.log;

import java.util.Random;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
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
 * Tests: test 'space sql' on 'A' with good credentials
 * bad credentials, no credentials, insufficient privileges
 * and verify expected result in space instances 
 * (i.e: object count in each space instance: primary and backup)
 * Moreover, in case of expected failure, verify expected console output.
 * 
 * @author Dan Kilman
 *
 */
public class CliSecuritySpaceSqlTest extends CliSecurityAbstractSpaceTest {
    
    ProcessingUnit datagrid;

    @Override
    @BeforeMethod
    public void beforeTest() {
        
        super.beforeTest();
        
        log("Deploying space 'A' partitioned 1,1");
        datagrid = gsm.deploy(new SpaceDeployment("A").partitioned(1, 1).maxInstancesPerVM(1).secured(true));
        
        assertTrue(datagrid.waitFor(datagrid.getTotalNumberOfInstances()));
        
        GigaSpace gigaSpace = datagrid.getSpaces()[0].getGigaSpace();
       
        log("Filling space 'A' with 1000 SpaceMessage objects");
        Random random = new Random();
        for (int i=0; i<1000; i++) {
            gigaSpace.write(new SpaceMessage("SpaceMessage", random.nextInt(1000)));
        }
        
    }
    
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceSqlWithGoodCredentials() {
        
        String[] args = {
          "-user", "Master",
          "-password", "master",
          "space", "sql",
          "-url", "jini://*/*/A",
          "-query", "select", "count(*)", "from", "test.cli.common.SpaceMessage"
        };
        
        String mainOutput = CliUtils.invokeGSMainOn(args);
        
        assertEquals("Testing: 1000", 1, CliUtils.patternCounter("1000", mainOutput));
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceSqlWithNoCredentials() {
        
        String[] args = {
          "space", "sql",
          "-url", "jini://*/*/A",
          "-query", "select", "count(*)", "from", "test.cli.common.SpaceMessage"
        };
        
        String mainOutput = CliUtils.invokeGSMainOn(args);
        
        String regex = "Failed due to a security error: No authentication details were supplied";
        assertEquals("Testing: " + regex, 1, CliUtils.patternCounter(regex, mainOutput));
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceSqlWithBadCredentials() {
        
        String[] args = {
          "-user", "NoSuchUser",
          "-password", "BasPassword",
          "space", "sql",
          "-url", "jini://*/*/A",
          "-query", "select", "count(*)", "from", "test.cli.common.SpaceMessage"
        };
        
        String mainOutput = CliUtils.invokeGSMainOn(args);
        
        String regex = "BadCredentialsException";
        assertEquals("Testing: " + regex, 1, CliUtils.patternCounter(regex, mainOutput));
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceSqlWithInsufficientPrivileges() {
        
        String[] args = {
          "-user", "Writer",
          "-password", "writer",
          "space", "sql",
          "-url", "jini://*/*/A",
          "-query", "select", "count(*)", "from", "test.cli.common.SpaceMessage"
        };
        
        String mainOutput = CliUtils.invokeGSMainOn(args);
        
        String regex = "User \\[Writer\\] lacks \\[Read\\] privileges";
        assertEquals("Testing: " + regex, 1, CliUtils.patternCounter(regex, mainOutput));
    }
    
}
