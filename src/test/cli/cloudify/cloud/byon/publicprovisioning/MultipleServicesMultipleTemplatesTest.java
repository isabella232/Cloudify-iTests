package test.cli.cloudify.cloud.byon.publicprovisioning;

import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.services.byon.MultipleTemplatesByonCloudService;

public class MultipleServicesMultipleTemplatesTest extends AbstractPublicProvisioningByonCloudTest {
	
	private static final String GROOVY_TWO = "groovy-two";
	private static final String GROOVY_ONE = "groovy-one";
	private MultipleTemplatesByonCloudService service = new MultipleTemplatesByonCloudService(this.getClass().getName());
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext, service);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testTwoServicesTwoTemplatesOnTwoMachines() throws Exception {
		
		String[] temlpateNames = service.getTemlpateNames();
		
		String groovy1TemplateName = temlpateNames[0];
		String groovy2TemplateName = temlpateNames[1];
		
		// this should install services on different machine because each service uses a different template
		// in byon the temlpate determines which host is used by the template. so if we define different hosts in different templates
		// different mahcines should be used when deploying using different templates
		
		installManualPublicProvisioningServiceAndWait(GROOVY_ONE, 1, 128, 0, groovy1TemplateName);
		installManualPublicProvisioningServiceAndWait(GROOVY_TWO, 1, 128, 0, groovy2TemplateName);
		
		// check that it is the case
		ProcessingUnit groovy1 = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("default", GROOVY_ONE), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		ProcessingUnit groovy2 = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("default", GROOVY_TWO), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		ProcessingUnitInstance[] instances1 = groovy1.getInstances();
		ProcessingUnitInstance[] instances2 = groovy2.getInstances();
		assertTrue("Wrong number of groovy instances discovered", instances1.length == 2);
		assertTrue("Wrong number of groovy instances discovered", instances2.length == 2);
		assertTrue("groovy-one and groovy-two instances were installed on the same machine", !instances1[0].getGridServiceContainer().getMachine().getHostAddress().equals(instances2[0].getGridServiceContainer().getMachine().getHostAddress()));
		
		uninstallServiceAndWait(GROOVY_ONE);
		uninstallServiceAndWait(GROOVY_TWO);

		
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}
}
