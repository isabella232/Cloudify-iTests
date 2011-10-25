package test;

import static test.utils.LogUtils.log;

import java.io.File;

import org.openspaces.admin.Admin;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import test.utils.AdminUtils;
import test.utils.AssertUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.DumpUtils;
import test.utils.LogUtils;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;


public abstract class AbstractTest {
	public final static long DEFAULT_TEST_TIMEOUT = 15 * 60 * 1000;
	public final static long OPERATION_TIMEOUT = 5 * 60 * 1000;
    protected Admin admin;

    @BeforeMethod
    public void beforeTest() {
        LogUtils.log("Test Configuration Started: "+ this.getClass());
        admin = newAdmin();
        SetupUtils.assertCleanSetup(admin);
    }

    /**
     * Tests may override the {@link Admin} creation if the default admin needs
     * modifications.
     *
     * @return a new Admin to be used by the test.
     */
    protected Admin newAdmin() {
        return AdminUtils.createAdmin();
    }

    /**
     * Tests may override the {@link Admin} creation if the default admin needs
     * modifications.
     *
     * @return a new Admin with all available machines locators to be used by the test.
     */
    protected Admin newAdminWithLocators() {
        return AdminUtils.createAdminWithLocators(admin);
    }

    @AfterMethod
    public void afterTest() {
    	if (admin != null) {
	    	try {
	            DumpUtils.dumpLogs(admin);
	        } catch (Throwable t) {
	            log("failed to dump logs", t);
	        }
	        try {
	            TeardownUtils.teardownAll(admin);
	        } catch (Throwable t) {
	            log("failed to teardown", t);
	        }
        	admin.close();
        	admin = null;
        }
	}

    protected File getTestFolder(){
        return DumpUtils.getTestFolder();
    }

    protected File getBuildFolder(){
        return DumpUtils.getTestFolder().getParentFile();    
    }

    
    ////////////
    
    public static void repetitiveAssertTrue(String message,
            								RepetitiveConditionProvider condition, 
            								long timeoutMilliseconds) {
    	AssertUtils.repetitiveAssertTrue(message, condition, timeoutMilliseconds);
    }
    
	public static void assertEquals(int expected, int actual) {
		AssertUtils.assertEquals(expected, actual);
	}

	
	public static void assertEquals(String msg, Object expected, Object actual) {
		AssertUtils.assertEquals(msg, expected, actual);
	}

	public static void assertEquals(Object expected, Object actual) {
		AssertUtils.assertEquals(expected, actual);
	}
	
	
	public static void AssertFail(String msg) {
		AssertUtils.AssertFail(msg);
	}
	
	public static void AssertFail(String msg, Exception e) {
		AssertUtils.AssertFail(msg, e);
	}

	public static void assertNotNull(String msg, Object obj) {
		AssertUtils.assertNotNull(msg, obj);
	}

	public static void assertNotNull(Object obj) {
		AssertUtils.assertNotNull(obj);
	}
	
	public static void assertTrue(String msg, boolean cond) {
		AssertUtils.assertTrue(msg, cond);
	}
	
	public static void assertTrue(boolean cond) {
		AssertUtils.assertTrue(cond);
	}
	
	public static void assertEquals(String msg, int a, int b) {
		AssertUtils.assertEquals(msg, a, b);
	}
	
	public static boolean sleep(long millisecs) {
		try {
			Thread.sleep(millisecs);
			return true;
		} catch (InterruptedException e) {
			// no op
		}
		return false;		
	}

    
}
