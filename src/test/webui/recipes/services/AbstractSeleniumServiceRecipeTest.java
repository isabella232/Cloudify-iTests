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

import test.cli.cloudify.CommandTestUtils;
import test.webui.AbstractSeleniumTest;

public class AbstractSeleniumServiceRecipeTest extends AbstractSeleniumTest {
	
	ProcessingUnit pu;
	private String currentRecipe;
	public static final String MANAGEMENT = "management";
	
	public void setCurrentRecipe(String recipe) {
		this.currentRecipe = recipe;
	}
	
	@Override
	@BeforeMethod(alwaysRun = true)
	public void beforeTest() {
		
		admin = newAdmin();
		
		String gigaDir = ScriptUtils.getBuildPath();
		
		String pathToService = gigaDir + "/recipes/" + currentRecipe;
		
		boolean success = false;
		
		try {
			String command = "bootstrap-localcloud --verbose;install-service --verbose -timeout 10 " + pathToService + ";exit";
			String output = CommandTestUtils.runCommandAndWait(command);
			if (output.contains("installed successfully")) {
				success = true;
				AdminFactory factory = new AdminFactory();
				for (Machine machine : admin.getMachines().getMachines()){
					factory.addLocator(machine.getHostAddress() + ":4168");
				}
				
				admin = factory.createAdmin();
				
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
				afterTest();
			}
		}
	}
	
	@Override
	@AfterMethod(alwaysRun = true)
	public void afterTest() {
		
		String command = "teardown-localcloud;" + "exit;";
		try {
	    	if (admin != null) {
		    	try {
		            DumpUtils.dumpLogs(admin);
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
			CommandTestUtils.runCommandAndWait(command);
			stopWebBrowser();
		} catch (IOException e) {
			LogUtils.log("teardown-cloud failed.", e);
		} catch (InterruptedException e) {
			LogUtils.log("teardown-cloud failed.", e);
		}
	}

}
