package org.cloudifysource.quality.iTests.test.esm.stateless.manual.memory;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DedicatedStatelessManualByonCleanupFailureTest extends AbstractStatelessManualByonCleanupTest {

	private static final String EXPECTED_ESM_LOG = "on-service-uninstalled-failure-injection";
	private static final String CLOUD_DRIVER_CLASS_NAME = "org.cloudifysource.quality.iTests.OnServiceUninstalledFailureByonProvisioningDriver";
	
	DedicatedStatelessManualByonCleanupFailureTest() {
		super(EXPECTED_ESM_LOG, CLOUD_DRIVER_CLASS_NAME);
	}
	
	@BeforeMethod
    public void beforeTest() {
		super.beforeTestInit();
	}
	
	@BeforeClass
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@AfterMethod
    public void afterTest() {
		super.afterTest();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardownAfterClass() throws Exception {
		super.teardownAfterClass();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void test() {
	    super.testCloudCleanup();
    }
}
