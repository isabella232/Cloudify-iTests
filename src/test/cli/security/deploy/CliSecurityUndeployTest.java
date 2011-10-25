package test.cli.security.deploy;

import static test.utils.LogUtils.log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.events.ProcessingUnitAddedEventListener;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.CliUtils;

/***
 * 
 * Topology: 1 GSA on 1 machine, 1 GSM, 1 GCS (all secured), 1 SpaceDeployment 'A'
 * 
 * Tests: test undeployment throught cli with good credentials, bad credentials, insufficent privileges.
 * and check expected result (i.e: successful undeployment in case of good credentials, and no undeployment/error message, otherwise)
 * 
 * @author Dan Kilman
 *
 */
public class CliSecurityUndeployTest extends CliSecurityAbstractDeployTest {

    @Override
    @BeforeMethod
    public void beforeTest() {

        super.beforeTest();
        
        ProcessingUnit pu = gsm.deploy(new SpaceDeployment("A"));
        assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));
        
    }
    
    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testGoodCredentials() throws InterruptedException {

        final CountDownLatch puRemoved = new CountDownLatch(1);
        admin.getProcessingUnits().getProcessingUnitAdded().add(new ProcessingUnitAddedEventListener() {
            public void processingUnitAdded(ProcessingUnit processingUnit) {
                puRemoved.countDown();
                log("PU Instance: " + processingUnit.getName() + " removed");
            }
        });
        
        String[] arguments = {
                "-user", "Master",
                "-password", "master",
                "undeploy", "A"
        };
        
        CliUtils.invokeGSMainOn(false, arguments);
        
        assertTrue(puRemoved.await(30, TimeUnit.SECONDS));  
        
    }
    
    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testBadCredentials() {

        String[] arguments = {
                "-user", "doesnotexist",
                "-password", "wrongpassword",
                "undeploy", "A"
        };
        
        String mainOutput = CliUtils.invokeGSMainOn(arguments);
        
        String regex = "AuthenticationException: Authentication request is invalid";
        assertEquals("Testing: " + regex, 1, CliUtils.patternCounter(regex, mainOutput));

        assertNotNull(admin.getProcessingUnits().waitFor("A", 30, TimeUnit.SECONDS));
    }
    
    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testInsufficentPrivileges() {

        String[] arguments = {
                "-user", "Reader",
                "-password", "reader",
                "undeploy", "A"
        };
        
        String mainOutput = CliUtils.invokeGSMainOn(arguments);

        String regex = "AccessDeniedException: User \\[Reader\\] lacks \\[Provision PU\\] privileges";
        assertEquals("Testing: " + regex, 1, CliUtils.patternCounter(regex, mainOutput));

        assertNotNull(admin.getProcessingUnits().waitFor("A", 30, TimeUnit.SECONDS));
        
    }

}
