package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.publicprovisioning;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;

/**
 * CLOUDIFY-1353 
 * @author elip
 *
 */
public class PublicProvisioningWithManagementMachineTest extends AbstractPublicProvisioningByonCloudTest {
	
	private static final String GROOVY_ONE = "groovy-one";

	@Override
	protected String getCloudName() {
		return "byon";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	/**
	 * This test installs two large instances using the global isolationSLA.
	 * Expected Result : One instance on the management machine, and one on a new machine.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testTwoLargeInstances() throws IOException, InterruptedException {
		
		Machine managementMachine = getManagementMachines().get(0);

		int numberOfDesiredInstancesOnManagementMachine = 1;
		
		int reservedMemoryCapacityPerManagementMachineInMB = getService().getCloud().getProvider().getReservedMemoryCapacityPerManagementMachineInMB();
		int machineMemoryMB = getService().getCloud().getCloudCompute().getTemplates().get(DEFAULT_TEMPLATE_NAME).getMachineMemoryMB();
		
		int memoryForServicesOnManagementMachine = machineMemoryMB - reservedMemoryCapacityPerManagementMachineInMB;
		int memoryPerServiceInstanceOnManagementMachine = memoryForServicesOnManagementMachine / numberOfDesiredInstancesOnManagementMachine;
				
		// this should install one instance on the management(since there is room left)
		// and another instance on its own machine since the management is maxed out.
		installManualPublicProvisioningServiceAndWait(GROOVY_ONE, 2, (int)Math.ceil(0.8 * memoryPerServiceInstanceOnManagementMachine) , 0, DEFAULT_TEMPLATE_NAME, true);
		
		ProcessingUnit groovy1 = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("default", GROOVY_ONE), AbstractTestSupport.OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		AssertUtils.assertNotNull("Failed to discover processing unit " + GROOVY_ONE + " even though it was installed succesfully", groovy1);
		AbstractTestSupport.assertTrue(groovy1.waitFor(2, AbstractTestSupport.OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));

		// collect all machines of groovy 1
		Set<Machine> groovy1Machines = new HashSet<Machine>();
		ProcessingUnitInstance[] instances = groovy1.getInstances();
		for (ProcessingUnitInstance instance : instances) {
			groovy1Machines.add(instance.getMachine());
		}
		
		// subtract the management machines
		AssertUtils.assertTrue("No instance of Processing Unit " + groovy1.getName() + " were installed on the management machine, even though it could have accomodated one instance", groovy1Machines.remove(managementMachine));
		AssertUtils.assertTrue("Both instances of " + groovy1.getName() + " were installed on the management machien even though it could accomodate only one instance", !groovy1Machines.isEmpty());
		
		uninstallServiceAndWait(GROOVY_ONE);		
		super.scanForLeakedAgentNodes();
	}
	
	/**
	 * This test installs two small instances using the global isolationSLA.
	 * Expected Result : Both instances should be installed on the management machine.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testTwoSmallInstances() throws IOException, InterruptedException {
		
		Machine managementMachine = getManagementMachines().get(0);
				
		// this should install one instance on the management(since there is room left)
		// and another instance on its own machine since the management is maxed out.
		installManualPublicProvisioningServiceAndWait(GROOVY_ONE, 2, 128 , 0, DEFAULT_TEMPLATE_NAME, true);
		
		ProcessingUnit groovy1 = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("default", GROOVY_ONE), AbstractTestSupport.OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		AssertUtils.assertNotNull("Failed to discover processing unit " + GROOVY_ONE + " even though it was installed succesfully", groovy1);
		AbstractTestSupport.assertTrue(groovy1.waitFor(2, AbstractTestSupport.OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));

		// collect all machines of groovy 1
		Set<Machine> groovy1Machines = new HashSet<Machine>();
		ProcessingUnitInstance[] instances = groovy1.getInstances();
		for (ProcessingUnitInstance instance : instances) {
			groovy1Machines.add(instance.getMachine());
		}
		
		// subtract the management machines
		AssertUtils.assertTrue("No instance of Processing Unit " + groovy1.getName() + " were installed on the management machine, even though it could have accomodated one instance", groovy1Machines.remove(managementMachine));
		AssertUtils.assertTrue("Not both instances of " + groovy1.getName() + " were installed on the management machien even though it could accomodate both on them", groovy1Machines.isEmpty());
		
		uninstallServiceAndWait(GROOVY_ONE);		
		super.scanForLeakedAgentNodes();
	}
		
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}	
}
