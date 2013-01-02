package test.cli.cloudify.cloud.byon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.pu.service.ServiceDetails;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.cloud.services.CloudService;
import test.cli.cloudify.cloud.services.CloudServiceManager;
import test.cli.cloudify.cloud.services.byon.ByonCloudService;
import framework.utils.AssertUtils;
import framework.utils.IOUtils;
import framework.utils.LogUtils;
import framework.utils.SSHUtils;

public abstract class AbstractByonAddRemoveTemplatesTest extends AbstractByonCloudTest {
	String[] mngMachinesIP;
	AtomicInteger numOfMachinesInUse = new AtomicInteger(0);
	List<TemplatesBatchHandler> templatesHandlers;
	AtomicInteger numLastTemplateFolder;
	AtomicInteger numLastAddedTemplate;
	List<String> defaultTempaltes;

	public static final String USER = "tgrid";
	public static final String PASSWORD = "tgrid";
	
	protected final String[] DEFAULT_TEMPLATES = {"SMALL_LINUX"};
	private final String TEMPLATE_NAME_PREFIX = "template_";
	private final String UPLOAD_DIR_NAME_PREFIX = "upload";

	protected final String TEMPLATES_ROOT_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simpleWithTemplates/templates");
	protected final String TEMP_TEMPLATES_DIR_PATH = TEMPLATES_ROOT_PATH + File.separator + "templates.tmp";
	protected final String TEMPLATE_FILE_NAME = "template"; 
	protected final String BOOTSTRAP_MANAGEMENT_FILE_NAME = "bootstrap-management.sh"; 
	protected final String TEMPLATE_NAME_STRING = "TEMPLATE_NAME";
	protected final String UPLOAD_DIR_NAME_STRING = "UPLOAD_DIR_NAME";
	protected final String UPLOAD_PROPERTY_NAME = "uploadDir";
	protected final String NODE_IP_PROPERTY_NAME = "node_ip";
	protected final String NODE_ID_PROPERTY_NAME = "node_id";
	protected final String ABSOLUTE_UPLOAD_DIR_PROPERTY = "absoluteUploadDir";
	protected final String UPLOAD_DIR_NAME_PROPERTY = "localDirectory";

	protected final String SERVICES_ROOT_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simpleWithTemplates/services");
	protected final String TEMP_SERVICES_DIR_PATH = SERVICES_ROOT_PATH + File.separator + "services.tmp";
	protected final String SERVICE_FILE_NAME = "service"; 
	protected final String SERVICE_NAME_PROPERTY_NAME = "serviceName";
	protected final String TEMPLATE_NAME_PROPERTY_NAME = "templateName";

	public abstract int getNumOfMngMachines();
	public abstract boolean isBootstrap();
	public abstract boolean isTeardown();

