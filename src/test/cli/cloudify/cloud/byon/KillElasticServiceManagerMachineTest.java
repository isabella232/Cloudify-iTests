package test.cli.cloudify.cloud.byon;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.machine.Machine;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class KillElasticServiceManagerMachineTest extends AbstractKillManagementTest {
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@BeforeMethod
	public void installApplication() throws IOException, InterruptedException {
		super.installApplication();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testKillMachine() throws Exception {
		super.testKillMachine();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	@Override
	protected void customizeCloud() throws Exception {
		super.customizeCloud();
		getService().setNumberOfManagementMachines(2);
	}
	
	@Override
	protected Machine getMachineToKill() {
		admin.getElasticServiceManagers().waitFor(1, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		ElasticServiceManager elasticServiceManager = admin.getElasticServiceManagers().getManagers()[0];
		Machine esmMachine = elasticServiceManager.getMachine();
		return esmMachine;
	}
}
