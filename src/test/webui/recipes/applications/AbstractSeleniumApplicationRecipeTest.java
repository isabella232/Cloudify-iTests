package test.webui.recipes.applications;

import static framework.utils.LogUtils.log;

import java.io.IOException;

import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import test.AbstractTest;
import test.webui.recipes.AbstractSeleniumRecipeTest;
import framework.utils.DumpUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.TeardownUtils;

public class AbstractSeleniumApplicationRecipeTest extends AbstractSeleniumRecipeTest {
	
	ProcessingUnit pu;
	private String currentApplication;
	public static final String MANAGEMENT = "management";
	Machine[] machines;
	
	public void setCurrentApplication(String application) {
		this.currentApplication = application;
	}
	
	@BeforeMethod
	public void bootstrapAndInstall() throws IOException, InterruptedException {
		LogUtils.log("LOOKUPGROUPS = " + System.getenv("LOOKUPGROUPS"));
		admin = newAdmin();
		machines = admin.getMachines().getMachines();
		admin.close();
		admin = null;
		if (bootstrapLocalCloud() && installApplication(currentApplication)) {
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
		else {
			tearDownLocalCloud();
			AbstractTest.AssertFail("Failed to install application");
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
		        TeardownUtils.teardownAll(admin);
		    } catch (Throwable t) {
		        log("failed to teardown", t);
		    }
			admin.close();
			admin = null;
		}
		tearDownLocalCloud();
		stopWebBrowser();
	}
}
