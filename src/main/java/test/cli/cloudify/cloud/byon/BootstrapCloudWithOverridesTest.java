package test.cli.cloudify.cloud.byon;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.machine.Machine;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.services.byon.ByonCloudService;
import test.cli.cloudify.cloud.services.byon.MultipleTemplatesByonCloudService;
import framework.utils.CloudBootstrapper;
import framework.utils.ServiceInstaller;

public class BootstrapCloudWithOverridesTest extends AbstractByonCloudTest {

	private MultipleTemplatesByonCloudService service = new MultipleTemplatesByonCloudService();
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		service.setNumberOfHostsForTemplate("TEMPLATE_1", 2);
		service.setNumberOfHostsForTemplate("TEMPLATE_2", 0);
		service.setNumberOfHostsForTemplate("TEMPLATE_3", 0);
		// this is for validation purposes, cant have an empty list as host-list in BYON
		service.addHostToTemplate("192.168.1.1","TEMPLATE_2");
		service.addHostToTemplate("192.168.1.2", "TEMPLATE_3");
		
		Map<String, Object> cloudOverrides = new HashMap<String, Object>();
		cloudOverrides.put("myEnvVariable", "DEFAULT_OVERRIDES_ENV_VARIABLE");

		CloudBootstrapper bootstrapper = new CloudBootstrapper()
			.cloudOverrides(cloudOverrides);
		service.setBootstrapper(bootstrapper);
		
		super.bootstrap(service);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void testOverridesDuringBootstrap() throws IOException, InterruptedException {
		
		List<Machine> machines = getAllMachines();
		assertOverrides(machines);
		
		ServiceInstaller installer = new ServiceInstaller(getRestUrl(), "tomcat");
		installer.setRecipePath("tomcat");
		
		installer.install();
		
		machines = getAllMachines();
		
		assertOverrides(machines);
		
		installer.uninstall();
		
	}
	
	@AfterMethod
	public void cleanup() throws IOException, InterruptedException {
		super.uninstallServiceIfFound("tomcat");
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	@Override
	protected String getCloudName() {
		return "byon";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
	
	private void assertOverrides(List<Machine> machines) {
		
		for (Machine machine : machines) {
			GridServiceAgent agent = machine.getGridServiceAgent();		
			String agentMachineEnvVariable = agent.getVirtualMachine().getDetails().
					getEnvironmentVariables().get(ByonCloudService.ENV_VARIABLE_NAME);
			
			// agent machine should have the overrides because we install with overrides
			assertEquals("DEFAULT_OVERRIDES_ENV_VARIABLE", agentMachineEnvVariable);
		}	
	}
}
