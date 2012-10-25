package test.cli.cloudify.cloud.byon.publicprovisioning;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MultipleServicesTest extends AbstractPublicProvisioningByonCloudTest {
			
	private static final String GROOVY_TWO = "groovy-two";
	private static final String GROOVY_ONE = "groovy-one";

	@Override
	protected String getCloudName() {
		return "byon";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testTwoServiceOnOneMachine() throws IOException, InterruptedException {
		
		// this should install both services on the same machine
		installManualPublicProvisioningServiceAndWait(GROOVY_ONE, 1, 128, 0, DEFAULT_TEMPLATE_NAME);
		installManualPublicProvisioningServiceAndWait(GROOVY_TWO, 1, 128, 0, DEFAULT_TEMPLATE_NAME);
		
		// check that it is the case
		ProcessingUnit groovy1 = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("default", GROOVY_ONE), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		ProcessingUnit groovy2 = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("default", GROOVY_TWO), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		ProcessingUnitInstance[] instances1 = groovy1.getInstances();
		ProcessingUnitInstance[] instances2 = groovy2.getInstances();
		assertTrue("Wrong number of groovy instances discovered", instances1.length == 1);
		assertTrue("Wrong number of groovy instances discovered", instances2.length == 1);
		assertTrue("groovy instances were installed on 2 different machines", instances1[0].getMachine().equals(instances2[0].getMachine()));
		
		uninstallServiceAndWait(GROOVY_ONE);
		uninstallServiceAndWait(GROOVY_TWO);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}
}
