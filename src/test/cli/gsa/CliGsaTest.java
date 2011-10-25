package test.cli.gsa;

import static framework.utils.LogUtils.log;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.events.GridServiceContainerAddedEventListener;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.gsm.events.GridServiceManagerAddedEventListener;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.CliUtils;
import framework.utils.TeardownUtils;

import test.AbstractTest;

public class CliGsaTest extends AbstractTest {

    @Override
    @BeforeMethod
    public void beforeTest() {
        
        super.beforeTest();
        
        //Needed so GS.main could work properly
        String key = "com.gs.jini_lus.groups";
        String value = admin.getGroups()[0];
        System.setProperty(key, value);
        
        log("waiting for 1 machine");
        admin.getMachines().waitFor(1);

        log("waiting for 1 GSA");
        admin.getGridServiceAgents().waitFor(1);

    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void testStartGSC() {
        final CountDownLatch gscAdded = new CountDownLatch(1);
        GridServiceContainerAddedEventListener listener = new GridServiceContainerAddedEventListener() {
            public void gridServiceContainerAdded(GridServiceContainer gridServiceContainer) {
                gscAdded.countDown();
            }
        };
        
        admin.getGridServiceContainers().getGridServiceContainerAdded().add(listener, false);
        
        try {
            String mainOutput = CliUtils.invokeGSMainWithResourceAsInputStreamOn(
                    "test/cli/resources/StartGSCOnFirstGSAInput.txt",
                            true, new String[] {});
            
            log("CLI Output:\n" + mainOutput);
            
            assertTrue(gscAdded.await(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
            
        } catch (IOException e) {
            Assert.fail("While running ", e);
        } catch (InterruptedException e) {
            Assert.fail("While running ", e);
        } finally {
            admin.getGridServiceContainers().getGridServiceContainerAdded().remove(listener);
        }
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void testStartGSM() {

        final CountDownLatch gsmAdded = new CountDownLatch(1);
        GridServiceManagerAddedEventListener listener = new GridServiceManagerAddedEventListener() {
            public void gridServiceManagerAdded(GridServiceManager gridServiceManager) {
                gsmAdded.countDown();
            }
        };
        
        admin.getGridServiceManagers().getGridServiceManagerAdded().add(listener, false);
        
        try {
            String mainOutput = CliUtils.invokeGSMainWithResourceAsInputStreamOn(
                    "test/cli/resources/StartGSMOnFirstGSAInput.txt",
                            true, new String[] {});
            
            log("CLI Output:\n" + mainOutput);
            
            assertTrue(gsmAdded.await(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
            
        } catch (IOException e) {
            Assert.fail("While running ", e);
        } catch (InterruptedException e) {
            Assert.fail("While running ", e);
        } finally {
            admin.getGridServiceManagers().getGridServiceManagerAdded().remove(listener);
        }
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void testStartLUS() {
        try {
        	log("-----> debugging why the LUS is started although there is a global LUS of 2 <--------");
        	TeardownUtils.snapshot(admin);
        	log("-------------------------------------------------------------------------------------");
            String mainOutput = CliUtils.invokeGSMainWithResourceAsInputStreamOn(
                    "test/cli/resources/StartLUSOnFirstGSAInput.txt",
                            true, new String[] {});
            
            log("CLI Output:\n" + mainOutput);
            
            assertEquals(1, CliUtils.patternCounter("failed. Please see CLI log file for details.", mainOutput));
            
        } catch (IOException e) {
            Assert.fail("While running ", e);
        }
    }
    
}
