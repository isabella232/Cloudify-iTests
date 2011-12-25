package test.webui.recipes.services;

import static framework.utils.LogUtils.log;

import java.io.IOException;

import org.openspaces.admin.AdminFactory;
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
		if (installService(currentRecipe, wait)) {
			LogUtils.log("retrieving webui url");
			ProcessingUnit webui = admin.getProcessingUnits().waitFor("webui");
			ProcessingUnitUtils.waitForDeploymentStatus(webui, DeploymentStatus.INTACT);
			assertTrue(webui != null);
			assertTrue(webui.getInstances().length != 0);	
			String url = ProcessingUnitUtils.getWebProcessingUnitURL(webui).toString();	
			startWebBrowser(url); 
		}
	}
	
	@AfterMethod
	public void tearDown() throws IOException, InterruptedException {
		if (admin != null) {
			try {
		        DumpUtils.dumpLogs(admin);
		    } catch (Throwable t) {
		        log("failed to dump logs", t);
		    }
		    try {
		    	uninstallService(currentRecipe, wait);
		    } catch (Throwable t) {
		        log("failed to teardown", t);
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
		if (wait) {
			String output = CommandTestUtils.runCommandAndWait(command);
			return output.contains("successfully installed");
		}
		else {
			CommandTestUtils.runCommand(command);
			return true;
		}
	}
	
	public boolean uninstallService(String serviceName, boolean wait) throws IOException, InterruptedException {
		String gigaDir = ScriptUtils.getBuildPath();	
		String pathToService = gigaDir + "/recipes/" + serviceName;	
		String command = "connect localhost:8100;uninstall-service --verbose -timeout 25 " + pathToService;
		if (wait) {
			String output = CommandTestUtils.runCommandAndWait(command);
			return output.contains("Successfully undeployed " + serviceName);
		}
		else {
			CommandTestUtils.runCommand(command);
			return true;
		}
	}
}
