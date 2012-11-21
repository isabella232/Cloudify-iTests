package test.cli.cloudify.cloud.byon.publicprovisioning;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.pu.ProcessingUnit;
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
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testTwoInstancesOfTheSameServiceOnOneMachine() throws IOException, InterruptedException {

		// this should install both service instances on the same machine
		installManualPublicProvisioningServiceAndWait(GROOVY, 2, 128, 0, DEFAULT_TEMPLATE_NAME);
		
		// check that it is the case
		ProcessingUnit groovy = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("default", GROOVY), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		assertTrue(groovy.waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
		assertTrue("groovy instances were installed on 2 different machines", groovy.getInstances()[0].getGridServiceContainer().getMachine().getHostAddress().equals(groovy.getInstances()[1].getGridServiceContainer().getMachine().getHostAddress()));
		
		uninstallServiceAndWait(GROOVY);
		
		super.scanForLeakedAgentNodes();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
}
