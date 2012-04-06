package test;

import static framework.utils.LogUtils.log;

import java.io.File;

import org.openspaces.admin.Admin;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.DumpUtils;
import framework.utils.LogUtils;
import framework.utils.SetupUtils;
import framework.utils.TeardownUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;



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
	
	public static ProcessingUnitInstance[] repetitiveAssertNumberOfInstances(ProcessingUnit pu, int expectedNumberOfInstances) {
		return AssertUtils.repetitiveAssertNumberOfInstances(pu,expectedNumberOfInstances);
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
