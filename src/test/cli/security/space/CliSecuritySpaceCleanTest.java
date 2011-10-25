package test.cli.security.space;

import static test.utils.LogUtils.log;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.core.GigaSpace;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.common.SpaceMessage;
import test.utils.CliUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;

/***
 * 
 * Topology: 1 GSA on 1 Machine, 1 GSM, 2 GSC's. (secured)
 * Deploy secured space 'A' partitioned 1,1 
 * fill 'A' with 1000 'SpaceMessage' objects
 *
 * Tests: test 'space clean' against 'A' with good credentials, bad credentials, no credentials,
 * and insufficient privileges and verify expected result in space instances 
 * (i.e: object count in each space instance: primary and backup)
 * Moreover, in case of expected failure, verify expected console output.
 * 
 * @author Dan Kilman
 *
 */
public class CliSecuritySpaceCleanTest extends CliSecurityAbstractSpaceTest {

    ProcessingUnit datagrid;
    GigaSpace gigaSpace;
    GigaSpace gigaSpaceBackup;
    
    @Override
    @BeforeMethod
    public void beforeTest() {
        
        super.beforeTest();
        
        ProcessingUnit pu = gsm.deploy(new SpaceDeployment("A")
            .partitioned(1, 1).maxInstancesPerVM(1).secured(true));
        
        assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));

        Space space = pu.waitForSpace(30, TimeUnit.SECONDS);
        assertNotNull(space);
        
        gigaSpace = space.getGigaSpace();
        
        log("Filling space 'A' with 1000 SpaceMessage objects");
        Random random = new Random();
        for (int i=0; i<1000; i++) {
            gigaSpace.write(new SpaceMessage("SpaceMessage", random.nextInt(1000)));
        }
        
        SpaceInstance[] aSpaces = pu.getSpace().getInstances();
        SpaceInstance aSpacePrimaryInstance = aSpaces[0].getMode().equals(SpaceMode.PRIMARY) ? aSpaces[0] : aSpaces[1]; 
        SpaceInstance aSpaceBackupInstance = aSpaces[0].getMode().equals(SpaceMode.BACKUP) ? aSpaces[0] : aSpaces[1]; 

        gigaSpace = aSpacePrimaryInstance.getGigaSpace();
        gigaSpaceBackup = aSpaceBackupInstance.getGigaSpace();
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceCleanWithGoodCredentials() {
        String[] args = {
                "-user", "Master",
                "-password", "master",
                "space", "clean",
                "-url", "jini://*/*/A",
                "-c"
            };
        
        CliUtils.invokeGSMainOn(args);
        assertEquals(0, gigaSpace.count(null));
        //Hack needed to count objects in a backup space instance
        try {
            CliUtils.assertCountInTarget(gigaSpaceBackup.getSpace(), 0, new SpaceMessage());
        } catch (Exception e) {
            Assert.fail("Error while checking backup space", e);
        }
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceCleanWithoutCredentials() {
        String[] args = {
                "space", "clean",
                "-url", "jini://*/*/A",
                "-c"
            };
        String mainOutput = CliUtils.invokeGSMainOn(args);
        
        String regex = "AuthenticationException";
        assertEquals("Testing: " + regex, 2, CliUtils.patternCounter(regex, mainOutput));
        
        assertEquals(1000, gigaSpace.count(null));
        //Hack needed to count objects in a backup space instance
        try {
            CliUtils.assertCountInTarget(gigaSpaceBackup.getSpace(), 1000, new SpaceMessage());
        } catch (Exception e) {
            Assert.fail("Error while checking backup space", e);
        }
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceCleanWithInsufficientPrivileges() {
        String[] args = {
                "-user", "Reader",
                "-password", "reader",
                "space", "clean",
                "-url", "jini://*/*/A",
                "-c"
            };
        String mainOutput = CliUtils.invokeGSMainOn(args);
    
        String regex = "AccessDeniedException";
        assertEquals("Testing: " + regex, 2, CliUtils.patternCounter(regex, mainOutput));
        
        regex = "User \\[Reader\\] lacks \\[Alter\\] privileges";
        assertEquals("Testing: " + regex, 2, CliUtils.patternCounter(regex, mainOutput));
        
        assertEquals(1000, gigaSpace.count(null));
        //Hack needed to count objects in a backup space instance
        try {
            CliUtils.assertCountInTarget(gigaSpaceBackup.getSpace(), 1000, new SpaceMessage());
        } catch (Exception e) {
            Assert.fail("Error while checking backup space", e);
        }
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceCleanWithBadCredentials() {
        String[] args = {
                "-user", "IDontExist",
                "-password", "WhatIsMyPassword",
                "space", "clean",
                "-url", "jini://*/*/A",
                "-c"
            };
        String mainOutput = CliUtils.invokeGSMainOn(args);
        
        String regex = "AuthenticationException";
        assertEquals("Testing: " + regex, 2, CliUtils.patternCounter(regex, mainOutput));
        
        regex = "BadCredentialsException";
        assertEquals("Testing: " + regex, 2, CliUtils.patternCounter(regex, mainOutput));
        
        assertEquals(1000, gigaSpace.count(null));
        //Hack needed to count objects in a backup space instance
        try {
            CliUtils.assertCountInTarget(gigaSpaceBackup.getSpace(), 1000, new SpaceMessage());
        } catch (Exception e) {
            Assert.fail("Error while checking backup space", e);
        }
    }

}
