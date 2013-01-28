package test.cli.cloudify.cloud.byon;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
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

	private static final int SERVICE_PORT = 10001;
	private static final String SERVICE_NAME = "mongod";
	private static final String USER = "tgrid";
	private static final String PASSWORD = "tgrid";
	private static String[] ips = null;
	private static Machine machineToDisconnect = null;
	private static List<Machine> machines = null;

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		
		super.bootstrap();
		WANemUtils.init();
	}

	@BeforeMethod(alwaysRun = true)
	private void install() throws Exception {
		
		installServiceAndWait(SGTestHelper.getBuildDir() + "/recipes/services/mongodb/mongod", SERVICE_NAME);
				
		machines = getManagementMachines();
		List<Machine> agentMachines = getAgentMachines("default." + SERVICE_NAME);		
		machines.addAll(agentMachines);

		ips = new String[machines.size()];
		int i = 0;
		for(Machine m : machines){
			ips[i] = m.getHostAddress();
			i++;
		}

		WANemUtils.addRoutingTableEntries(ips);

		machineToDisconnect = agentMachines.get(0);
	}

	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		
		if(ips != null){
			WANemUtils.removeRoutingTableEntries(ips);
		}
		
		if(machineToDisconnect != null){
			SSHUtils.runCommand(machineToDisconnect.getHostAddress(), OPERATION_TIMEOUT/3, "killall -9 " + SERVICE_NAME, USER, PASSWORD);
		}
		
		WANemUtils.reset();
		WANemUtils.destroy();
		super.teardown();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 3, enabled = true)
	public void testDisconnection() throws Exception {

		WANemUtils.disconnect(machineToDisconnect, machines);

		LogUtils.log("waiting for machine to disconnect");
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider(){

			@Override
			public boolean getCondition() {

				LogUtils.log(admin.getProcessingUnits().getProcessingUnit("default." + SERVICE_NAME).getInstances()[0].getMachine().getHostAddress());

				int activeInstances = admin.getProcessingUnits().getProcessingUnit("default." + SERVICE_NAME).getInstances().length;
//				LogUtils.log("active instances: " + activeInstances);
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
//				LogUtils.log("active instances: " + activeInstances);
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
				int plannedInstances = admin.getProcessingUnits().getProcessingUnit("default." + SERVICE_NAME).getNumberOfInstances();
				return ((actualInstances == 2) && (plannedInstances == 2));
			}

		};
		AssertUtils.repetitiveAssertTrue("service failed to stabilize after disconnection", condition, OPERATION_TIMEOUT/3);

		condition = new RepetitiveConditionProvider(){

			@Override
			public boolean getCondition() {
				
				return ServiceUtils.isPortFree(machineToDisconnect.getHostAddress(), SERVICE_PORT);
			}

		};
		AssertUtils.repetitiveAssertTrue("the machine that was disconnected (" + machineToDisconnect.getHostAddress() + ") has leaking java processes", condition, OPERATION_TIMEOUT/3);

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
