package test.webui.recipes.applications;

import static framework.utils.LogUtils.log;

import java.io.IOException;

import org.openspaces.admin.AdminFactory;
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

public class AbstractSeleniumApplicationRecipeTest extends AbstractSeleniumTest {
	
	ProcessingUnit pu;
	private String currentApplication;
	public static final String MANAGEMENT = "management";
	
	public void setCurrentRecipe(String recipe) {
		this.currentApplication = recipe;
	}
	
	@Override
	@BeforeMethod(alwaysRun = true)
	public void beforeTest() {
		
		String gigaDir = ScriptUtils.getBuildPath();
		
		String pathToService = gigaDir + "/examples/" + currentApplication;
		
		boolean success = false;
		
		try {
			String command = "bootstrap-localcloud --verbose;install-application --verbose -timeout 10 " + pathToService + ";exit";
			CommandTestUtils.runCommandAndWait(command);

			success = true;
			AdminFactory factory = new AdminFactory();
			factory.addLocator("localhost:4168");
			
			admin = factory.createAdmin();
			
			ProcessingUnit webui = admin.getProcessingUnits().waitFor("webui");
			
			String url = ProcessingUnitUtils.getWebProcessingUnitURL(webui).toString();
			
			startWebBrowser(url); 
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
