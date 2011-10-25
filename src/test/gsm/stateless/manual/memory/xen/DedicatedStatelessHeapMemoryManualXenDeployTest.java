package test.gsm.stateless.manual.memory.xen;

import java.io.File;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.vm.VirtualMachineDetails;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.utils.DeploymentUtils;

/**
 * Allocate more memory than java heap size used by container.
 * This feature is used by PU instances that spawn a new process or use non-java-heap in process.
 * 
 * @author itaif
 */
public class DedicatedStatelessHeapMemoryManualXenDeployTest extends
		AbstractXenGSMTest {

	private static final int XMS = 128;
	private static final int XMX = 256;

	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void test() {
		File archive = DeploymentUtils.getArchive("servlet.war");

		final ProcessingUnit pu = gsm
				.deploy(new ElasticStatelessProcessingUnitDeployment(archive)
						.memoryCapacityPerContainer(1, MemoryUnit.GIGABYTES)
						.addCommandLineArgument("-Xmx" + XMX +"m")
						.addCommandLineArgument("-Xms" + XMS+"m")
						.dedicatedMachineProvisioning(
								getMachineProvisioningConfig())
						.scale(new ManualCapacityScaleConfigurer()
								.memoryCapacity(2, MemoryUnit.GIGABYTES)
								.create()));

		pu.waitFor(2);

		// According to the XMX we should start 8 containers to reach 2GB
		// According to memoryCapacityPerContainer we should start only 2 containers to reach 2GB
		assertEquals("Number of GSCs added", 2, getNumberOfGSCsAdded());
		assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
		assertEquals("Number of GSAs added", 2, getNumberOfGSCsAdded());
		assertEquals("Number of GSAs removed", 0, getNumberOfGSCsRemoved());

		VirtualMachineDetails jvmDetails = admin.getGridServiceContainers()
				.getContainers()[0].getVirtualMachine().getDetails();
		long xmx = MemoryUnit.MEGABYTES.convert(
				jvmDetails.getMemoryHeapMaxInBytes(), MemoryUnit.BYTES);

		long xms = MemoryUnit.MEGABYTES.convert(
				jvmDetails.getMemoryHeapInitInBytes(), MemoryUnit.BYTES);
		assertEquals("Xmx", 1 , (int)Math.round((1.0*XMX)/xmx));
		assertEquals("Xms", 1 , (int)Math.round((1.0*XMS)/xms));
	}
}
