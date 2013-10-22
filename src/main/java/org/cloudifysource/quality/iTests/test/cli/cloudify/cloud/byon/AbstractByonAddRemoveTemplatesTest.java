package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;
import iTests.framework.utils.SSHUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.ByonTemplatesHandler;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.SimpleServiceCreator;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplateDetails;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplatesCommands;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplatesCommandsRestAPI;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.pu.service.ServiceDetails;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class AbstractByonAddRemoveTemplatesTest extends AbstractByonCloudTest {

	private final String TEMPLATES_ROOT_PATH = CommandTestUtils.getPath("src/main/resources/templates");
	private final String TEMP_TEMPLATES_DIR_PATH = TEMPLATES_ROOT_PATH + File.separator + "templates.tmp";
	
	String[] mngMachinesIP;
	String[] machines;
	protected ByonTemplatesHandler templatesHandler;
	SimpleServiceCreator serviceCreator;
	List<String> defaultTemplates;
	protected String username;
	protected String password;

	public static final String USER = "tgrid";
	public static final String PASSWORD = "tgrid";

	private final String[] DEFAULT_TEMPLATES = { "SMALL_LINUX" };

	private final String ABSOLUTE_UPLOAD_DIR_NAME = "absoluteUploadDir";
	private final String UPLOAD_DIR_NAME_NAME = "localDirectory";
	
	public abstract int getNumOfMngMachines();

	@Override
	protected void customizeCloud() throws Exception {
		super.customizeCloud();
		machines = getService().getMachines();
		final int numOfMngMachines = getNumOfMngMachines();
		if (machines.length < numOfMngMachines) {
			Assert.fail("Not enough management machines to use.");
		}
		mngMachinesIP = new String[numOfMngMachines];
		final StringBuilder ipListBuilder = new StringBuilder();
		for (int i = 0; i < numOfMngMachines; i++) {
			final String nextMachine = machines[i];
			mngMachinesIP[i] = nextMachine;
			if (i > 0) {
				ipListBuilder.append(",");
			}
			ipListBuilder.append(nextMachine);
		}
		getService().setNumberOfManagementMachines(numOfMngMachines);
		LogUtils.log("Updating MNG machine IPs: " + ipListBuilder);
		getService().setIpList(ipListBuilder.toString());
	}

	@Override
	protected void afterBootstrap() throws Exception {
		super.afterBootstrap();
	}

	@BeforeMethod(alwaysRun = true)
	public void init() {
		defaultTemplates = new LinkedList<String>();
		for (final String templateName : DEFAULT_TEMPLATES) {
			defaultTemplates.add(templateName);
		}
		File templatesTempFolder = new File(TEMP_TEMPLATES_DIR_PATH);
		templatesTempFolder.mkdirs();
		templatesHandler = new ByonTemplatesHandler(defaultTemplates, getRestUrl(), mngMachinesIP.length, machines, templatesTempFolder);
		serviceCreator = new SimpleServiceCreator();
	}

	@AfterMethod(alwaysRun = true)
	public void clean() throws Exception {
		templatesHandler.clean();
		serviceCreator.clean();
		try {
			removeAllTemplates();
		} catch (final AssertionError e) {
			final String remoteDir =
					getService().getCloud().getCloudCompute().getTemplates().get(DEFAULT_TEMPLATES[0])
							.getRemoteDirectory();
			for (final String mngMachineIP : mngMachinesIP) {
				LogUtils.log("removing folder " + remoteDir + '/' + CloudifyConstants.ADDITIONAL_TEMPLATES_FOLDER_NAME);
				String commandOutput = SSHUtils.runCommand(
						mngMachineIP,
						AbstractTestSupport.OPERATION_TIMEOUT,
						"rm -f -r " + remoteDir + "/" + CloudifyConstants.ADDITIONAL_TEMPLATES_FOLDER_NAME,
						USER,
						PASSWORD);
				LogUtils.log("templates folder removal output is: " + commandOutput);
			}
		}
		templatesHandler.assertExpectedList(username, password);
	}

	private void removeAllTemplates() {
		List<String> listTemplates = TemplatesCommandsRestAPI.listTemplates(getRestUrl(), username, password);
		Map<String, String> failedTempaltesList = new HashMap<String, String>();
		listTemplates.removeAll(defaultTemplates);
		boolean failed = false;
		for (String templateName : listTemplates) {
			try {
				TemplatesCommands.removeTemplateCLI(getRestUrl(), templateName, false);
			} catch (AssertionError error) {
				failedTempaltesList.put(templateName, error.getLocalizedMessage());
				failed = true;
			}
		}
		if (failed) {
			throw new AssertionError("failed to remove templates: " + failedTempaltesList);
		}
		TemplatesCommandsRestAPI.assertExpectedList(getRestUrl(), defaultTemplates, username, password);
	}

	protected String installServiceWithCoputeTemplate(final String serviceName, final String templateName,
			final boolean expectToFail) {
		try {
			final File serviceDir = serviceCreator.createServiceDir(serviceName, templateName);
			return installServiceAndWait(serviceDir.getAbsolutePath(), serviceName, 5, expectToFail);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
		return null;
	}

	protected void assertRightUploadDir(final String serviceName, final String expectedUploadDirName) {
		final ProcessingUnits processingUnits = admin.getProcessingUnits();
		final ProcessingUnit processingUnit = processingUnits.getProcessingUnit("default." + serviceName);
		final ProcessingUnitInstance processingUnitInstance = processingUnit.getInstances()[0];
		final Collection<ServiceDetails> detailes = processingUnitInstance.getServiceDetailsByServiceId().values();
		final Map<String, Object> allDetails = new HashMap<String, Object>();
		for (final ServiceDetails serviceDetails : detailes) {
			allDetails.putAll(serviceDetails.getAttributes());
		}
		final Object uploadDetail = allDetails.get(SimpleServiceCreator.UPLOAD_ENV_NAME);
		Assert.assertNotNull(uploadDetail);
		Assert.assertEquals(expectedUploadDirName, uploadDetail.toString());
	}
	
	public String getTemplateRemoteDirFullPath(final String templateName) throws Exception {

		final String output = TemplatesCommands.getTemplateCLI(getRestUrl(), templateName, false);

		int index = output.indexOf(ABSOLUTE_UPLOAD_DIR_NAME) + ABSOLUTE_UPLOAD_DIR_NAME.length() + 3;
		final int endIndex = output.indexOf(getUploadDirName(output), index);

		return output.substring(index, endIndex);
	}
	
	private String getUploadDirName(final String getTemplateOutput) throws Exception {

		final int index = getTemplateOutput.indexOf(UPLOAD_DIR_NAME_NAME) + UPLOAD_DIR_NAME_NAME.length() + 3;
		final int endIndex = getTemplateOutput.indexOf(",", index);

		return getTemplateOutput.substring(index, endIndex);

	}

	public void verifyTemplateExistence(final String machineIP, final TemplateDetails template,
			final String templateRemotePath, final boolean shouldExist) throws Exception {

		String output;

		final String templateName = template.getTemplateName();
		final String command =
				"if [ -f " + templateRemotePath + " ]; then echo " + shouldExist + "; else echo " + !shouldExist
						+ "; fi";

		output = SSHUtils.runCommand(machineIP, AbstractTestSupport.OPERATION_TIMEOUT, command, USER, PASSWORD);

		if (shouldExist) {
			AssertUtils.assertTrue("the template " + templateName + " doesn't exist in " + mngMachinesIP[0] + " under "
					+ templateRemotePath, output.contains(Boolean.toString(shouldExist)));
		}
		else {
			AssertUtils.assertTrue("the template " + templateName + " exists in " + mngMachinesIP[0] + " under "
					+ templateRemotePath, output.contains(Boolean.toString(!shouldExist)));
		}
	}
}