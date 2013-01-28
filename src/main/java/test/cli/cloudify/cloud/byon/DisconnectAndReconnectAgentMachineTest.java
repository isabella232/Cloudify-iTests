package test.cli.cloudify.cloud.byon;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.machine.Machine;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.tools.SGTestHelper;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.LogUtils;
import framework.utils.SSHUtils;
import framework.utils.WANemUtils;

public class DisconnectAndReconnectAgentMachineTest extends AbstractByonCloudTest{

	private static final String SERVICE_NAME = "mongod";
	private static final String USER = "tgrid";
	private static final String PASSWORD = "tgrid";

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		
		super.bootstrap(this.cloudService);
		super.afterBootstrap();
		admin = super.createAdminAndWaitForManagement();

		WANemUtils.init();

	}

	@BeforeMethod(alwaysRun = true)
	private void install() throws Exception {
		installServiceAndWait(SGTestHelper.getBuildDir() + "/recipes/services/mongodb/mongod", SERVICE_NAME);
	}

	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
		WANemUtils.destroy();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 3, enabled = true)
	public void testDisconnection() throws Exception {

		List<Machine> machines = getManagementMachines();
		List<Machine> agentMachines = getAgentMachines("default." + SERVICE_NAME);		
		machines.addAll(agentMachines);

		String[] ips = new String[machines.size()];
		int i = 0;
		for(Machine m : machines){
			ips[i] = m.getHostAddress();
			i++;
		}

		WANemUtils.addRoutingTableEntries(ips);

		final Machine machineToDisconnect = agentMachines.get(0);

		WANemUtils.disconnect(machineToDisconnect, machines);

		RepetitiveConditionProvider condition = new RepetitiveConditionProvider(){

			@Override
			public boolean getCondition() {

				int activeInstances = admin.getProcessingUnits().getProcessingUnit("default." + SERVICE_NAME).getInstances().length;
				LogUtils.log("active instances: " + activeInstances);
				return activeInstances == 1;
			}

		};
		AssertUtils.repetitiveAssertTrue("number of actual instances has not decreased by 1", condition, OPERATION_TIMEOUT/2);

		//waiting for ESM to start a new machine
		LogUtils.log("waiting for ESM to start a new machine");
		condition = new RepetitiveConditionProvider(){

			@Override
			public boolean getCondition() {
				int activeInstances = admin.getProcessingUnits().getProcessingUnit("default." + SERVICE_NAME).getInstances().length;
				LogUtils.log("active instances: " + activeInstances);
				return activeInstances == 2;			
			}
		};
		AssertUtils.repetitiveAssertTrue("number of actual instances has not increased by 1", condition, OPERATION_TIMEOUT/2);

		//reconnecting the network
		WANemUtils.reset();
		TimeUnit.SECONDS.sleep(10);

		//waiting for stabilization
		condition = new RepetitiveConditionProvider(){

			@Override
			public boolean getCondition() {
				int actualInstances = admin.getProcessingUnits().getProcessingUnit("default." + SERVICE_NAME).getInstances().length;
				LogUtils.log("active instances: " + actualInstances);
				int plannedInstances = admin.getProcessingUnits().getProcessingUnit("default." + SERVICE_NAME).getNumberOfInstances();
				LogUtils.log("planned instances: " + plannedInstances);
				return ((actualInstances == 2) && (plannedInstances == 2));
			}

		};
		AssertUtils.repetitiveAssertTrue("service failed to stabilize after disconnection", condition, OPERATION_TIMEOUT/3);

		condition = new RepetitiveConditionProvider(){

			@Override
			public boolean getCondition() {
				String output = SSHUtils.runCommand(machineToDisconnect.getHostAddress(), OPERATION_TIMEOUT/3, "jps", USER, PASSWORD);
				String[] outputSplit = output.split("\n");
				return outputSplit.length == 1;
			}

		};
		AssertUtils.repetitiveAssertTrue("the machine that was disconnected (" + machineToDisconnect + ") has leaking java processes", condition, OPERATION_TIMEOUT/3);

		WANemUtils.removeRoutingTableEntries(ips);
		
		SSHUtils.runCommand(machineToDisconnect.getHostAddress(), OPERATION_TIMEOUT/3, "killall -9 " + SERVICE_NAME, USER, PASSWORD);

	}

	@Override
	protected String getCloudName() {
		return "byon";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

}
