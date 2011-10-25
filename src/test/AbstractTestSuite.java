package test;

import static framework.utils.LogUtils.log;

import org.openspaces.admin.Admin;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import framework.utils.AdminUtils;
import framework.utils.DumpUtils;
import framework.utils.LogUtils;
import framework.utils.SetupUtils;
import framework.utils.TeardownUtils;


public abstract class AbstractTestSuite {

    public final static long DEFAULT_TEST_TIMEOUT = 15 * 60 * 1000;
    protected Admin admin;
    
    @BeforeClass
    public void beforeClass() {
        LogUtils.log("Test Configuration Started: "+ this.getClass());
        admin = AdminUtils.createAdmin();
        SetupUtils.assertCleanSetup(admin);
    }
    
    
    @AfterClass
    public void afterClass() {
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
