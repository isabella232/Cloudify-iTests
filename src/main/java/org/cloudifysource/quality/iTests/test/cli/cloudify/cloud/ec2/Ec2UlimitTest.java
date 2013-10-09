package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.IOUtils;
import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.rest.AddTemplatesException;
import org.cloudifysource.quality.iTests.framework.utils.JCloudsUtils;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.RestClientFacade;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.j_spaces.kernel.PlatformVersion;

/**
 * 
 * @author yael
 * 
 */
public class Ec2UlimitTest extends NewAbstractCloudTest {

	private static final String SERVICE_FOLDER_PATH =
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simpleForAddTemplateOnEC2");
	private static final String CREDENTIALS_FOLDER = System.getProperty("iTests.credentialsFolder",
			SGTestHelper.getSGTestRootDir() + "/src/main/resources/credentials");
	private final String PEM_FILE_NAME = "ec2-sgtest-eu.pem";
	private final String PEM_FILE_PATH = CREDENTIALS_FOLDER + "/cloud/ec2/" + PEM_FILE_NAME;
	private static final String SERVICE_NAME = "simpleForEC2";
	private static final String TEMPLATE_FOLDER_PATH =
			CommandTestUtils.getPath("src/main/resources/templates/ubuntu");
	private static final String UPLOAD_FOLDER_PATH = TEMPLATE_FOLDER_PATH + "/ubuntu_upload";
	private static final String TEMPLATE_NAME = "UBUNTU_TEST";
	private static final String TEMPLATE_PROPERTIES_FILE_PATH = TEMPLATE_FOLDER_PATH + "/ubuntu-template.properties";
	private static final String UBUNTU_IMAGE_ID_US = "us-east-1/ami-82fa58eb";
	private static final String UBUNTU_IMAGE_ID_EU = "eu-west-1/ami-ce7b6fba";
	private static final int SERVICE_INSTALLATION_TIMEOUT_IN_MINUTES = 15;

