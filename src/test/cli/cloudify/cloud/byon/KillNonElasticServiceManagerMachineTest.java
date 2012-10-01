package test.cli.cloudify.cloud.byon;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.machine.Machines;
import org.testng.annotations.Test;

public class KillNonElasticServiceManagerMachineTest extends AbstractKillManagementTest {

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void testKillMachine() throws Exception {
		super.testKillMachine();
	}
	
	@Override
	protected Machine getMachineToKill() {
		
		Machine result = null;
		
		admin.getElasticServiceManagers().waitFor(1, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		ElasticServiceManager elasticServiceManager = admin.getElasticServiceManagers().getManagers()[0];
		Machine esmMachine = elasticServiceManager.getMachine();
		Machines machines = admin.getMachines();
		for (Machine machine : machines) {
			if (!esmMachine.equals(machine)) {
				result = machine;
			}
		}
		return result;
	}
}
