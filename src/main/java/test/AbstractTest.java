package test;

import static framework.utils.LogUtils.log;


import org.openspaces.admin.Admin;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import framework.utils.AdminUtils;
import framework.utils.DumpUtils;
import framework.utils.LogUtils;
import framework.utils.SetupUtils;
import framework.utils.TeardownUtils;



public abstract class AbstractTest extends AbstractTestSupport {
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
    public void afterTest() throws Exception{
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

    
    ////////////


    
}
