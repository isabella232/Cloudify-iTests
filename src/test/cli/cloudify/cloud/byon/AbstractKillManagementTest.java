package test.cli.cloudify.cloud.byon;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.junit.Assert;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import test.cli.cloudify.cloud.services.byon.ByonCloudService;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.IRepetitiveRunnable;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.SSHUtils;
import framework.utils.ScriptUtils;
import framework.utils.WebUtils;

public abstract class AbstractKillManagementTest extends AbstractByonCloudTest {

	private int numOManagementMachines = 2;

	private GridServiceManager[] managers;
	private ProcessingUnit tomcat;
	
	private static final long TEN_SECONDS = 10 * 1000;
	
	protected abstract Machine getMachineToKill();
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}

	@BeforeMethod
	public void installApplication() throws IOException, InterruptedException {
		LogUtils.log("installing application petclinic on byon");
		installApplicationAndWait(ScriptUtils.getBuildPath() + "/recipes/apps/petclinic-simple", "petclinic");
		admin.getGridServiceManagers().waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		managers = admin.getGridServiceManagers().getManagers();
		tomcat = admin.getProcessingUnits().waitFor("petclinic.tomcat", OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
	}

	
	public void testKillMachine() throws Exception {
		
		Machine machine = getMachineToKill();
		
		String machineAddress = machine.getHostAddress();
		
		GridServiceManager otherManager = getManagerInOtherHostThen(machineAddress);
		
		restartMachineAndWait(machineAddress);
		ProcessingUnitUtils.waitForManaged(tomcat, otherManager);
		LogUtils.log("waiting for esm to be re-discovered");
		admin.getElasticServiceManagers().waitFor(1, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				String spec = null;
				try {
					String hostAddress = tomcat.getInstances()[0].getGridServiceContainer().getMachine().getHostAddress();
					spec = "http://" + hostAddress + ":8080/petclinic";
					LogUtils.log("checking that url : " + spec + " is available");
					return ServiceUtils.isHttpURLAvailable(spec);
				} catch (final Exception e) {
					throw new RuntimeException("Error polling to URL : " + spec + " . Reason --> " + e.getMessage());
				} 
			}
		};
		AssertUtils.repetitiveAssertConditionHolds("petclinic url is not available!", condition, TEN_SECONDS, 1000);
		
		startManagement(machineAddress);
		
		Assert.assertTrue(admin.getGridServiceManagers().waitFor(numOManagementMachines, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
		ProcessingUnitUtils.waitForBackupGsm(tomcat, machine.getGridServiceManagers().getManagers()[0]);
	}

	/**
	 * this method accepts a host address and return a GridServiceManager that is not located at the given address.
	 * @param esmMachineAddress
	 * @return
	 */
	private GridServiceManager getManagerInOtherHostThen(
			String esmMachineAddress) {
		
		GridServiceManager result = null;
		for (GridServiceManager manager : managers) {
			if (!manager.getMachine().getHostAddress().equals(esmMachineAddress)) {
				result = manager;
				break;
			}
		}
		return result;
	}


	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}

	@Override
	protected void beforeTeardown() throws Exception {
		super.beforeTeardown();
		uninstallApplicationAndWait("petclinic");
	}

	@Override
	protected void customizeCloud() throws Exception {
		super.customizeCloud();
		getService().setNumberOfManagementMachines(numOManagementMachines);
	}

	//TODO: add support for windows machines (BYON doesn't support windows right now)
	private void startManagement(String machine1) throws IOException, DSLException {
		SSHUtils.runCommand(machine1, DEFAULT_TEST_TIMEOUT,  ByonCloudService.BYON_HOME_FOLDER + "/gigaspaces/tools/cli/cloudify.sh start-management", ByonCloudService.BYON_CLOUD_USER, ByonCloudService.BYON_CLOUD_PASSWORD);

	}

	private void restartMachineAndWait(final String machine) throws Exception {
		restartMachine(machine);
		AssertUtils.assertTrue(WebUtils.waitForHost(machine, (int)OPERATION_TIMEOUT));
		AssertUtils.repetitive(new IRepetitiveRunnable() {
			@Override
			public void run() throws Exception {
				SSHUtils.validateSSHUp(machine, ByonCloudService.BYON_CLOUD_USER, ByonCloudService.BYON_CLOUD_PASSWORD);
			}
		}, (int)OPERATION_TIMEOUT);
	}

	private void restartMachine(String toKill) {
		SSHUtils.runCommand(toKill, TimeUnit.SECONDS.toMillis(30),
				"sudo shutdown now -r", ByonCloudService.BYON_CLOUD_USER, ByonCloudService.BYON_CLOUD_PASSWORD);
	}
}