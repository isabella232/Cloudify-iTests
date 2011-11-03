package test.webui.recipes.services;

import static framework.utils.LogUtils.log;

import java.io.IOException;

import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import test.AbstractTest;
import test.webui.recipes.AbstractSeleniumRecipeTest;
import framework.utils.DumpUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.TeardownUtils;

public class AbstractSeleniumServiceRecipeTest extends AbstractSeleniumRecipeTest {
	
	ProcessingUnit pu;
	private String currentRecipe;
	public static final String MANAGEMENT = "management";
	
	public void setCurrentRecipe(String recipe) {
		this.currentRecipe = recipe;
	}
	
	@BeforeMethod
	public void bootstrapAndInstall() throws IOException, InterruptedException {		
		AdminFactory factory = new AdminFactory();
		LogUtils.log("Adding locators to new admin factory");
		factory.addLocator("127.0.0.1:4168");
		if (bootstrapLocalCloud()) {
			LogUtils.log("creating new admin from factory");
			admin = factory.createAdmin();
			if (installService(currentRecipe)) {
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
