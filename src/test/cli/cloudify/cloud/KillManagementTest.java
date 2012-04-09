package test.cli.cloudify.cloud;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.byon.ByonCloudService;
import framework.tools.SGTestHelper;
import framework.utils.AssertUtils;
import framework.utils.IRepetitiveRunnable;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.SSHUtils;
import framework.utils.ScriptUtils;
import framework.utils.WebUtils;

public class KillManagementTest extends AbstractCloudTest{
	private URL petClinicUrl;
	private ByonCloudService service;
	private String cloudName = "byon";
	private int numOManagementMachines = 2;
	final private static String USERNAME = "tgrid";
	final private static String PASSWORD= "tgrid";
	private volatile boolean run = true;
	private ExecutorService threadPool;

	@BeforeMethod
	public void before() throws IOException, InterruptedException{
		AdminFactory adminFactory = new AdminFactory();
		admin = adminFactory.addGroup("gigaspaces-Cloudify-2.1.0-m4").createAdmin();
		
		
		service = new ByonCloudService();
		//TODO: this is only for dev mode testing
		//service.setIpList("192.168.9.59,192.168.9.60,192.168.9.61,192.168.9.104");
		service.setNumberOfManagementMachines(2);
		service.setMachinePrefix(this.getClass().getName());
		service.bootstrapCloud();
		setService(service);
		String hostIp = service.getRestUrl().substring(0, service.getRestUrl().lastIndexOf(':'));
		petClinicUrl = new URL(hostIp + ":8080/petclinic-mongo/");
		threadPool = Executors.newFixedThreadPool(1);

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void testPetclinic() throws Exception {
		LogUtils.log("installing application petclinic on " + cloudName);
		installApplicationAndWait(ScriptUtils.getBuildPath() + "/examples/petclinic", "petclinic");

		Future<Void> ping = threadPool.submit(new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				while(run){
					Assert.assertTrue(WebUtils.isURLAvailable(petClinicUrl));
					TimeUnit.SECONDS.sleep(10);
				}
				return null;
			}
		});

		Assert.assertTrue(admin.getGridServiceManagers().waitFor(numOManagementMachines, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
		GridServiceManager[] gsms = admin.getGridServiceManagers().getManagers();
		ProcessingUnit pu = admin.getProcessingUnits().getProcessingUnit("petclinic.mongod");
		GridServiceManager gsm1 = pu.waitForManaged();
		GridServiceManager gsm2 = gsms[0].equals(gsm1) ? gsms[1] : gsms[0];
		final String machine1 = gsm1.getMachine().getHostAddress();

		restartMachineAndWait(machine1);
		ProcessingUnitUtils.waitForManaged(pu, gsm2);
		startManagement(machine1);
		Assert.assertTrue(admin.getGridServiceManagers().waitFor(numOManagementMachines, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
		gsms = admin.getGridServiceManagers().getManagers();
		gsm1 = gsms[0].equals(gsm2) ? gsms[1] : gsms[0];
		ProcessingUnitUtils.waitForBackupGsm(pu, gsm1);

		final String machine2 = gsm2.getMachine().getHostAddress();
		restartMachineAndWait(machine2);

		ProcessingUnitUtils.waitForManaged(pu, gsm1);
		startManagement(machine2);
		Assert.assertTrue(admin.getGridServiceManagers().waitFor(numOManagementMachines, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
		gsms = admin.getGridServiceManagers().getManagers();
		gsm2 = gsms[0].equals(gsm1) ? gsms[1] : gsms[0];
		ProcessingUnitUtils.waitForBackupGsm(pu, gsm2);


		run = false;
		ping.get();
	}

	//TODO: add support for windows machines
	//TODO: get the remote folder from the groovy file
	private void startManagement(String machine1) {
		SSHUtils.runCommand(machine1, DEFAULT_TEST_TIMEOUT, "/tmp/gs-files/gigaspaces/tools/cli/cloudify.sh start-management", USERNAME, PASSWORD);
		
	}

	private void restartMachineAndWait(final String machine) throws Exception {
		restartMachine(machine);
		AssertUtils.assertTrue(WebUtils.waitForHost(machine, (int)OPERATION_TIMEOUT));
		AssertUtils.repetitive(new IRepetitiveRunnable() {
			@Override
			public void run() throws Exception {
				SSHUtils.validateSSHUp(machine, USERNAME, PASSWORD);
			}
		}, (int)OPERATION_TIMEOUT);
	}

	private void restartMachine(String toKill) {
		SSHUtils.runCommand(toKill, TimeUnit.SECONDS.toMillis(30),
				"sudo shutdown now -r", USERNAME, PASSWORD);
	}





	@AfterMethod(alwaysRun = true)
	public void teardown() throws IOException {
		threadPool.shutdownNow();
		try {
			service.teardownCloud();
		}
		catch (Throwable e) {
			LogUtils.log("caught an exception while tearing down " + service.getCloudName(), e);
			sendTeardownCloudFailedMail(cloudName, e);
		}
		File backupByonDir = new File(SGTestHelper.getBuildDir() + "/tools/cli/plugins/esc/byon.backup");
		File currentByonDir = new File(SGTestHelper.getBuildDir() + "/tools/cli/plugins/esc/byon");
		if (backupByonDir.exists()){
			FileUtils.deleteDirectory(currentByonDir);
			FileUtils.moveDirectory(backupByonDir, currentByonDir);
		}

	}


}


