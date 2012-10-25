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
	
	private static final String DEFAULT_TEMPLATE_NAME = "SMALL_LINUX";
	
	private static final String PUBLIC_PROVISIONING_ORIGINAL_GROOVY_PATH = CommandTestUtils.getPath("/apps/USM/usm/groovy-public-provisioning");
	
	private static final String DEDICATED_PROVISIONING_ORIGINAL_GROOVY_PATH = CommandTestUtils.getPath("/apps/USM/usm/groovy");
	
	private static final String BUILD_SERVICES_FOLDER = SGTestHelper.getBuildDir() + "/recipes/services";
	
	protected void installPublicProvisioningServiceAndWait(final String serviceName,
			final int numInstances,
			final int instanceMemoryInMB,
			final double instanceCpuCores, String templateName) throws IOException, InterruptedException {
		generateGroovyService(serviceName, numInstances, instanceMemoryInMB, instanceCpuCores, true, templateName);
		super.installServiceAndWait(BUILD_SERVICES_FOLDER + "/" + serviceName, serviceName);
		FileUtils.deleteDirectory(new File(BUILD_SERVICES_FOLDER + "/" + serviceName));
	}
	
	protected void installDedicatedProvisioningServiceAndWait(final String serviceName,
			final int numInstances, String templateName) throws IOException, InterruptedException {
		generateGroovyService(serviceName, numInstances, 0, 0, false, templateName);
		super.installServiceAndWait(BUILD_SERVICES_FOLDER + "/" + serviceName, serviceName);
		FileUtils.deleteDirectory(new File(BUILD_SERVICES_FOLDER + "/" + serviceName));
	}
	
	protected void installPublicProvisioningServiceAndWait(final String serviceName,
			final int numInstances,
			final int instanceMemoryInMB,
			final double instanceCpuCores) throws IOException, InterruptedException {
		
		try {
			generateGroovyService(serviceName, numInstances, instanceMemoryInMB, instanceCpuCores, true, DEFAULT_TEMPLATE_NAME);
			super.installServiceAndWait(BUILD_SERVICES_FOLDER + "/" + serviceName, serviceName);
		} finally {
			FileUtils.deleteDirectory(new File(BUILD_SERVICES_FOLDER + "/" + serviceName));			
		}
	}
	
	protected void installDedicatedProvisioningServiceAndWait(final String serviceName,
			final int numInstances) throws IOException, InterruptedException {
		
		try {
			generateGroovyService(serviceName, numInstances, 0, 0, false, DEFAULT_TEMPLATE_NAME);
			super.installServiceAndWait(BUILD_SERVICES_FOLDER + "/" + serviceName, serviceName);
		} finally {
			FileUtils.deleteDirectory(new File(BUILD_SERVICES_FOLDER + "/" + serviceName));						
		}
		
	}
	
	protected void uninstallDedicatedProvisioningServiceAndWait() throws IOException, InterruptedException {
		super.uninstallServiceAndWait("groovy");
	}
	
	protected String getDedicatedProvisioningServiceName() {
		return "groovy";
	}
	
	private void generateGroovyService(final String serviceName,
			final int numInstances,
			final int instanceMemoryInMB,
			final double instanceCpuCores,
			final boolean publicProvisioning, String templateName) throws IOException {

		Map<String, String> props = new HashMap<String,String>();
		
		String defaultServicePath = null;
		if (publicProvisioning) {
			defaultServicePath = PUBLIC_PROVISIONING_ORIGINAL_GROOVY_PATH;
			props.put("instanceMemoryMB 128", "instanceMemoryMB " + instanceMemoryInMB);
			props.put("instanceCpuCores 0", "instanceCpuCores " + instanceCpuCores);
			
		} else {
			defaultServicePath = DEDICATED_PROVISIONING_ORIGINAL_GROOVY_PATH;
		}
		FileUtils.copyDirectory(new File(defaultServicePath), new File(BUILD_SERVICES_FOLDER + "/" + serviceName));
		File dslFile = new File(BUILD_SERVICES_FOLDER + "/" + serviceName + "/groovy-service.groovy");
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
