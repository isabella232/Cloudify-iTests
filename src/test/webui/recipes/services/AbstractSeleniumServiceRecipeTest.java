package test.webui.recipes.services;

import java.io.IOException;

import org.openspaces.admin.AdminFactory;
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

public class AbstractSeleniumServiceRecipeTest extends AbstractSeleniumRecipeTest {
	
	ProcessingUnit pu;
	private String currentRecipe;
	public static final String MANAGEMENT = "management";
	private boolean wait = true;
	
	public void setCurrentRecipe(String recipe) {
		this.currentRecipe = recipe;
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
		LogUtils.log("Installing service " + currentRecipe);
		if (installService(currentRecipe, wait)) {
			LogUtils.log("retrieving webui url");
			ProcessingUnit webui = admin.getProcessingUnits().waitFor("webui");
			ProcessingUnitUtils.waitForDeploymentStatus(webui, DeploymentStatus.INTACT);
			assertTrue(webui != null);
			assertTrue(webui.getInstances().length != 0);	
			String url = ProcessingUnitUtils.getWebProcessingUnitURL(webui).toString();	
			startWebBrowser(url); 
		}
		else {
			Assert.fail("Failed to install service " + currentRecipe);
		}
	}
	
	@AfterMethod
	public void uninstall() throws IOException, InterruptedException {
		if (admin != null) {
		    LogUtils.log("Uninstalling service");
		    uninstallService(currentRecipe, true);
			if (!isDevMode()) {
				DumpUtils.dumpLogs(admin);
			}
			admin.close();
			admin = null;
		}
		stopWebBrowser();
	}
	
	public boolean installService(String serviceName, boolean wait) throws IOException, InterruptedException {
		String gigaDir = ScriptUtils.getBuildPath();	
		String pathToService = gigaDir + "/recipes/" + serviceName;	
		String command = "connect localhost:8100;install-service --verbose -timeout 25 " + pathToService;
		try {
			if (wait) {
				LogUtils.log("Waiting for install-service to finish...");
				String output = CommandTestUtils.runCommandAndWait(command);
				boolean success = output.contains("successfully installed");
				if (success) {
					LogUtils.log("Cli returned that service was installed succesfully");
					return true;
				}
				else {
					LogUtils.log("Cli returned that service was not installed succesfully");
					return false;
				}
			}
			else {
				LogUtils.log("Not waiting for service to finish, assuming it will succeed");
				CommandTestUtils.runCommand(command);
				return true;
			}
		}
		catch (IOException e) {
			LogUtils.log("Failed to install service " + serviceName);
			LogUtils.log("Caught exception: " , e);
			return false;
		}
		catch (Throwable t) {
			LogUtils.log("Failed to install service " + serviceName);
			LogUtils.log("Caught throwable: " , t);
			return false;
		}
	}
	
	public boolean uninstallService(String serviceName, boolean wait) throws IOException, InterruptedException {
		String command = "connect localhost:8100;uninstall-service --verbose -timeout 25 " + serviceName;
		try {
		if (wait) {
			LogUtils.log("Waiting for uninstall-service to finish...");
			String output = CommandTestUtils.runCommandAndWait(command);
			boolean success = output.contains("Successfully undeployed");
			if (success) {
				LogUtils.log("Cli returned that service was un-installed succesfully");
				return true;
			}
			else {
				LogUtils.log("Cli returned that service was not un-installed succesfully");
				return false;
			}
		}
		else {
			CommandTestUtils.runCommand(command);
			return true;
		}
		}
		catch (IOException e) {
			LogUtils.log("Failed to uninstall service " + serviceName);
			LogUtils.log("Caught exception: " , e);
			return false;
		}
		catch (Throwable t) {
			LogUtils.log("Failed to uninstall service " + serviceName);
			LogUtils.log("Caught throwable: " , t);
			return false;
		}
	}
}
