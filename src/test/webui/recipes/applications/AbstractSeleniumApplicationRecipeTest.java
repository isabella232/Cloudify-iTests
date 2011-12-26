package test.webui.recipes.applications;

import java.io.IOException;

import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import test.cli.cloudify.CommandTestUtils;
import test.webui.recipes.AbstractSeleniumRecipeTest;
import framework.utils.DumpUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.ScriptUtils;

public class AbstractSeleniumApplicationRecipeTest extends AbstractSeleniumRecipeTest {
	
	ProcessingUnit pu;
	private String currentApplication;
	public static final String MANAGEMENT = "management";
	Machine[] machines;
	private boolean wait = true;
	
	public void setCurrentApplication(String application) {
		this.currentApplication = application;
	}
	
	public void setWait(boolean wait) {
		this.wait = wait;
	}
	
	@BeforeMethod
	public void install() throws IOException, InterruptedException {		
		AdminFactory factory = new AdminFactory();
		LogUtils.log("Adding locators to new admin factory");
		factory.addLocator("127.0.0.1:4168");
		LogUtils.log("creating new admin from factory");
		admin = factory.createAdmin();
		LogUtils.log("Installing application " + currentApplication);
		if (installApplication(currentApplication, wait)) {
			LogUtils.log("retrieving webui url");
			ProcessingUnit webui = admin.getProcessingUnits().waitFor("webui");
			ProcessingUnitUtils.waitForDeploymentStatus(webui, DeploymentStatus.INTACT);
			assertTrue(webui != null);
			assertTrue(webui.getInstances().length != 0);	
			String url = ProcessingUnitUtils.getWebProcessingUnitURL(webui).toString();	
			startWebBrowser(url); 
		}
		else {
			Assert.fail("Failed to install application " + currentApplication);
		}
	}
	
	@AfterMethod
	public void uninstall() throws InterruptedException {
		if (admin != null) {
		    LogUtils.log("Uninstalling application");
		    uninstallApplication(currentApplication, true);
			if (!isDevMode()) {
				DumpUtils.dumpLogs(admin);
			}
			admin.close();
			admin = null;
		}
		stopWebBrowser();
	}
	
	public boolean installApplication(String applicationName, boolean wait) throws InterruptedException {
		String gigaDir = ScriptUtils.getBuildPath();	
		String pathToApplication = gigaDir + "/examples/" + applicationName;	
		String command = "connect localhost:8100;install-application --verbose -timeout 25 " + pathToApplication;
		try {
			if (wait) {
				LogUtils.log("Waiting for install-application to finish...");
				String output = CommandTestUtils.runCommandAndWait(command);
				boolean success = output.contains("installed successfully");
				if (success) {
					LogUtils.log("Cli returned that application was installed succesfully");
					return true;
				}
				else {
					LogUtils.log("Cli returned that application was not installed succesfully");
					return false;
				}
			}
			else {
				LogUtils.log("Not waiting for application to finish, assuming it will succeed");
				CommandTestUtils.runCommand(command);
				return true;
			}
		}
		catch (IOException e) {
			LogUtils.log("Failed to install application " + applicationName);
			LogUtils.log("Caught exception: " , e);
			return false;
		}
		catch (Throwable t) {
			LogUtils.log("Failed to install application " + applicationName);
			LogUtils.log("Caught throwable: " , t);
			return false;
		}
	}
	
	public boolean uninstallApplication(String applicationName, boolean wait) throws InterruptedException {	
		String command = "connect localhost:8100;uninstall-application --verbose -timeout 25 " + applicationName;
		try {
		if (wait) {
			LogUtils.log("Waiting for uninstall-application to finish...");
			String output = CommandTestUtils.runCommandAndWait(command);
			boolean success = output.contains("uninstalled successfully");
			if (success) {
				LogUtils.log("Cli returned that application was un-installed succesfully");
				return true;
			}
			else {
				LogUtils.log("Cli returned that application was not un-installed succesfully");
				return false;
			}
		}
		else {
			CommandTestUtils.runCommand(command);
			return true;
		}
		}
		catch (IOException e) {
			LogUtils.log("Failed to uninstall application " + applicationName);
			LogUtils.log("Caught exception: " , e);
			return false;
		}
		catch (Throwable t) {
			LogUtils.log("Failed to uninstall application " + applicationName);
			LogUtils.log("Caught throwable: " , t);
			return false;
		}
	}
}
