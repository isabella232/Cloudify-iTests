package test.webui.recipes.services;

import java.io.IOException;

import org.testng.annotations.BeforeMethod;

import test.cli.cloudify.CommandTestUtils;
import test.webui.AbstractWebUILocalCloudTest;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class AbstractSeleniumServiceRecipeTest extends AbstractWebUILocalCloudTest {
	
	protected boolean wait = true;
	protected String pathToService;
	protected String serviceName;

	public void setCurrentRecipe(String recipe) {
		this.serviceName = recipe;
		String gigaDir = ScriptUtils.getBuildPath();	
		this.pathToService = gigaDir + "/recipes/services/" + recipe;
	}
	
	public void setPathToServiceRelativeToSGTestRootDir(String recipe, String relativePath) {
		this.pathToService = CommandTestUtils.getPath(relativePath);
	}

	public void setWait(boolean wait) {
		this.wait = wait;
	}


	@BeforeMethod(alwaysRun = true)
	public void install() throws IOException, InterruptedException {	
		LogUtils.log("Installing service " + serviceName);
		assertNotNull(pathToService);
		installService(pathToService, wait); 
		startBrowser();
	}


	public void installService(String pathToService, boolean wait) throws IOException, InterruptedException {
		String command = "connect localhost:8100;install-service --verbose -timeout 25 " + pathToService;
		if (wait) {
			LogUtils.log("Waiting for install-service to finish...");
			CommandTestUtils.runCommandAndWait(command);
		}
		else {
			LogUtils.log("Not waiting for service to finish, assuming it will succeed");
			CommandTestUtils.runCommand(command);
		}
	}

	public void uninstallService(String serviceName, boolean wait) throws IOException, InterruptedException  {
		String command = "connect localhost:8100;uninstall-service --verbose -timeout 25 " + serviceName;
		if (wait) {
			LogUtils.log("Waiting for uninstall-service to finish...");
			CommandTestUtils.runCommandAndWait(command);
		}
		else {
			LogUtils.log("Not waiting for uninstall-service to finish, assuming it will succeed");
			CommandTestUtils.runCommand(command);
		}
	}
}
