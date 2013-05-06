package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.publicprovisioning;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import iTests.framework.utils.AssertUtils;

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
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testTwoInstancesOfTheSameServiceOnOneMachine() throws IOException, InterruptedException {

		// this should install both service instances on the same machine
		installManualPublicProvisioningServiceAndWait(GROOVY, 2, 128, 0, DEFAULT_TEMPLATE_NAME, false);
		
		// check that it is the case
		ProcessingUnit groovy = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("default", GROOVY), AbstractTestSupport.OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		
		AssertUtils.assertNotNull("Failed to discover processing unit " + GROOVY + " even though it was installed succesfully", groovy);
		
		AbstractTestSupport.assertTrue(groovy.waitFor(2, AbstractTestSupport.OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
		AbstractTestSupport.assertTrue("groovy instances were installed on 2 different machines", groovy.getInstances()[0].getGridServiceContainer().getMachine().getHostAddress().equals(groovy.getInstances()[1].getGridServiceContainer().getMachine().getHostAddress()));
		
		uninstallServiceAndWait(GROOVY);
		
		super.scanForLeakedAgentNodes();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
}
