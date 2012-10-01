package test.cli.cloudify.cloud.byon;

import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
public class RepetativeInstallAndUninstallOnByon extends AbstractByonCloudTest {

	private final static int REPETITIONS = 3;
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testPetclinicSimple() throws Exception {

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
	
	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}

}