	@Override
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		CloudService service = null;
		bootstrap(service);
	}

	@Override
	protected void bootstrap(CloudService service) throws Exception {
		
		ByonCloudService byonService;
		
		if(service == null){
			byonService = new ByonCloudService();

		}else{
			byonService = (ByonCloudService)service;
		}
		String[] machines = byonService.getMachines();
		final int numOfMngMachines = getNumOfMngMachines();
		if (machines.length < numOfMngMachines) {
			Assert.fail("Not enough management machines to use.");
		}
		mngMachinesIP = new String[numOfMngMachines];
		StringBuilder ipListBuilder = new StringBuilder();
		for (int i = 0; i < numOfMngMachines; i++) {
			final String nextMachine = machines[i];
			mngMachinesIP[i] = nextMachine;
			if (i > 0) {
				ipListBuilder.append(",");
			}
			ipListBuilder.append(nextMachine);
		}
		byonService.setNumberOfManagementMachines(numOfMngMachines);
		numOfMachinesInUse.addAndGet(numOfMngMachines);
		LogUtils.log("Updating MNG machine IPs: " + ipListBuilder);
		byonService.setIpList(ipListBuilder.toString());

		if (isBootstrap()) {
			super.bootstrap(byonService);
		} else {
			this.cloudService = CloudServiceManager.getInstance().getCloudService(this.getCloudName());
			AdminFactory factory = new AdminFactory();
			for (String mngMachineIP : mngMachinesIP) {				
				factory.addLocators(mngMachineIP + ":" + CloudifyConstants.DEFAULT_LUS_PORT);
			}
			admin = factory.createAdmin();
		}
		defaultTempaltes = new LinkedList<String>();
		for (String templateName : DEFAULT_TEMPLATES) {			
			defaultTempaltes.add(templateName);
		}
	}
	
	@Override
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		if (isTeardown()) {
			super.teardown();
		}
	}

	@Override
	protected String getRestUrl() {
		if (!isBootstrap()) {
			return "http://" + mngMachinesIP[0] + ":8100";
		}
		return super.getRestUrl();
	}
	protected String addTempaltes(TemplatesBatchHandler handler) {
		return addTempaltes(handler, null);
	}
	protected String addTempaltes(TemplatesBatchHandler handler, String outputContains) {
		String command = "connect " + getRestUrl() + ";add-templates " + handler.getTemplatesFolder();
		String output = null;
		try {
			List<String> expectedFailedToAddTemplates = handler.getExpectedFailedTempaltes();
			if (expectedFailedToAddTemplates != null && !expectedFailedToAddTemplates.isEmpty()) {
				output = CommandTestUtils.runCommandExpectedFail(command);
			} else {
				output = CommandTestUtils.runCommandAndWait(command);
			}
			if (outputContains != null) {
				Assert.assertTrue(output.contains(outputContains));
			}
		} catch (final Exception e) {
			LogUtils.log(e.getMessage(), e);
			Assert.fail(e.getMessage());
		}
		return output;
	}

	protected String removeTemplate(TemplatesBatchHandler handler, String templateName, boolean expectedToFail, String expectedOutput) {
		if (!expectedToFail) {
			handler.removeTemplate(templateName);
		}
		return removeTemplate(templateName, expectedToFail, expectedOutput);
	}
	
	protected String removeTemplate(String templateName, boolean expectToFail, String expectedOutput) {
		
		String command = "connect " + getRestUrl() + ";remove-template " + templateName;
		String output = "no output";
		try {
			if (expectToFail) {
				output = CommandTestUtils.runCommandExpectedFail(command);
			} else {
				output = CommandTestUtils.runCommandAndWait(command);
			}
			if (expectedOutput != null) {
				Assert.assertTrue(output.contains(expectedOutput));
			}
		} catch (final Exception e) {
			LogUtils.log(e.getMessage(), e);
			Assert.fail(e.getMessage());
		}
		
		return output;
	}
	
	protected void installService(String serviceName, String templateName, boolean expectToFail) {
		try {
			installServiceAndWait(createServiceDir(serviceName, templateName), serviceName, 5 , expectToFail);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	protected String createServiceDir(String serviceName, String templateName) throws IOException {
		File servicesTempFolder = new File(TEMP_SERVICES_DIR_PATH);
		if (!servicesTempFolder.exists()) {
			servicesTempFolder.mkdir();
		}
		File serviceFolder = new File(servicesTempFolder,  serviceName);
		serviceFolder.mkdir();
		File serviceFile = new File(SERVICES_ROOT_PATH, SERVICE_FILE_NAME);
		File tempServiceFile = new File(serviceFolder, serviceName + DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX);
		FileUtils.copyFile(serviceFile, tempServiceFile);

		Properties props = new Properties();
		props.put(SERVICE_NAME_PROPERTY_NAME, '"' + serviceName + '"');
		props.put(TEMPLATE_NAME_PROPERTY_NAME, '"' + templateName + '"');
		String proeprtiesFileName = serviceName + "-service" + DSLUtils.PROPERTIES_FILE_SUFFIX;
		File servicePropsFile = new File(serviceFolder, proeprtiesFileName);
		IOUtils.writePropertiesToFile(props, servicePropsFile);

		return serviceFolder.getAbsolutePath();
	}

	protected void assertRightUploadDir(String serviceName, String expectedUploadDirName) {
		ProcessingUnits processingUnits = admin.getProcessingUnits();
		ProcessingUnit processingUnit = processingUnits.getProcessingUnit("default." + serviceName);
		ProcessingUnitInstance processingUnitInstance = processingUnit.getInstances()[0];
		Collection<ServiceDetails> detailes = processingUnitInstance.getServiceDetailsByServiceId().values();		
		final Map<String, Object> allDetails = new HashMap<String, Object>();
		for (final ServiceDetails serviceDetails : detailes) {
			allDetails.putAll(serviceDetails.getAttributes());
		}
		Object uploadDetail = allDetails.get("UPLOAD_NAME");
		Assert.assertNotNull(uploadDetail);
		Assert.assertEquals(expectedUploadDirName, uploadDetail.toString());
	}
	
	protected void assertExpectedListTempaltes() {
		assertListTempaltes(getExpectedExistTemplateNames());
	}
	
	protected void assertListTempaltes(List<String> expectedTemplatesList) {
		try {
			String output = listTemplates();
			List<String> templateNames = getTemplateNamesFromOutput(output);
			assertEquals("Expected tempaltes: " + expectedTemplatesList + ", but was: " 
					+ templateNames, expectedTemplatesList.size(), templateNames.size());
			for (String templateName : expectedTemplatesList) {
				Assert.assertTrue(templateNames.contains(templateName));
			}
		} catch (final Exception e) {
			LogUtils.log(e.getMessage(), e);
			Assert.fail(e.getMessage());
		}
	}

	protected List<String> getExpectedExistTemplateNames() {
		List<String> names = new LinkedList<String>();
		names.addAll(defaultTempaltes);
		for (TemplatesBatchHandler handler : templatesHandlers) {
			final List<String> expectedTempaltesExist = handler.getExpectedTempaltesExist();
			if (expectedTempaltesExist != null) {
				names.addAll(expectedTempaltesExist);
			}
		}
		return names;
	}
	
	private String getNextMachineIP(boolean forServiceInstallation) {
		if (forServiceInstallation) {
			String[] machines = getService().getMachines();
			final int nextMachine = numOfMachinesInUse.getAndIncrement();
			if (machines.length <= nextMachine) {
				Assert.fail("Cannot allocate machine number " + nextMachine + ", there are only " + machines.length + " machines to use.");
			}
			return machines[nextMachine];
		}
		return mngMachinesIP[0];
	}

	private void replaceStringInFile(File readFrom, File writeTo, String stringToReplace, String replacement) 
			throws IOException {

		BufferedReader reader = new BufferedReader(new FileReader(readFrom));
		PrintWriter writer = new PrintWriter(new FileWriter(writeTo));
		String line = null;
		while ((line = reader.readLine()) != null) {
			writer.print(line.replace(stringToReplace, replacement));
			writer.print("\n");
		}
		reader.close();
		writer.close();		
	}
	
	private List<String> getTemplateNamesFromOutput(String outputTemplatesList) {
		List<String> templateNames = new LinkedList<String>();
		templateNames.addAll(defaultTempaltes);
		String templates = outputTemplatesList;
		while(true) {
			int begin = templates.indexOf(TEMPLATE_NAME_PREFIX);
			if (begin == -1) {
				break;
			}
			int end = templates.indexOf(":", begin);
			String nextTemplateName = templates.substring(begin, end).trim();
			templates = templates.substring(end);
			templateNames.add(nextTemplateName);
		}
		return templateNames;		
	}
	
	public String getUploadDirName(String templateName, String outputTemplatesList) throws Exception{
		
		int index = outputTemplatesList.indexOf(templateName);
		
		if(index == -1){
			AssertUtils.assertFail("the template " + templateName + " does not exist");
		}
		
		index = outputTemplatesList.indexOf(UPLOAD_DIR_NAME_PROPERTY, index) + UPLOAD_DIR_NAME_PROPERTY.length() + 3;
		int endIndex = outputTemplatesList.indexOf(",", index);
		
		return outputTemplatesList.substring(index, endIndex);
		
	}
	
	public String getTemplateRemoteDirFullPath(String templateName) throws Exception{
		 
		String output = listTemplates();
		int index = output.indexOf(templateName);
		
		if(index == -1){
			AssertUtils.assertFail("the template " + templateName + " does not exist");
		}
		
		index = output.indexOf(ABSOLUTE_UPLOAD_DIR_PROPERTY, index) + ABSOLUTE_UPLOAD_DIR_PROPERTY.length() + 3;
		int endIndex = output.indexOf(getUploadDirName(templateName, output), index);
				
		return output.substring(index, endIndex);
		
	}
	
	public String listTemplates() throws Exception{
		
		String command = "connect " + getRestUrl() + ";list-templates";
		String output = CommandTestUtils.runCommandAndWait(command);
		
		return output;
		
	}
	
	protected void removeAllAddedTempaltes(List<String> templateNames) throws Exception {
		for (String templateName : templateNames) {
			if (defaultTempaltes.contains(templateName)) {
				continue;
			}
			removeTemplate(templateName, false, null);
		}
		assertListTempaltes(defaultTempaltes);
	}
	
	@BeforeMethod(alwaysRun = true) 
	public void init() {
		templatesHandlers = new LinkedList<TemplatesBatchHandler>();
		numOfMachinesInUse = new AtomicInteger(getNumOfMngMachines());
		numLastTemplateFolder = new AtomicInteger(0);
	}
	
	@AfterMethod(alwaysRun = true)
	public void clean() throws Exception {
		File tempalteFolder = new File(TEMP_TEMPLATES_DIR_PATH);
		FileUtils.deleteQuietly(tempalteFolder);
		File serviceFolder = new File(TEMP_SERVICES_DIR_PATH);
		FileUtils.deleteQuietly(serviceFolder);
		String remoteDir = getService().getCloud().getTemplates().get(DEFAULT_TEMPLATES[0]).getRemoteDirectory();
		try{
			removeAllAddedTempaltes(getTemplateNamesFromOutput(listTemplates()));
		}
		catch (AssertionError ae){
			for(String mngMachineIP : mngMachinesIP){
				SSHUtils.runCommand(mngMachineIP, OPERATION_TIMEOUT, "rm -f -r " + remoteDir + "/" + CloudifyConstants.ADDITIONAL_TEMPLATES_FOLDER_NAME, USER, PASSWORD);
			}
		}
	}
	
	public void verifyTemplateExistence(String machineIP, TemplateDetails template, String templateRemotePath, boolean shouldExist) throws Exception {
		
		String output;
		
		String templateName = template.getTemplateName();
		String command = "if [ -f " + templateRemotePath + " ]; then echo " + shouldExist + "; else echo " + (!shouldExist) + "; fi";

		output = SSHUtils.runCommand(machineIP, OPERATION_TIMEOUT, command, USER, PASSWORD);
		
		if(shouldExist){			
			AssertUtils.assertTrue("the template " + templateName + " doesn't exist in " + mngMachinesIP[0] + " under " + templateRemotePath, output.contains(Boolean.toString(shouldExist)));
		}
		else{
			AssertUtils.assertTrue("the template " + templateName + " exists in " + mngMachinesIP[0] + " under " + templateRemotePath, output.contains(Boolean.toString(!shouldExist)));
		}
	}

	public class TemplatesBatchHandler {
		private File templatesFolder;
		private Map<String, TemplateDetails> templates;
		private List<String> expectedTempaltesExist;
		private List<String> expectedFailedTempaltes;

		public TemplatesBatchHandler() {
			File tempaltesTempFolder = new File(TEMP_TEMPLATES_DIR_PATH);
			if (!tempaltesTempFolder.exists()) {
				tempaltesTempFolder.mkdir();
			}
			File newTemplatesFolder = new File(tempaltesTempFolder, TEMPLATE_NAME_PREFIX + numLastTemplateFolder.getAndIncrement());
			newTemplatesFolder.mkdir();
			this.templatesFolder = newTemplatesFolder;

			templates = new HashMap<String, AbstractByonAddRemoveTemplatesTest.TemplateDetails>();
			
			numLastAddedTemplate = new AtomicInteger(0);
			
			templatesHandlers.add(this);
		}

		public List<TemplateDetails> addTemplates(int num) throws IOException {
			List<TemplateDetails> addedTemplates = new LinkedList<TemplateDetails>();
			for (int i = 0; i < num; i++) {
				TemplateDetails addedTempalte = addTemplate();
				addedTemplates.add(addedTempalte);
			}
			return addedTemplates;
		}

		public TemplateDetails addTemplate() throws IOException {
			return addCustomTemplate(new TemplateDetails(), false, false);
		}
		public TemplateDetails addExpectedToFailTempalte() throws IOException {
			return addCustomTemplate(new TemplateDetails(), false, true);
		}
		public TemplateDetails addServiceTemplate() throws IOException {
			return addCustomTemplate(new TemplateDetails(), true, false);
		}
		public TemplateDetails addExpectedToFailTemplate(TemplateDetails template) throws IOException {
			return addCustomTemplate(template, false, true);
		}
		public TemplateDetails addCustomTemplate(TemplateDetails template, boolean isForService, boolean expectedToFail) 
				throws IOException {
			int size = numLastAddedTemplate.getAndIncrement();
			String templateName = template.getTemplateName();
			if (templateName == null) {
				templateName = templatesFolder.getName() + "_" + size;
			}
			File basicTemplateFile = new File(TEMPLATES_ROOT_PATH, TEMPLATE_FILE_NAME);
			File addedTemplateFile = template.getTemplateFile();
			if(addedTemplateFile == null) {
				addedTemplateFile = new File(templatesFolder, templateName + DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX);
			}
			replaceStringInFile(basicTemplateFile, addedTemplateFile, TEMPLATE_NAME_STRING, templateName);

			Properties props = new Properties();
			String uploadDirName = template.getUploadDirName();
			if (uploadDirName == null) {
				uploadDirName = UPLOAD_DIR_NAME_PREFIX + size;
			}
			props.put(UPLOAD_PROPERTY_NAME, '"' + uploadDirName + '"');
			String nodeIP = template.getMachineIP();
			if (nodeIP == null) {
				nodeIP = getNextMachineIP(isForService);
			}
			props.put(NODE_IP_PROPERTY_NAME, '"' + nodeIP + '"');
			props.put(NODE_ID_PROPERTY_NAME, "\"byon-pc-lab-" + nodeIP + "{0}\"");
			File templatePropsFile = template.getTemplatePropertiesFile();
			if (templatePropsFile == null) {
				final String templateFileName = addedTemplateFile.getName();
				final int tempalteFileNamePrefixEndIndex = templateFileName.indexOf(".");
				final String templateFileNamePrefix = templateFileName.substring(0, tempalteFileNamePrefixEndIndex);
				String proeprtiesFileName =  templateFileNamePrefix + DSLUtils.PROPERTIES_FILE_SUFFIX;				
				templatePropsFile = new File(templatesFolder, proeprtiesFileName);
			}
			IOUtils.writePropertiesToFile(props, templatePropsFile);

			File uploadFolder = new File(templatesFolder, uploadDirName);
			uploadFolder.mkdir();

			File basicBootstrapManagementFile = new File(TEMPLATES_ROOT_PATH, BOOTSTRAP_MANAGEMENT_FILE_NAME);
			final File addedBootstrapManagementFile = new File(uploadFolder, BOOTSTRAP_MANAGEMENT_FILE_NAME);
			replaceStringInFile(basicBootstrapManagementFile, addedBootstrapManagementFile, UPLOAD_DIR_NAME_STRING, uploadDirName);
		
			TemplateDetails tempalteDetails = new TemplateDetails(templateName, addedTemplateFile, templatePropsFile, uploadDirName, nodeIP);
			templates.put(templateName, tempalteDetails);
			
			if (expectedToFail) {
				if (expectedFailedTempaltes == null) {
					expectedFailedTempaltes = new LinkedList<String>();
				}
				expectedFailedTempaltes.add(templateName);
			} else {
				if (expectedTempaltesExist == null) {
					expectedTempaltesExist = new LinkedList<String>();
				}
				expectedTempaltesExist.add(templateName);
			}
			
			return tempalteDetails;
		}

		public void removeTemplate(String templateName) {
			expectedTempaltesExist.remove(templateName);
			TemplateDetails templateDetails = templates.get(templateName);
			templateDetails.getTemplateFile().delete();
			templateDetails.getTemplatePropertiesFile().delete();
		}

		public File getTemplatesFolder() {
			return templatesFolder;
		}
		
		public void setTemplatesFolder(File templatesFolder) {
			this.templatesFolder = templatesFolder;
		}

		public Map<String, TemplateDetails> getTemplates() {
			return templates;
		}
		public List<String> getExpectedTempaltesExist() {
			return expectedTempaltesExist;
		}
		public List<String> getExpectedFailedTempaltes() {
			return expectedFailedTempaltes;
		}
	}

	public class TemplateDetails {
		private String templateName;
		private File templateFile;
		private File templatePropertiesFile;
		private String uploadDirName;
		private String machineIP;

		TemplateDetails() {
			
		}
		
		TemplateDetails(String templateName, File templateFile, File propertiesFile, String uploadDirName, String machineIP) {
			this.templateName = templateName;
			this.templateFile = templateFile;
			this.templatePropertiesFile = propertiesFile;
			this.uploadDirName = uploadDirName;
			this.machineIP = machineIP;
		}

		public String getTemplateName() {
			return templateName;
		}
		public void setTemplateName(String name) {
			templateName = name;
		}
		public File getTemplateFile() {
			return templateFile;
		}
		public void setTemplateFile(File templateFile) {
			this.templateFile = templateFile;
		}
		public String getUploadDirName() {
			return uploadDirName;
		}
		public void setUploadDirName(String uploadDirName) {
			this.uploadDirName = uploadDirName;
		}
		public String getMachineIP() {
			return machineIP;
		}

		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof TemplateDetails)) {
				return false;
			}
			TemplateDetails tempalteDetails = (TemplateDetails) obj;
			return this.templateName.equals(tempalteDetails.getTemplateName());
		}

		public File getTemplatePropertiesFile() {
			return templatePropertiesFile;
		}

		public void setTemplatePropertiesFile(File templatePropertiesFile) {
			this.templatePropertiesFile = templatePropertiesFile;
		}
	}
}
