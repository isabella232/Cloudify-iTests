package test.cli.cloudify;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.cloudify.dsl.utils.ServiceUtils;

import test.AbstractTest;
import framework.utils.LogUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

public class InstallAndUninstallServiceTest extends AbstractTest {

	public static final String SERVLET_WAR_NAME = "servlet.war";
	public static final String SERVLET_SERVICE_NAME = "servlet";
	
	public static final String SERVLET_FOLDER_NAME = "statelessPU";
	public static final String SERVLET_RECIPE_SERVICE_NAME = "statelessPU";
	
	public static final String STATEFUL_FOLDER_NAME = "statefulPU";
	public static final String STATEFUL_SERVICE_NAME = "stateful";
	
	public static final String DATAGRID_FOLDER_NAME = "datagrid";
	public static final String DATAGRID_SERVICE_NAME = "datagrid";
	
	public static final String USM_SERVICE_FOLDER_NAME = "simple";
	public static final String USM_SERVICE_NAME = "simple";
	
	public static final String MIRROR_SERVICE_FOLDER_NAME = "stockAnalyticsMirror";
	public static final String MIRROR_SERVICE_NAME = "stockAnalyticsMirror";
	private Machine[] machines;
	
	@Override
	@BeforeMethod
	public void beforeTest() {
		super.beforeTest();
		machines = admin.getMachines().getMachines();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
	public void testServletInstall() throws IOException, InterruptedException {
		String absolutePUName = ServiceUtils.getAbsolutePUName("default", SERVLET_SERVICE_NAME);
		testRestApiInstall(absolutePUName, getArchiveServicePath(SERVLET_WAR_NAME));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
	public void testUSMInstall() throws IOException, InterruptedException {
		String absolutePUName = ServiceUtils.getAbsolutePUName("default", USM_SERVICE_NAME);
		testRestApiInstall(absolutePUName, getUsmServicePath(USM_SERVICE_FOLDER_NAME));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
	public void testServletRecipeInstall() throws IOException, InterruptedException {
		String absolutePUName = ServiceUtils.getAbsolutePUName("default", SERVLET_RECIPE_SERVICE_NAME);
		testRestApiInstall(absolutePUName, getUsmServicePath(SERVLET_FOLDER_NAME));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
	public void testStatefulRecipeInstall() throws IOException, InterruptedException {
		String absolutePUName = ServiceUtils.getAbsolutePUName("default", STATEFUL_SERVICE_NAME);
		testRestApiInstall(absolutePUName, getUsmServicePath(STATEFUL_FOLDER_NAME));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
	public void testDataGridRecipeInstall() throws IOException, InterruptedException {
		String absolutePUName = ServiceUtils.getAbsolutePUName("default", DATAGRID_SERVICE_NAME);
		testRestApiInstall(absolutePUName, getUsmServicePath(DATAGRID_FOLDER_NAME));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testMirrorRecipeInstall() throws IOException, InterruptedException {
		String absolutePUName = ServiceUtils.getAbsolutePUName("default", MIRROR_SERVICE_NAME);
		testRestApiInstall(absolutePUName, getUsmServicePath(MIRROR_SERVICE_FOLDER_NAME));
	}
	
	void testRestApiInstall(String serviceName, String servicePath) throws IOException, InterruptedException{
		
		CommandTestUtils.runCommandAndWait("bootstrap-localcloud " +
				";install-service --verbose " + servicePath + 
				";");
		
		admin = getAdminWithLocators();
		
		ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(serviceName, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS);
        assertTrue("Instance of '" + serviceName + "' service was not found", 
        		processingUnit != null && 
        		processingUnit.waitFor(1, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS));

		
	    final GridServiceContainer gsc = processingUnit.getInstances()[0].getGridServiceContainer();
	    
	    CommandTestUtils.runCommandAndWait("teardown-localcloud");
		
		assertGSCIsNotDiscovered(gsc);
	}
	
	private Admin getAdminWithLocators() {
		AdminFactory factory = new AdminFactory();
		for (Machine machine : machines) {
			LogUtils.log("adding locator to admin : " + machine.getHostName() + ":4168");
			factory.addLocator(machine.getHostAddress() + ":4168");
		}
		LogUtils.log("adding localhost locator to admin");
		factory.addLocator("127.0.0.1:4168");
		return factory.createAdmin();
	}

	private static void assertGSCIsNotDiscovered(final GridServiceContainer gsc) {
	    repetitiveAssertTrue("Failed waiting for GSC not to be discovered", new RepetitiveConditionProvider() {
            public boolean getCondition() {
                return !gsc.isDiscovered();
            }
        }, OPERATION_TIMEOUT);
	}
	
	private String getArchiveServicePath(String dirOrFilename) {
		return CommandTestUtils.getPath("apps/archives/" + dirOrFilename);
	}
	
	private String getUsmServicePath(String dirOrFilename) {
		return CommandTestUtils.getPath("apps/USM/usm/" + dirOrFilename);
	}
}