	@Override
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}

	@Override
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
		final File pemFile = new File(UPLOAD_FOLDER_PATH, PEM_FILE_NAME);
		pemFile.delete();
	}

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testUlimitAndPriviligedScript()
			throws IOException, InterruptedException, RestClientException, AddTemplatesException {

		final RestClient client =
				new RestClient(new URL(this.cloudService.getRestUrls()[0]), "", "", PlatformVersion.getVersion());

		final RestClientFacade facade = new RestClientFacade(client);
		// first, create the template folder
		final File templateDirectory = createTemplateDirectory();

		//
		final File serviceOverrides = createServiceOverrides();

		final String servicePath = "src/main/resources/apps/USM/usm/shell-service";
		final File serviceDirectory = new File(servicePath);
		final ServiceInstaller installer =
				new ServiceInstaller(this.cloudService.getRestUrls()[0], serviceDirectory.getAbsolutePath());
		boolean installed = false;
		try {
			facade.addTemplates(templateDirectory);
//			final String addTemplatesResult =
//					"connect " + getRestUrl() + ";add-templates " + templateDirectory.getAbsolutePath();
//			CommandTestUtils.runCommandAndWait(addTemplatesResult);

			Assert.assertTrue(serviceDirectory.exists() && serviceDirectory.isDirectory());

			installer.install();

			final String envResult = installer.invoke("eval-script", "env");
			Assert.assertTrue(envResult.contains("PRIVILIGED_TEST_MARKER"), "Missing env var from priviliged script");
			
			final String ulimitResult = installer.invoke("eval-script", "ulimit", "-n");
			Assert.assertTrue(ulimitResult.contains("12345"), "Expected ulimit to be 12345, was: " + ulimitResult);
			

			installed = true;

		} finally {
			FileUtils.deleteDirectory(templateDirectory.getParentFile());
			FileUtils.deleteQuietly(serviceOverrides);
			installer.uninstallIfFound();
			try {
				client.removeTemplate("ULIMIT");
			} catch (final RestClientException e) {
				// ignore
			}

		}

	}

	private File createServiceOverrides() throws IOException {
		final String overridesContent = "// Generated by: " + this.getClass().getName() + " \n"
				+ "templateName = \"ULIMIT\"";

		final File tempOverridesFile = File.createTempFile("shell-service", "overrides");
		tempOverridesFile.deleteOnExit();
		FileUtils.writeStringToFile(tempOverridesFile, overridesContent);
		return tempOverridesFile;
	}

	private File createTemplateDirectory() throws IOException {
		final File tempDirectory = File.createTempFile("template", "dir");
		FileUtils.deleteQuietly(tempDirectory);
		final boolean directoryCreated = tempDirectory.mkdirs();
		Assert.assertTrue(directoryCreated, "Failed to create temp dir");

		tempDirectory.deleteOnExit();

		final File originalTemplateDirectory = new File("src/main/resources/templates/ulimitAndPriviligedScript");
		Assert.assertTrue(originalTemplateDirectory.exists());
		FileUtils.copyDirectory(originalTemplateDirectory, tempDirectory);

		final File actualTemplateDirectory = new File(tempDirectory, "ulimitAndPriviligedScript");
		final File propertiesFile = new File(actualTemplateDirectory, "ulimit-template.properties");

		final String originalPropertiesText = FileUtils.readFileToString(propertiesFile);
		final String modifiedPropertiesText = originalPropertiesText + "\n"
				+ "// Properties modified by: " + Ec2UlimitTest.class.getName() + "\n"
				+ "ec2LinuxImageId = " + Ec2CloudService.DEFAULT_EU_WEST_LINUX_AMI;
		FileUtils.writeStringToFile(propertiesFile, modifiedPropertiesText);

		final String bootstrapScriptFilePath =
				SGTestHelper.getBuildDir() + "/clouds/ec2/upload/bootstrap-management.sh";
		final File bootstrapScriptFile = new File(bootstrapScriptFilePath);
		org.testng.Assert.assertTrue(bootstrapScriptFile.exists(),
				"Expected file at: " + bootstrapScriptFile.getAbsolutePath());
		final File uploadDir = new File(actualTemplateDirectory, "upload");
		FileUtils.copyFileToDirectory(bootstrapScriptFile, uploadDir);

		final File pemFile = new File(PEM_FILE_PATH);
		FileUtils.copyFileToDirectory(pemFile, uploadDir);

		return actualTemplateDirectory;

	}

	private void assertImageID(final String expectedImageID) {
		JCloudsUtils.createContext(getService());
		final Set<? extends ComputeMetadata> allNodes = JCloudsUtils.getAllNodes();
		final List<String> foundImages = new LinkedList<String>();
		for (final ComputeMetadata computeMetadata : allNodes) {
			final NodeMetadata nodeMetadata = (NodeMetadata) computeMetadata;
			final String imageId = nodeMetadata.getImageId();
			foundImages.add(imageId);
			if (expectedImageID.equals(imageId)) {
				return;
			}
		}
		Assert.fail("Expecting to find image id " + expectedImageID + ", but found " + foundImages);

	}

	private void updatePropertiesFile() throws IOException {

		final Ec2CloudService service = (Ec2CloudService) getService();

		final Map<String, Object> props = service.getProperties();

		final File propsFile = new File(TEMPLATE_PROPERTIES_FILE_PATH);

		if (service.getRegion().contains("eu")) {
			props.put("ubuntuImageId", UBUNTU_IMAGE_ID_EU);
		} else {
			props.put("ubuntuImageId", UBUNTU_IMAGE_ID_US);
		}
		IOUtils.writePropertiesToFile(props, propsFile);
	}

	private void assertTemplateRemoved(final String templateName) {
		final String command = "connect " + getRestUrl() + ";list-templates";
		try {
			final String output = CommandTestUtils.runCommandAndWait(command);
			Assert.assertFalse(output.contains(templateName));

		} catch (final Exception e) {
			LogUtils.log(e.getMessage(), e);
			Assert.fail(e.getMessage());
		}
	}

	private void copyPemFile() throws IOException {
		final File pemFile = new File(PEM_FILE_PATH);
		final File uploadFolder = new File(UPLOAD_FOLDER_PATH);
		FileUtils.copyFileToDirectory(pemFile, uploadFolder);
	}

}
