package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import org.cloudifysource.esc.driver.provisioning.byon.ByonProvisioningDriver;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.util.CloudTestUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * 
 * @author barakme
 * 
 */
public class ProvisioningContextByonTest extends AbstractByonCloudTest {

	@Override
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}

	@Override
	public void beforeBootstrap() throws Exception {

		final String newCloudDriverClazz = "CustomCloudDriver";

		getService().setNumberOfManagementMachines(2);
		CloudTestUtils.replaceGroovyDriverImplementation(
				getService(),
				ByonProvisioningDriver.class.getName(), // old class
				newCloudDriverClazz, // new class
				new File("src/main/resources/custom-cloud-configs/byon/provisioning-context")); // version
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void installTomcatTest() throws Exception {
		try {
			installServiceAndWait("tomcat", "tomcat");
		} finally {
			uninstallServiceAndWait("tomcat");
		}

	}

	@Override
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

}
