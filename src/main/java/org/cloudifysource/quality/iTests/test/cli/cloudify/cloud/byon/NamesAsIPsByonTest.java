package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.util.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import iTests.framework.utils.NetworkUtils;
import iTests.framework.utils.ScriptUtils;


/**
 * This test installs petclinic-simple on BYON after changing the byon-cloud.groovy file to contain machine names instead of IPs.
 * <p>It checks whether the bootstrap and install have succeeded.
 * <p>Note: this test uses 3 fixed machines - 192.168.9.115, 192.168.9.116, 192.168.9.120.
 */
public class NamesAsIPsByonTest extends AbstractByonCloudTest {

	private static int REQUIRED_NUMBER_OF_MACHINES = 3;

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testPetclinic() throws IOException, InterruptedException{

		installApplicationAndWait(ScriptUtils.getBuildPath() + "/recipes/apps/petclinic-simple", "petclinic");
		assertTrue("petclinic.mongod is not up - install failed", admin.getProcessingUnits().waitFor("petclinic.mongod", OPERATION_TIMEOUT, TimeUnit.MILLISECONDS) != null);
		assertTrue("petclinic.tomcat is not up - install failed", admin.getProcessingUnits().waitFor("petclinic.tomcat", OPERATION_TIMEOUT, TimeUnit.MILLISECONDS) != null);
		uninstallApplicationAndWait("petclinic");
		
		super.scanForLeakedAgentNodes();

	}	

	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

	@Override
	protected void customizeCloud() throws Exception {
		
		String[] ips = getService().getIpList().split(",");
		
		if (ips.length < REQUIRED_NUMBER_OF_MACHINES) {
			throw new IllegalStateException("This test requires a minimum 3 thress machines in the machines pool");
		} 
		
		String[] hostNames = NetworkUtils.resolveIpsToHostNames((String[])ArrayUtils.subarray(ips, 0, 3));
		
		super.customizeCloud();
		getService().setMachines(hostNames);
		getService().setIpList(StringUtils.arrayToCommaDelimitedString(hostNames));
	}
}