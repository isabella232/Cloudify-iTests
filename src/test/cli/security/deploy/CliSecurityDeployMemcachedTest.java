package test.cli.security.deploy;

import static framework.utils.LogUtils.log;
import static org.testng.AssertJUnit.assertNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.events.ProcessingUnitAddedEventListener;
import org.testng.annotations.Test;

import framework.utils.CliUtils;


/***
 * 
 * Topology: 1 GSA on 1 machine, 1 GSM, 1 GCS (all secured)
 * 
 * Tests: test deployment throught cli with good credentials, bad credentials, insufficent privileges.
 * and check expected result (i.e: successful deployment in case of good credentials, and no deployment/error message, otherwise)
 * 
 * @author Dan Kilman
 *
 */
public class CliSecurityDeployMemcachedTest extends CliSecurityAbstractDeployTest {

    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testGoodCredentials() throws InterruptedException {
        
        final CountDownLatch puAdded = new CountDownLatch(1);
        admin.getProcessingUnits().getProcessingUnitAdded().add(new ProcessingUnitAddedEventListener() {
            public void processingUnitAdded(ProcessingUnit processingUnit) {
                puAdded.countDown();
                log("PU Instance: " + processingUnit.getName() + " added");
            }
        });
    
        String[] arguments = {
                "-user", "Master",
                "-password", "master",
                "deploy-memcached", "/./testMemcached"
        };
        
        CliUtils.invokeGSMainOn(false, arguments);
        
        assertTrue(puAdded.await(30, TimeUnit.SECONDS));
        
    }
    
    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testBadCredentials() {

        String[] arguments = {
                "-user", "doesnotexist",
                "-password", "wrongpassword",
                "deploy-memcached", "/./testMemcached"
        };
        
        String mainOutput = CliUtils.invokeGSMainOn(arguments);
        
        String regex = "AuthenticationException: Authentication request is invalid";
        assertEquals("Testing: " + regex, 1, CliUtils.patternCounter(regex, mainOutput));

        assertNull(admin.getProcessingUnits().waitFor("testMemcached-memcached", 30, TimeUnit.SECONDS));
        
    }
    
    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testInsufficentPrivileges() {

        String[] arguments = {
                "-user", "Reader",
                "-password", "reader",
                "deploy-memcached", "/./testMemcached"
        };
        
        String mainOutput = CliUtils.invokeGSMainOn(arguments);

        String regex = "AccessDeniedException: User \\[Reader\\] lacks \\[Provision PU\\] privileges";
        assertEquals("Testing: " + regex, 1, CliUtils.patternCounter(regex, mainOutput));

        assertNull(admin.getProcessingUnits().waitFor("testMemcached-memcached", 30, TimeUnit.SECONDS));
        
    }
    
}
