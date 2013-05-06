package org.cloudifysource.quality.iTests.test.esm;

import java.util.concurrent.TimeUnit;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;

public class ByonStartNewMachineTest extends AbstractFromXenToByonGSMTest {
	

	@BeforeMethod
    public void beforeTest() {
		super.beforeTestInit();
	}
	
	@BeforeClass
	protected void bootstrap() throws Exception {
		super.bootstrapBeforeClass();
	}
	
	@AfterMethod
    public void afterTest() {
		super.afterTest();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardownAfterClass() throws Exception {
		super.teardownAfterClass();
	}
	
	//This test is covered in other tests
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void startMachineBasicTest() throws Exception {
		LogUtils.log("Start new machine test");
		
		AssertUtils.assertEquals(0, admin.getGridServiceContainers().getSize());	
		admin.getGridServiceAgents().waitFor(1,OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
		AssertUtils.assertEquals(1, admin.getGridServiceAgents().getSize());
		GridServiceAgent agent = startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(),OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
		AssertUtils.assertNotNull(agent);
		repetitiveAssertNumberOfGSAsAdded(2, OPERATION_TIMEOUT);
        LogUtils.log("Start new machine test passed");
	}
}
