package test;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.LogUtils;
import framework.utils.SetupUtils;


/***
 * 
 * <b>DO NOT RUN THIS TEST ON AN IDE.</b>
 * 
 * This is a dummy Test. It's sole purpose is to get the currently running java processes id before any test starts.
 * these should be solely the LUS and GSA available. note this only caputres process id's from GSA/LUS's running 
 * on linux machines, and it silently ignores windows machines.
 * If other tests are run manually from an IDE environment, this configuration will not run and remaining java processes
 * such as 'jboss' that are brought up, should be killed manaully on the relevant machine
 * See also: test.utils.StartupUtils
 */
public class StartTest extends AbstractTest {

    @Override
    @BeforeMethod
    public void beforeTest() {
        
        LogUtils.log("SGTest classpath is:");
        LogUtils.log(System.getProperty("java.class.path"));
        
        admin = newAdmin();
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test() {
        SetupUtils.fetchStartProcessesIDs(admin);
    }
    
}
