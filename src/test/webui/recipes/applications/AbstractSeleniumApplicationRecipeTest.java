package test.webui.recipes.applications;

import java.io.IOException;

import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import test.cli.cloudify.CommandTestUtils;
import test.webui.recipes.AbstractSeleniumRecipeTest;
import framework.utils.DumpUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.ScriptUtils;

public class AbstractSeleniumApplicationRecipeTest extends AbstractSeleniumRecipeTest {
	
	public static final String MANAGEMENT = "management";

	private String currentApplication;
	private boolean wait = true;
	
	public void setCurrentApplication(String application) {
		this.currentApplication = application;
		LogUtils.log("Current Application is : " + currentApplication);
	}
	
	public void setWait(boolean wait) {
		this.wait = wait;
	}
	
	@BeforeMethod
	public void install() throws IOException, InterruptedException {	
		assertTrue("Seems like a bootstrap has not been executed, skipping test", isBootstraped());
		AdminFactory factory = new AdminFactory();
		LogUtils.log("Adding locators to new admin factory");
		factory.addLocator("127.0.0.1:4168");
		LogUtils.log("creating new admin from factory");
		admin = factory.createAdmin();
		LogUtils.log("Installing application " + currentApplication);
		assertTrue("Failed To install application " + currentApplication, installApplication(currentApplication, wait));
		LogUtils.log("retrieving webui url");
		ProcessingUnit webui = admin.getProcessingUnits().waitFor("webui");
		ProcessingUnitUtils.waitForDeploymentStatus(webui, DeploymentStatus.INTACT);
		assertTrue(webui != null);
		assertTrue(webui.getInstances().length != 0);	
		String url = ProcessingUnitUtils.getWebProcessingUnitURL(webui).toString();	
		startWebBrowser(url); 
	}
	
	@AfterMethod(alwaysRun = true)
	public void uninstall() throws InterruptedException, IOException {
		stopWebBrowser();
		if (isApplicationInstalled(currentApplication)) {
			LogUtils.log("Uninstalling application");
			assertTrue("Failed to uninstall application " + currentApplication, uninstallApplication(currentApplication, true));
		}
		if (admin != null) {
			if (!isDevMode()) {
				DumpUtils.dumpLogs(admin);
			}
			admin.close();
			admin = null;
		}
	}
	
	public boolean installApplication(String applicationName, boolean wait) throws InterruptedException, IOException {
		String gigaDir = ScriptUtils.getBuildPath();	
		String pathToApplication = gigaDir + "/examples/" + applicationName;	
		String command = "connect localhost:8100;install-application --verbose -timeout 25 " + pathToApplication;
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
	
	public boolean uninstallApplication(String applicationName, boolean wait) throws InterruptedException, IOException {	
		String command = "connect localhost:8100;uninstall-application --verbose -timeout 25 " + applicationName;
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
	
	private boolean isApplicationInstalled(String applicationName) {
		Application app = admin.getApplications().getApplication(applicationName);
		return (app != null);
		
	}
}
