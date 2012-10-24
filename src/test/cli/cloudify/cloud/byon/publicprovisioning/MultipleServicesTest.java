package test.cli.cloudify.cloud.byon.publicprovisioning;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import framework.utils.ProcessingUnitUtils;

public class MultipleServicesTest extends AbstractPublicProvisioningByonCloudTest {
			
	@Override
	protected String getCloudName() {
		return "byon";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testTwoPublic() throws IOException, InterruptedException {
		
		// this should install both services on the same machine
		installPublicProvisioningServiceAndWait("groovy-one", 1, 128, 0);
		installPublicProvisioningServiceAndWait("groovy-two", 1, 128, 0);
		
		// check that it is the case
		ProcessingUnit groovy1 = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("default", "groovy-one"), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		ProcessingUnit groovy2 = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("default", "groovy-two"), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		ProcessingUnitInstance[] instances1 = groovy1.getInstances();
		ProcessingUnitInstance[] instances2 = groovy2.getInstances();
		assertTrue("Wrong number of groovy instances discovered", instances1.length == 1);
		assertTrue("Wrong number of groovy instances discovered", instances2.length == 1);
		assertTrue("groovy instances were installed on 2 different machines", instances1[0].getMachine().equals(instances2[0].getMachine()));
		
		uninstallServiceAndWait("groovy-one");
		uninstallServiceAndWait("groovy-two");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void testOneDedicatedOnePublic() throws IOException, InterruptedException {
		
		// this should install both instances of 'groovy-public' on the same machine
		installPublicProvisioningServiceAndWait("groovy-public", 2, 128, 0);
		
		// this should install two instances of 'groovy-dedicated' on two different machine (and not the one for groovy-one)
		installDedicatedProvisioningServiceAndWait("groovy-dedicated", 2);
		
		// check that it is the case
		ProcessingUnit groovyPublic = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("default", "groovy-public"), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		ProcessingUnit groovyDedicated = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("default", "groovy-dedicated"), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);

		Machine groovyPublicMachine = groovyPublic.getInstances()[0].getMachine();
		
		Set<Machine> groovyDedicatedMachines = ProcessingUnitUtils.getMachinesFromPu(groovyDedicated);
		
		assertEquals("2 instances of groovy-two were not installed on two different machines", 2, groovyDedicatedMachines.size());
		
		// after we know they were installed on 2 different machines, make sure non of these machines is the one installed from the public service
		assertTrue("an instance of groovy-two was installed on the same machine as groovy-one, even though one is dedicated provisioning and the other is public", !groovyDedicatedMachines.contains(groovyPublicMachine));
		
		uninstallServiceAndWait("groovy-public");
		uninstallServiceAndWait("groovy-dedicated");
		
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}
}
