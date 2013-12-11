package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.sharedprovisioning;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.ProcessingUnitUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudService;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudServiceManager;
import org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants;
import org.openspaces.admin.machine.Machine;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Set;

public class TenantSharedProvisioningTest extends AbstractSharedProvisioningByonCloudTest {
		
	private static final String APPLICATION_ONE = "application1";
	private static final String APPLICATION_TWO = "application2";
	
	private static final String SERVICE_ONE = "service1";
	private static final String SERVICE_TWO = "service2";

	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		
		CloudBootstrapper bootstrapper = new CloudBootstrapper();
		bootstrapper.user(SecurityConstants.USER_PWD_CLOUD_ADMIN).password(SecurityConstants.USER_PWD_CLOUD_ADMIN).secured(true)
			.securityFilePath(SecurityConstants.BUILD_SECURITY_FILE_PATH)
			.keystoreFilePath(SecurityConstants.DEFAULT_KEYSTORE_FILE_PATH)
			.keystorePassword(SecurityConstants.DEFAULT_KEYSTORE_PASSWORD);
		
		CloudService service = CloudServiceManager.getInstance().getCloudService(getCloudName());
		service.setBootstrapper(bootstrapper);
		super.bootstrap(service);
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testTwoTenant() throws Exception {

		super.installManualTenantSharedProvisioningApplicationAndWait(SecurityConstants.BEZEQ_GROUP, APPLICATION_ONE);
		super.installManualTenantSharedProvisioningApplicationAndWait(SecurityConstants.CELLCOM_GROUP, APPLICATION_TWO);
		
		Set<Machine> applicationOneMachines = ProcessingUnitUtils.getMachinesOfApplication(admin, APPLICATION_ONE);
		Set<Machine> applicationTwoMachines = ProcessingUnitUtils.getMachinesOfApplication(admin, APPLICATION_TWO);
		
		// make sure they dont overlap
		AssertUtils.assertTrue("applications have ovelapping machines even though the isolation is app based", 
				!applicationOneMachines.removeAll(applicationTwoMachines));
		
		ApplicationInstaller applicationOneInstaller = new ApplicationInstaller(getRestUrl(), APPLICATION_ONE);
		applicationOneInstaller.cloudifyPassword(SecurityConstants.USER_PWD_CLOUD_ADMIN).cloudifyUsername(SecurityConstants.USER_PWD_CLOUD_ADMIN);
		applicationOneInstaller.recipePath(APPLICATION_ONE);
		applicationOneInstaller.uninstall();
		
		ApplicationInstaller applicationTwoInstaller = new ApplicationInstaller(getRestUrl(), APPLICATION_TWO);
		applicationTwoInstaller.cloudifyPassword(SecurityConstants.USER_PWD_CLOUD_ADMIN).cloudifyUsername(SecurityConstants.USER_PWD_CLOUD_ADMIN);
		applicationTwoInstaller.recipePath(APPLICATION_TWO);
		applicationTwoInstaller.uninstall();

		
	}
	
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testTwoServicesOnOneTenant() throws Exception {
		super.installManualTenantSharedProvisioningServiceAndWait(SecurityConstants.BEZEQ_GROUP, SERVICE_ONE);
		super.installManualTenantSharedProvisioningServiceAndWait(SecurityConstants.BEZEQ_GROUP, SERVICE_TWO);
		
		Set<Machine> serviceOneMachines = ProcessingUnitUtils.getMachinesOfService(admin, ServiceUtils.getAbsolutePUName(CloudifyConstants.DEFAULT_APPLICATION_NAME, SERVICE_ONE));
		Set<Machine> serviceTwoMachines = ProcessingUnitUtils.getMachinesOfService(admin, ServiceUtils.getAbsolutePUName(CloudifyConstants.DEFAULT_APPLICATION_NAME, SERVICE_TWO));
		
		AssertUtils.assertTrue("services should be deployed on the same machine since they belong to the same tenant", 
				serviceOneMachines.equals(serviceTwoMachines));
		
		ServiceInstaller serviceOneInstaller = new ServiceInstaller(getRestUrl(), SERVICE_ONE);
		serviceOneInstaller.cloudifyPassword(SecurityConstants.USER_PWD_CLOUD_ADMIN).cloudifyUsername(SecurityConstants.USER_PWD_CLOUD_ADMIN);
		serviceOneInstaller.recipePath(SERVICE_ONE);
		serviceOneInstaller.uninstall();
		
		ServiceInstaller serviceTwoInstaller = new ServiceInstaller(getRestUrl(), SERVICE_TWO);
		serviceTwoInstaller.cloudifyPassword(SecurityConstants.USER_PWD_CLOUD_ADMIN).cloudifyUsername(SecurityConstants.USER_PWD_CLOUD_ADMIN);
		serviceTwoInstaller.recipePath(SERVICE_TWO);
		serviceTwoInstaller.uninstall();
	}
	
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
}
