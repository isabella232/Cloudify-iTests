package test.cli.cloudify.cloud.byon.sharedprovisioning;

import java.io.IOException;
import java.util.Set;

import org.openspaces.admin.machine.Machine;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import framework.utils.AssertUtils;
import framework.utils.ProcessingUnitUtils;

public class TenantSharedProvisioningTest extends AbstractSharedProvisioningByonCloudTest {
		
	private static final String APPLICATION_ONE = "application1";
	private static final String APPLICATION_TWO = "application2";
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void test() throws IOException, InterruptedException {

		super.installManualTenantSharedProvisioningApplicationAndWait("ROLE_CLOUDADMINS", APPLICATION_ONE);
		super.installManualTenantSharedProvisioningApplicationAndWait("ROLE_APPMANAGERS", APPLICATION_TWO);
		
		Set<Machine> applicationOneMachines = ProcessingUnitUtils.getMachinesOfApplication(admin, APPLICATION_ONE);
		Set<Machine> applicationTwoMachines = ProcessingUnitUtils.getMachinesOfApplication(admin, APPLICATION_TWO);
		
		// make sure they dont overlap
		AssertUtils.assertTrue("applications have ovelapping machines even though the isolation is app based", 
				!applicationOneMachines.removeAll(applicationTwoMachines));
		
	}
	
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

}
