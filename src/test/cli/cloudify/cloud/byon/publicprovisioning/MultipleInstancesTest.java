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

public class MultipleInstancesTest extends AbstractPublicProvisioningByonCloudTest {	
	
	private static final String GROOVY = "groovy";

	@Override
	protected String getCloudName() {
		return "byon";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testTwoInstancesOfTheSameServiceOnOneMachine() throws IOException, InterruptedException {

		// this should install both service instances on the same machine
		installManualPublicProvisioningServiceAndWait(GROOVY, 2, 128, 0, DEFAULT_TEMPLATE_NAME);
		
		// check that it is the case
		ProcessingUnit groovy = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("default", GROOVY), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		ProcessingUnitInstance[] instances = groovy.getInstances();
		assertTrue("Wrong number of groovy instances discovered", instances.length == 2);
		assertTrue("groovy instances were installed on 2 different machines", instances[0].getGridServiceContainer().getMachine().getHostAddress().equals(instances[1].getGridServiceContainer().getMachine().getHostAddress()));
		
		uninstallServiceAndWait(GROOVY);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}
}
