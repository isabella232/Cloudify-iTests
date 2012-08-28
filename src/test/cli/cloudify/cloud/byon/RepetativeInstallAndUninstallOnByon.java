package test.cli.cloudify.cloud.byon;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.NewAbstractCloudTest;
import test.cli.cloudify.cloud.services.byon.ByonCloudService;
import framework.tools.SGTestHelper;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

/**
 * <p>
 * 1. bootstrap byon.
 * <p>
 * 2. repeat steps 3-4 three times.
 * <p>
 * 3. install petclinic-simple and assert installation.
 * <p>
 * 4. uninstall petclinic-simple and assert successful uninstall.
 * 
 */
public class RepetativeInstallAndUninstallOnByon extends NewAbstractCloudTest {

	private final static int REPETITIONS = 3;
	private static final String TEST_UNIQUE_NAME = "RepetativeInstallAndUninstallOnByon";

	private Admin admin;
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}
	
	@AfterMethod
	public void cleanUp() {
		super.scanAgentNodesLeak();
	}

	@Override
	protected void customizeCloud() throws Exception {
		// replace the default bootstrap-management.sh with a custom version one
		ByonCloudService byonService = (ByonCloudService) cloud;
		File standardBootstrapManagement = new File(byonService.getPathToCloudFolder() + "/upload",
				"bootstrap-management.sh");
		File bootstrapManagementWithMulticast = new File(SGTestHelper.getSGTestRootDir()
				+ "/apps/cloudify/cloud/byon/bootstrap-management-" + byonService.getServiceFolder() + ".sh");
		Map<File, File> filesToReplace = new HashMap<File, File>();
		filesToReplace.put(standardBootstrapManagement, bootstrapManagementWithMulticast);
		byonService.addFilesToReplace(filesToReplace);
		byonService.setMachinePrefix(this.getClass().getName());
	}

	@Override
	protected void afterBootstrap() throws Exception {
		LogUtils.log("creating admin");
		AdminFactory factory = new AdminFactory();
		factory.addGroup(TEST_UNIQUE_NAME);
		admin = factory.createAdmin();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void testPetclinic() throws Exception {

		for (int i = 0; i < REPETITIONS; i++) {
			LogUtils.log("petclinic install number " + (i + 1));
			installApplicationAndWait(ScriptUtils.getBuildPath() + "/recipes/apps/petclinic-simple", "petclinic");

			Assert.assertTrue(
					admin.getProcessingUnits().getProcessingUnit("petclinic.mongod").waitFor(1, 10, TimeUnit.MINUTES),
					"petclinic.mongod is not up - install failed");
			Assert.assertTrue(
					admin.getProcessingUnits().getProcessingUnit("petclinic.tomcat").waitFor(1, 10, TimeUnit.MINUTES),
					"petclinic.tomcat is not up - install failed");

			LogUtils.log("petclinic uninstall number " + (i + 1));
			uninstallApplicationAndWait("petclinic");
			Assert.assertTrue(admin.getProcessingUnits().getProcessingUnit("petclinic.mongod") == null,
					"petclinic.mongod is up - uninstall failed");
			Assert.assertTrue(admin.getProcessingUnits().getProcessingUnit("petclinic.tomcat") == null,
					"petclinic.tomcat is up - uninstall failed");
		}
	}

	@Override
	protected void beforeTeardown() throws Exception {
		// clean
		if (admin != null) {
			admin.close();
			admin = null;
		}
	}

	@Override
	protected String getCloudName() {
		return "byon";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
}
