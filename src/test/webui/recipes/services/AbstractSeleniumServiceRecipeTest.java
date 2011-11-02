package test.webui.recipes.services;

import static framework.utils.LogUtils.log;

import java.io.IOException;

import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import framework.utils.DumpUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.ScriptUtils;
import framework.utils.TeardownUtils;

import test.AbstractTest;
import test.cli.cloudify.CommandTestUtils;
import test.webui.AbstractSeleniumTest;

public class AbstractSeleniumServiceRecipeTest extends AbstractSeleniumTest {
	
	ProcessingUnit pu;
	private String currentRecipe;
	public static final String MANAGEMENT = "management";
	Machine[] machines;
	
	public void setCurrentRecipe(String recipe) {
		this.currentRecipe = recipe;
	}
	
	@Override
	@BeforeMethod
	public void beforeTest() {
		admin = newAdmin();
		machines = admin.getMachines().getMachines();
		admin.close();
		String gigaDir = ScriptUtils.getBuildPath();	
		String pathToService = gigaDir + "/recipes/" + currentRecipe;	
		boolean success = false;
		
		try {
			String command = "bootstrap-localcloud --verbose;install-service --verbose -timeout 25 " + pathToService + ";exit";
			String output = CommandTestUtils.runCommandAndWait(command);
			if (isServiceInstalled(currentRecipe, output)) {
				success = true;
				AdminFactory factory = new AdminFactory();
				for (Machine machine : machines){
					LogUtils.log("adding " + machine.getHostName() + ":4168 to admin locators" );
					factory.addLocator(machine.getHostAddress() + ":4168");
				}
				LogUtils.log("creating new admin");
				admin = factory.createAdmin();
				
				LogUtils.log("retrieving webui url");
				ProcessingUnit webui = admin.getProcessingUnits().waitFor("webui");
				assertTrue(webui != null);
				assertTrue(webui.getInstances().length != 0);	
				String url = ProcessingUnitUtils.getWebProcessingUnitURL(webui).toString();	
				startWebBrowser(url); 
			}
		} catch (IOException e) {
			LogUtils.log("bootstrap-cloud failed.", e);
		} catch (InterruptedException e) {
			LogUtils.log("bootstrap-cloud failed.", e);
		}
		finally {
			if (!success) {
				admin = newAdmin();
				afterTest();
				AbstractTest.AssertFail("Application wasnt installed");
			}
		}
	}
	
	@Override
	@AfterMethod
	public void afterTest() {
		
		String command = "teardown-localcloud;" + "exit;";
		if (admin != null) {
			try {
		        DumpUtils.dumpLogs(admin);
		        CommandTestUtils.runCommandAndWait(command);
		        stopWebBrowser();
		    } catch (Throwable t) {
		        log("failed to dump logs", t);
		    }
		    try {
		        TeardownUtils.teardownAll(admin);
		    } catch (Throwable t) {
		        log("failed to teardown", t);
		    }
			admin.close();
			admin = null;
		}
	}
	
	private boolean isServiceInstalled(String serviceName, String cliOutPut) {
		return cliOutPut.contains("Service" + serviceName + "successfully installed");
	}
}
