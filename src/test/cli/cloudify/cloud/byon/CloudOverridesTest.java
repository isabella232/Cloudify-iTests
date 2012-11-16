package test.cli.cloudify.cloud.byon;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.machine.Machine;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.cloud.services.byon.ByonCloudService;
import test.cli.cloudify.cloud.services.byon.MultipleTemplatesByonCloudService;
import framework.utils.ApplicationInstaller;
import framework.utils.ServiceInstaller;

/**
 * CLOUDIFY-1128.
 * 
 * @author elip
 *
 */
public class CloudOverridesTest extends AbstractByonCloudTest {

	private final static String SIMPLE_RECIPE_FOLDER = CommandTestUtils.getPath("apps/USM/usm/simple-with-template");
	private final static String SIMPLE_APP_FOLDER = CommandTestUtils.getPath("apps/USM/usm/applications/simple-with-template");
	
	private MultipleTemplatesByonCloudService service = new MultipleTemplatesByonCloudService();
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		service.setNumberOfHostsForTemplate("TEMPLATE_1", 2);
		service.setNumberOfHostsForTemplate("TEMPLATE_2", 0);
		service.setNumberOfHostsForTemplate("TEMPLATE_3", 0);
		// this is for validation purposes, cant have an empty list as host-list in BYON
		service.addHostToTemplate("192.168.1.1","TEMPLATE_2");
		service.addHostToTemplate("192.168.1.2", "TEMPLATE_3");
		super.bootstrap(service);
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testInstallService() throws IOException, InterruptedException {
		
		File createOverridesFile = null;
		try {
			ServiceInstaller serviceInstaller = new ServiceInstaller(getRestUrl(), "simple");
			serviceInstaller.setRecipePath(SIMPLE_RECIPE_FOLDER);
			serviceInstaller.setCloudOverrides(createCloudOverrideProperties());
			
			serviceInstaller.install();
			
			// check that override was indeed taken under consideration
			assertOverrides("default.simple");
			
			serviceInstaller.uninstall();
			
			super.scanForLeakedAgentNodes();
			
		} finally {
			if (createOverridesFile != null) {
				createOverridesFile.delete();
			}
		}
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void testInstallApplication() throws IOException, InterruptedException {
		
		File cloudOverridesFile = null;
	
		try {
			ApplicationInstaller applicationInstaller = new ApplicationInstaller(getRestUrl(), "simple");
			applicationInstaller.setRecipePath(SIMPLE_APP_FOLDER);
			applicationInstaller.setCloudOverrides(createCloudOverrideProperties());
			
			applicationInstaller.install();
			
			// check that override was indeed taken under consideration
			assertOverrides("simple.simple");
			
			applicationInstaller.uninstall();
			
			super.scanForLeakedAgentNodes();

		} finally {
			if (cloudOverridesFile != null) {
				cloudOverridesFile.delete();
			}
		}
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void testSetInstances() throws IOException, InterruptedException {
		
		File createOverridesFile = null;
		try {
			ServiceInstaller serviceInstaller = new ServiceInstaller(getRestUrl(), "simple");
			serviceInstaller.setRecipePath(SIMPLE_RECIPE_FOLDER);
			serviceInstaller.setCloudOverrides(createCloudOverrideProperties());
			
			serviceInstaller.install();
			
			// check that override was indeed taken under consideration
			assertOverrides("default.simple");
			
			// increment instance, this should use the same cloud driver so no overrides needed
			serviceInstaller.setInstances(2);
			
			assertOverrides("default.simple");
			
			serviceInstaller.uninstall();
			
			super.scanForLeakedAgentNodes();
						
		} finally {
			if (createOverridesFile != null) {
				createOverridesFile.delete();
			}
		}
		
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
	
	private Map<String, Object> createCloudOverrideProperties() {
		
		Map<String, Object> cloudOverrides = new HashMap<String, Object>();
		cloudOverrides.put("myEnvVariable", "DEFAULT_OVERRIDES_ENV_VARIABLE");

		return cloudOverrides;
	}
	
	private void assertOverrides(final String puName) {
		
		Machine managementMachine = getManagementMachines().get(0);
		
		for (Machine agentMachine : getAgentMachines(puName)) {
			
			GridServiceAgent managementAgent = managementMachine.getGridServiceAgent();
			GridServiceAgent agentAgent = agentMachine.getGridServiceAgent();
			
			String managementMachineEnvVariable = managementAgent.getVirtualMachine().getDetails().
					getEnvironmentVariables().get(ByonCloudService.ENV_VARIABLE_NAME);
			String agentMachineEnvVariable = agentAgent.getVirtualMachine().getDetails().
					getEnvironmentVariables().get(ByonCloudService.ENV_VARIABLE_NAME);
			
			// management machine uses default env 
			assertEquals(ByonCloudService.ENV_VARIABLE_VALUE, managementMachineEnvVariable);
			// agent machine should have the overrides because we install with overrides
			assertEquals("DEFAULT_OVERRIDES_ENV_VARIABLE", agentMachineEnvVariable);
		}
		
		
	}
}
