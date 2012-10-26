package test.cli.cloudify.cloud.byon.publicprovisioning;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.cloud.byon.AbstractByonCloudTest;
import framework.tools.SGTestHelper;
import framework.utils.IOUtils;

public class AbstractPublicProvisioningByonCloudTest extends AbstractByonCloudTest {
	
	protected static final String DEFAULT_TEMPLATE_NAME = "SMALL_LINUX";
	
	private static final String PUBLIC_MANUAL_PROVISIONING_ORIGINAL_GROOVY_PATH = CommandTestUtils.getPath("/apps/USM/usm/groovy-public-provisioning");
	private static final String PUBLIC_AUTOMATIC_PROVISIONING_ORIGINAL_GROOVY_PATH = CommandTestUtils.getPath("/apps/USM/usm/customServiceMonitor-public-provisioning");
		
	private static final String BUILD_SERVICES_FOLDER = SGTestHelper.getBuildDir() + "/recipes/services";
	
	protected void installManualPublicProvisioningServiceAndWait(final String serviceName,
			final int numInstances,
			final int instanceMemoryInMB,
			final double instanceCpuCores, String templateName) throws IOException, InterruptedException {
		
		try {			
			generateServiceToBuildServicesFolder(
					serviceName,
					PUBLIC_MANUAL_PROVISIONING_ORIGINAL_GROOVY_PATH,
					"groovy",
					numInstances, 
					instanceMemoryInMB, 
					instanceCpuCores, 
					true, 
					templateName);
			super.installServiceAndWait(BUILD_SERVICES_FOLDER + "/" + serviceName, serviceName);
		} finally {			
			File serviceDir = new File(BUILD_SERVICES_FOLDER + "/" + serviceName);
			if (serviceDir.exists()) {
				FileUtils.deleteDirectory(serviceDir);
			}
		}
	}
	
	protected void installAutomaticManualPublicProvisioningServiceAndWait(final String serviceName,
			final int numInstances,
			final int instanceMemoryInMB,
			final double instanceCpuCores, String templateName) throws IOException, InterruptedException {
		
		try {
			generateServiceToBuildServicesFolder(
					serviceName,
					PUBLIC_AUTOMATIC_PROVISIONING_ORIGINAL_GROOVY_PATH,
					"customServiceMonitor-public-provisioning",
					numInstances, 
					instanceMemoryInMB, 
					instanceCpuCores, 
					true, 
					templateName);
			super.installServiceAndWait(BUILD_SERVICES_FOLDER + "/" + serviceName, serviceName);			
		} finally {
			File serviceDir = new File(BUILD_SERVICES_FOLDER + "/" + serviceName);
			if (serviceDir.exists()) {
				FileUtils.deleteDirectory(serviceDir);
			}
		}
	}

	private void generateServiceToBuildServicesFolder(final String serviceName,
			final String originalServicePath,
			final String originalServiceName,
			final int numInstances,
			final int instanceMemoryInMB,
			final double instanceCpuCores,
			final boolean publicProvisioning, 
			final String templateName) throws IOException {

		Map<String, String> props = new HashMap<String,String>();
		
		props.put("instanceMemoryMB 128", "instanceMemoryMB " + instanceMemoryInMB);
		props.put("instanceCpuCores 0", "instanceCpuCores " + instanceCpuCores);

		FileUtils.copyDirectory(new File(originalServicePath), new File(BUILD_SERVICES_FOLDER + "/" + serviceName));
		File dslFile = new File(BUILD_SERVICES_FOLDER + "/" + serviceName + "/" + originalServiceName + "-service.groovy");
		File serviceFile = new File(BUILD_SERVICES_FOLDER + "/" + serviceName + "/" + serviceName + "-service.groovy");
		if (!serviceFile.exists()) {
			FileUtils.moveFile(dslFile, serviceFile);
			dslFile.delete();
		}
		
		props.put("name \"groovy\"", "name " + '"' + serviceName + '"');
		props.put("numInstances 2", "numInstances " + numInstances);
		props.put("SMALL_LINUX", templateName);
		IOUtils.replaceTextInFile(serviceFile, props);
		
	}
}
