package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.failover;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.machine.Machine;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class KillNonElasticServiceManagerMachineTest extends AbstractKillManagementTest {

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@BeforeMethod
	public void installApplication() throws IOException, InterruptedException {
		super.installApplication();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testKillMachine() throws Exception {
		super.testKillMachine();
	}
	
	@AfterMethod(alwaysRun = true)
	public void cleanup() throws IOException, InterruptedException {
		super.uninstallApplicationIfFound("petclinic");
		super.scanForLeakedAgentNodes();
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
		
		Machine result = null;
		
		admin.getElasticServiceManagers().waitFor(1, AbstractTestSupport.OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		ElasticServiceManager elasticServiceManager = admin.getElasticServiceManagers().getManagers()[0];
		Machine esmMachine = elasticServiceManager.getMachine();
		Machine[] machines = getGridServiceManagerMachines();
		for (Machine machine : machines) {
			if (!esmMachine.equals(machine)) {
				result = machine;
			}
		}
		return result;
	}
}
