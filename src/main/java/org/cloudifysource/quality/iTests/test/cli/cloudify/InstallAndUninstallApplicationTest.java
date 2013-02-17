package org.cloudifysource.quality.iTests.test.cli.cloudify;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.Assert;
import org.testng.annotations.Test;


import org.cloudifysource.dsl.utils.ServiceUtils;

import org.cloudifysource.quality.iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import org.cloudifysource.quality.iTests.framework.utils.usm.USMTestUtils;

public class InstallAndUninstallApplicationTest extends AbstractLocalCloudTest {

	private static final String SERVICE_NAME = "simple";

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testInstallAndUninstall() throws IOException, InterruptedException {
		doTest(SERVICE_NAME);
	}

	private void doTest(final String serviceGroovyFilename) throws IOException, InterruptedException {

		final String applicationDir = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/" + serviceGroovyFilename);

		runCommand("connect " + this.restUrl + ";install-application --verbose " + applicationDir);
		String absolutePUName = ServiceUtils.getAbsolutePUName("simple", "simple");
		final ProcessingUnit processingUnit = assertProcessingUnitDeployed(absolutePUName);

	    final GridServiceContainer gsc = processingUnit.getInstances()[0].getGridServiceContainer();

		runCommand("connect " + this.restUrl + ";uninstall-application -timeout 5 " + serviceGroovyFilename);

		assertGSCIsNotDiscovered(gsc);
	}

	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	private void testInstallAndUninstallWithNameOption() throws IOException, InterruptedException {

		final String applicationName = "simpleApp";
		final String applicationDir = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/simple");

		runCommand("connect " + this.restUrl + ";install-application --verbose -name " + applicationName + " " + applicationDir);
		String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, "simple");
		final ProcessingUnit processingUnit = assertProcessingUnitDeployed(absolutePUName);

	    final GridServiceContainer gsc = processingUnit.getInstances()[0].getGridServiceContainer();

		runCommand("connect " + this.restUrl + ";uninstall-application -timeout 5 " + applicationName);

		assertGSCIsNotDiscovered(gsc);
	}
	
	private ProcessingUnit assertProcessingUnitDeployed(String absolutePUName) {
		final ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(absolutePUName, 30, TimeUnit.SECONDS);
		if (processingUnit == null) {
			Assert.fail("Processing unit '" + absolutePUName + "' was not found");
		}
		Assert.assertTrue(processingUnit.waitFor(1, 30, TimeUnit.SECONDS), "Instance of '" + absolutePUName + "' service was not found");
		AbstractTestSupport.assertTrue(USMTestUtils.waitForPuRunningState(absolutePUName, 60, TimeUnit.SECONDS, admin));
		return processingUnit;
	}

	private static void assertGSCIsNotDiscovered(final GridServiceContainer gsc) {
	    AbstractTestSupport.repetitiveAssertTrue("Failed waiting for GSC not to be discovered", new RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
                return !gsc.isDiscovered();
            }
        }, AbstractTestSupport.OPERATION_TIMEOUT);
	}
}
