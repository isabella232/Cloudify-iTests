package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.dynamicByon;

import java.util.List;

import junit.framework.Assert;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.AbstractByonCloudTest;
import org.openspaces.admin.machine.Machine;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DynamicByonBootstrapWithTwoMngMachinesTest extends AbstractByonCloudTest {
	private static String MANAGEMENT_MACHINES;
	
	@Override
	protected String getCloudName() {
		return "dynamic-byon";
	}

	@BeforeClass(alwaysRun = true)
	@Override
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}

	@AfterClass(alwaysRun = true)
	@Override
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	@Override
	protected void customizeCloud() throws Exception {
		String[] managementMachines = getService().getIpList().split(",");
		MANAGEMENT_MACHINES = managementMachines[0] + ","  + managementMachines[1];
		super.customizeCloud();
		getService().setNumberOfManagementMachines(2);
		getService().getProperties().put("password", "tgrid");
		getService().getProperties().put("password", "tgrid");
		getService().getProperties().put("startMachineIP", "pc-lab111");
		getService().getProperties().put("managementMachines", MANAGEMENT_MACHINES);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void installSimpleApplicationTest() {
		List<Machine> managementMachines = getManagementMachines();
		Assert.assertEquals(MANAGEMENT_MACHINES.split(",").length, managementMachines.size());
		for (Machine mngMachine : managementMachines) {	
			Assert.assertTrue(MANAGEMENT_MACHINES.contains(mngMachine.getHostName()));
		}
	}
}
