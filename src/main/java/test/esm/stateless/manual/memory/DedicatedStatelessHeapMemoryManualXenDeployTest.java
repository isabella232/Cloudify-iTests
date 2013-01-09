package test.gsm.stateless.manual.memory.xen;

import java.io.File;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.vm.VirtualMachineDetails;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import framework.utils.DeploymentUtils;

import test.gsm.AbstractXenGSMTest;

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

		final ProcessingUnit pu = super
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
		repetitiveAssertNumberOfGSCsAdded(2, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSCsAdded(2, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);

		VirtualMachineDetails jvmDetails = admin.getGridServiceContainers()
				.getContainers()[0].getVirtualMachine().getDetails();
		long xmx = MemoryUnit.MEGABYTES.convert(
				jvmDetails.getMemoryHeapMaxInBytes(), MemoryUnit.BYTES);

		long xms = MemoryUnit.MEGABYTES.convert(
				jvmDetails.getMemoryHeapInitInBytes(), MemoryUnit.BYTES);
		assertEquals("Xmx", 1 , (int)Math.round((1.0*XMX)/xmx));
		assertEquals("Xms", 1 , (int)Math.round((1.0*XMS)/xms));
		
		assertUndeployAndWait(pu);
	}
}
