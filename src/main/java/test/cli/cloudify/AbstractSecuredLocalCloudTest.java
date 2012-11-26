package test.cli.cloudify;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.Application;
import org.openspaces.admin.machine.Machine;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

import framework.utils.LocalCloudBootstrapper;
import framework.utils.LogUtils;

import test.cli.cloudify.CommandTestUtils.ProcessResult;
import test.cli.cloudify.security.SecurityConstants;

public class AbstractSecuredLocalCloudTest extends AbstractLocalCloudTest{
	
	@Override
	@BeforeMethod
	public void beforeTest() {
		LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
		beforeTest(bootstrapper);
		
	}

	public void beforeTest(LocalCloudBootstrapper bootstrapper) {
		
		user = bootstrapper.getUser();
		password = bootstrapper.getPassword();
		isSecured = true;

		LogUtils.log("Test Configuration Started: " + this.getClass());

		if (admin != null) {
			LogUtils.log("Admin has not been closed properly in the previous test. Closing old admin");
			admin.close();
			admin = null;
		}

		restUrl = "http://" + getLocalHostIpAddress() + ":" + restPort;

		if (checkIsDevEnv()) {
			LogUtils.log("Local cloud test running in dev mode, will use existing localcloud");
		} else {
			for (int i = 0; i < BOOTSTRAP_RETRIES_BEFOREMETHOD; i++) {

				try {
					if (!isRequiresBootstrap()) {
						break;
					}

					cleanUpCloudifyLocalDir();

					LogUtils.log("Tearing-down existing localclouds");
					final ProcessResult teardownResult = bootstrapper.teardown();
					if (teardownResult.getExitcode() != 0) {
						final String output = teardownResult.getOutput();
						if (!checkOutputForExceptions(output)) {
							// we assume that if teardown failed but no
							// exceptions were found in the output
							// then the reason was because no cloud was found.
							LogUtils.log("teardown failed because no cloud was found. proceeding with bootstrap.");
						} else {
							Assert.fail("Failed to teardown local cloud. output = "
									+ output);
						}
					}

					final ProcessResult bootstrapResult;

					bootstrapResult = bootstrapper.bootstrap();

					LogUtils.log(bootstrapResult.getOutput());
					Assert.assertEquals(bootstrapResult.getExitcode(), 0,
							"Bootstrap failed");
				} catch (final Throwable t) {
					LogUtils.log("Failed to bootstrap localcloud. iteration="
							+ i, t);

					if (i >= BOOTSTRAP_RETRIES_BEFOREMETHOD - 1) {
						Assert.fail("Failed to bootstrap localcloud after "
								+ BOOTSTRAP_RETRIES_BEFOREMETHOD + " retries.",
								t);
					}
				}

			}
		}

		Assert.assertFalse(isRequiresBootstrap(),
				"Cannot establish connection with localcloud");

		this.admin = getAdminWithLocators();
		final boolean foundLookupService = admin.getLookupServices().waitFor(1,
				WAIT_FOR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assert.assertTrue(foundLookupService,
				"Failed to discover lookup service after "
						+ WAIT_FOR_TIMEOUT_SECONDS + " seconds");

		final boolean foundMachine = admin.getMachines().waitFor(1,
				WAIT_FOR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assert.assertTrue(foundMachine, "Failed to discover machine after "
				+ WAIT_FOR_TIMEOUT_SECONDS + " seconds");
		final Machine[] machines = admin.getMachines().getMachines();
		Assert.assertTrue(machines.length >= 1, "Expected at least one machine");
		final Machine machine = machines[0];
		System.out.println("Machine ["
				+ machine.getHostName()
				+ "], "
				+ "TotalPhysicalMem ["
				+ machine.getOperatingSystem().getDetails()
				.getTotalPhysicalMemorySizeInGB()
				+ "GB], "
				+ "FreePhysicalMem ["
				+ machine.getOperatingSystem().getStatistics()
				.getFreePhysicalMemorySizeInGB() + "GB]]");

	}

	protected Application installApplication(final String applicationName, String user, String password) {

		setUserAndPassword(user, password);
		return installApplication(applicationName);
	}

	protected void uninstallApplication(final String applicationName, String user, String password) {

		setUserAndPassword(user, password);
		uninstallApplication(applicationName);
	}

	protected void installService(final String serviceName, String user, String password) {

		setUserAndPassword(user, password);
		installService(serviceName);
	}

	protected void uninstallService(final String serviceName, String user, String password) {

		setUserAndPassword(user, password);
		uninstallService(serviceName);
	}

	protected String listApplications(String user, String password){

		setUserAndPassword(user, password);
		return listApplications();
	}

	protected String connect(String user, String password){

		setUserAndPassword(user, password);
		return connect();
	}

	protected String login(String user, String password){

		if(user.equals(SecurityConstants.CLOUD_ADMIN_USER_PWD)){
			setUserAndPassword(SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD);	
		}
		else{
			setUserAndPassword(SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD);	
		}
		String output = "no output";

		try {
			output = runCommand(connectCommand() + ";" + loginCommand(user, password));
		} catch (IOException e) {
			Assert.fail("Failed to connect and login");
		} catch (InterruptedException e) {
			Assert.fail("Failed to connect and login");
		}

		return output;
	}

	protected String loginCommand(String user, String password){		
		return ("login " + user + " " + password);
	}

	protected void setUserAndPassword(String user, String password) {
		this.user = user;
		this.password = password;
	}

}
