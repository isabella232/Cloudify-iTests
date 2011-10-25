package test.cli.cloudify;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.AbstractTest;

public class InstallAndUninstallApplicationTest extends AbstractCommandTest {

	@Override
	@BeforeMethod
	public void beforeTest() {
		// since we are using the ESM, do not start a GSC
		super.beforeTest();
		//this.restUrl = "localhost:8080";
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testInstallAndUninstall() throws IOException, InterruptedException {
		doTest("simple");
	}

	private void doTest(final String serviceGroovyFilename) throws IOException, InterruptedException {

		final String applicationDir = CommandTestUtils.getPath("apps/USM/usm/applications/" + serviceGroovyFilename);
		

		runCommand("connect " + this.restUrl + ";install-application --verbose " + applicationDir);
		final ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(serviceGroovyFilename, 10, TimeUnit.SECONDS);
		if (processingUnit == null) {
			AbstractTest.AssertFail("Processing unit '" + serviceGroovyFilename + "' was not found");
		}
		AbstractTest.assertTrue("Instance of '" + serviceGroovyFilename + "' service was not found",
		processingUnit.waitFor(1, 10, TimeUnit.SECONDS));

	    final GridServiceContainer gsc = processingUnit.getInstances()[0].getGridServiceContainer();

		runCommand("connect " + this.restUrl + ";uninstall-application -timeout 5 " + serviceGroovyFilename);

		assertGSCIsNotDiscovered(gsc);
	}

	private static void assertGSCIsNotDiscovered(final GridServiceContainer gsc) {
	    repetitiveAssertTrue("Failed waiting for GSC not to be discovered", new RepetitiveConditionProvider() {
            @Override
			public boolean getCondition() {
                return !gsc.isDiscovered();
            }
        }, OPERATION_TIMEOUT);
	}
}
