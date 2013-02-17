package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.publicprovisioning;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.Service;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.AbstractByonCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.AbstractByonCloudTest;
import org.cloudifysource.quality.iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.framework.utils.DSLUtils;
import org.cloudifysource.quality.iTests.framework.utils.IOUtils;

public class AbstractPublicProvisioningByonCloudTest extends AbstractByonCloudTest {
	
	protected static final String DEFAULT_TEMPLATE_NAME = "SMALL_LINUX";
	
	private static final String PUBLIC_MANUAL_PROVISIONING_ORIGINAL_GROOVY_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/groovy-public-provisioning");
	private static final String PUBLIC_AUTOMATIC_PROVISIONING_ORIGINAL_GROOVY_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/customServiceMonitor-public-provisioning");
		
	private static final String BUILD_SERVICES_FOLDER = SGTestHelper.getBuildDir() + "/recipes/services";
	private static final String BUILD_APPLICATIONS_FOLDER = SGTestHelper.getBuildDir() + "/recipes/apps";
	
	protected void installManualPublicProvisioningServiceAndWait(final String serviceName,
			final int numInstances,
			final int instanceMemoryInMB,
			final double instanceCpuCores, 
			final String templateName, 
			final boolean useManagement) throws IOException, InterruptedException {
		installManualPublicProvisioningServiceAndWait(serviceName, numInstances, instanceMemoryInMB, instanceCpuCores,
				templateName, useManagement, false);
	}

	/**
	 * Installs an application called serviceName + "App".
	 * The application will have only one service called serviceName. and this service will have
	 * all the properties specified.
	 * @param serviceName
	 * @param numInstances
	 * @param instanceMemoryInMB
	 * @param instanceCpuCores
	 * @param templateName
	 * @param useManagement
	 * @param expectedToFail
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected String installManualPublicProvisioningApplicationAndWait(final String serviceName,
			final int numInstances,
			final int instanceMemoryInMB,
			final double instanceCpuCores, 
			final String templateName, 
			final boolean useManagement,
			final boolean expectedToFail) throws IOException, InterruptedException {
		
		Application application = new Application();
		String applicationName = serviceName + "App";
		application.setName(applicationName);
		
		Service service = new Service();
		service.setName(serviceName);
		
		List<Service> services = new ArrayList<Service>();
		services.add(service);
		application.setServices(services);
		
		File applicationFolder = null;
		try {
			applicationFolder = new File(BUILD_APPLICATIONS_FOLDER + "/" + applicationName);
			DSLUtils.writeSimpleApplicationToFolder(application, applicationFolder);
			generateServiceToFolder(applicationFolder.getAbsolutePath(), serviceName, PUBLIC_MANUAL_PROVISIONING_ORIGINAL_GROOVY_PATH, "groovy", 
					numInstances, instanceMemoryInMB, instanceCpuCores, true, templateName, useManagement);
			ApplicationInstaller installer = new ApplicationInstaller(getRestUrl(), applicationName);
			installer.recipePath(applicationName);
			installer.expectToFail(expectedToFail);
			return installer.install();
		} finally {
			if (applicationFolder != null && applicationFolder.exists()) {
				FileUtils.deleteDirectory(applicationFolder);
			}
		}
	}
	
	protected String installManualPublicProvisioningServiceAndWait(final String serviceName,
			final int numInstances,
			final int instanceMemoryInMB,
			final double instanceCpuCores, 
			final String templateName, 
			final boolean useManagement,
			final boolean expectedToFail) throws IOException, InterruptedException {
		
		try {			
			generateServiceToBuildServicesFolder(
					serviceName,
					PUBLIC_MANUAL_PROVISIONING_ORIGINAL_GROOVY_PATH,
					"groovy",
					numInstances, 
					instanceMemoryInMB, 
					instanceCpuCores, 
					true, 
					templateName,
					useManagement);
			return super.installServiceAndWait(BUILD_SERVICES_FOLDER + "/" + serviceName, serviceName, expectedToFail);
		} finally {			
			File serviceDir = new File(BUILD_SERVICES_FOLDER + "/" + serviceName);
			if (serviceDir.exists()) {
				FileUtils.deleteDirectory(serviceDir);
			}
		}
	}
	
	protected void installAutomaticPublicProvisioningServiceAndWait(final String serviceName,
			final int numInstances,
			final int instanceMemoryInMB,
			final double instanceCpuCores, String templateName, boolean useManagement) throws IOException, InterruptedException {
		
		try {
			generateServiceToBuildServicesFolder(
					serviceName,
					PUBLIC_AUTOMATIC_PROVISIONING_ORIGINAL_GROOVY_PATH,
					"customServiceMonitor-public-provisioning",
					numInstances, 
					instanceMemoryInMB, 
					instanceCpuCores, 
					true, 
					templateName,
					useManagement);
			super.installServiceAndWait(BUILD_SERVICES_FOLDER + "/" + serviceName, serviceName);			
		} finally {
			File serviceDir = new File(BUILD_SERVICES_FOLDER + "/" + serviceName);
			if (serviceDir.exists()) {
				FileUtils.deleteDirectory(serviceDir);
			}
		}
	}

	private void generateServiceToFolder(final String folderPath, final String serviceName,
			final String originalServicePath,
			final String originalServiceName,
			final int numInstances,
			final int instanceMemoryInMB,
			final double instanceCpuCores,
			final boolean publicProvisioning, 
			final String templateName,
			final boolean useManagement) throws IOException {
		
		Map<String, String> props = new HashMap<String,String>();
		
		props.put("instanceMemoryMB 128", "instanceMemoryMB " + instanceMemoryInMB);
		props.put("instanceCpuCores 0", "instanceCpuCores " + instanceCpuCores);

		FileUtils.copyDirectory(new File(originalServicePath), new File(folderPath + "/" + serviceName));
		File dslFile = new File(folderPath + "/" + serviceName + "/" + originalServiceName + "-service.groovy");
		File serviceFile = new File(folderPath + "/" + serviceName + "/" + serviceName + "-service.groovy");
		if (!serviceFile.exists()) {
			FileUtils.moveFile(dslFile, serviceFile);
			dslFile.delete();
		}
		
		props.put("name \"groovy\"", "name " + '"' + serviceName + '"');
		props.put("numInstances 2", "numInstances " + numInstances);
		props.put("SMALL_LINUX", templateName);
		props.put("useManagement false", "useManagement " + useManagement);
		IOUtils.replaceTextInFile(serviceFile, props);

	}
	private void generateServiceToBuildServicesFolder(final String serviceName,
			final String originalServicePath,
			final String originalServiceName,
			final int numInstances,
			final int instanceMemoryInMB,
			final double instanceCpuCores,
			final boolean publicProvisioning, 
			final String templateName,
			final boolean useManagement) throws IOException {
		generateServiceToFolder(BUILD_SERVICES_FOLDER, serviceName, originalServicePath,
				originalServiceName, numInstances, instanceMemoryInMB,
				instanceCpuCores, publicProvisioning, templateName,
				useManagement);
	}
}
