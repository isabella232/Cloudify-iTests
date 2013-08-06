package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.DSLUtils;

public abstract class TemplatesFolderHandler {
	private final String TEMPLATE_NAME_STRING = "TEMPLATE_NAME";
	private final String UPLOAD_DIR_NAME_STRING = "UPLOAD_DIR_NAME";
	private final String UPLOAD_DIR_NAME_PREFIX = "upload";
	private final String BOOTSTRAP_MANAGEMENT_FILE_NAME = "bootstrap-management.sh";
		
	protected File folder;
	private List<String> expectedFailedTemplates;
	private List<String> expectedToBeAddedTempaltes;
	private Map<String, TemplateDetails> templates;
	private AtomicInteger numLastAddedTemplate;
	private String expectedAddTemplatesFailureMessage;
	
	public TemplatesFolderHandler(final File folder) {
		super();
		this.folder = folder;
		this.templates = new HashMap<String, TemplateDetails>();
		this.expectedFailedTemplates = new LinkedList<String>();
		this.expectedToBeAddedTempaltes = new LinkedList<String>();
		this.numLastAddedTemplate = new AtomicInteger(0);
	}

	public List<TemplateDetails> addTempaltes(int numTemplatesToAdd) 
			throws IOException {
		List<TemplateDetails> templatesAdded = new LinkedList<TemplateDetails>();
		for (int i = 0; i < numTemplatesToAdd; i++) {
			TemplateDetails templateDetails = new TemplateDetails();
			addCustomTemplate(templateDetails);
			templatesAdded.add(templateDetails);
		}
		return templatesAdded;
	}
	 
	public TemplateDetails addTempalteForServiceInstallation() 
			throws IOException {
		TemplateDetails templateDetails = new TemplateDetails();
		templateDetails.setForServiceInstallation(true);
		return addCustomTemplate(templateDetails);
	}
	
	public TemplateDetails addExpectedToFailTempalte() 
			throws IOException {
		TemplateDetails templateDetails = new TemplateDetails();
		templateDetails.setExpectedToFail(true);
		return addCustomTemplate(templateDetails);
	}
	
	public TemplateDetails addDefaultTempalte() 
			throws IOException {
		TemplateDetails templateDetails = new TemplateDetails();
		return addCustomTemplate(templateDetails);
	}
	
	public TemplateDetails addCustomTemplate(final TemplateDetails template) {
		template.setTemplateFolder(folder);
		final int suffix = numLastAddedTemplate.getAndIncrement();
		String templateName = template.getTemplateName();
		if (templateName == null) {
			templateName = folder.getName() + "_" + suffix;
			template.setTemplateName(templateName);
		}
		File templateFile = template.getTemplateFile();
		if (templateFile == null) {
			templateFile = new File(folder, templateName + DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX);
			template.setTemplateFile(templateFile);
		} else {
			if (!templateFile.exists()) {
				templateFile = new File(folder, templateFile.getName()); 
			} else {
				if (!folder.equals(templateFile.getParentFile())) {
				try {
					FileUtils.copyFileToDirectory(templateFile, folder);
				} catch (IOException e) {
					Assert.fail("caught IOException while tried to copy template's file [" 
							+ templateFile.getAbsolutePath() + "] to folder [" + folder.getAbsolutePath() + "]. error was: " + e.getMessage());
				}
				}
			}
		}
		replaceStringInFile(getBasicTemplateFile(), templateFile, TEMPLATE_NAME_STRING, templateName);

		String uploadDirName = template.getUploadDirName();
		if (uploadDirName == null) {
			uploadDirName = UPLOAD_DIR_NAME_PREFIX + suffix;
			template.setUploadDirName(uploadDirName);
		}
		final File uploadFolder = new File(folder, uploadDirName);
		uploadFolder.mkdir();
		final File updatedBootstrapManagementFile = new File(uploadFolder, BOOTSTRAP_MANAGEMENT_FILE_NAME);
		replaceStringInFile(getBasicBootstrapManagementFile(), updatedBootstrapManagementFile,
				UPLOAD_DIR_NAME_STRING, uploadDirName);

		updatePropertiesFile(template);
		templates.put(templateName, template);
		if (template.isExpectedToFail()) {
			expectedFailedTemplates.add(templateName);
		} else {
			expectedToBeAddedTempaltes.add(templateName);
		}
		return template;
	}

	public abstract File getBasicTemplateFile();
	public abstract File getBasicBootstrapManagementFile();
	public abstract void updatePropertiesFile(final TemplateDetails template);

	private void replaceStringInFile(final File readFrom, final File writeTo, final String stringToReplace,
			final String replacement) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(readFrom));
		} catch (FileNotFoundException e2) {
			Assert.fail("failed to create reader from " + readFrom.getAbsolutePath());
		}
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(writeTo));
		} catch (IOException e1) {
			Assert.fail("failed to create writer from " + writeTo.getAbsolutePath());
		}
		try {
		String line = null;
		while ((line = reader.readLine()) != null) {
			writer.print(line.replace(stringToReplace, replacement));
			writer.print("\n");
		}
		} catch (IOException e) {
			Assert.fail("caught IOException while tring to write to " 
					+ writeTo.getAbsolutePath() + ". error was: " + e.getMessage());
		} finally {
			try {
				reader.close();
			} catch (IOException e) {

			}
			writer.close();
		}
	}

	public File getFolder() {
		return folder;
	}
	
	public void setFolder(File folder) {
		this.folder = folder;
	}

	public void removeTemplate(final String templateName) {
		final TemplateDetails templateDetails = templates.remove(templateName);
		if (templateDetails != null) {
			// remove template's files
			templateDetails.getTemplateFile().delete();
			templateDetails.getTemplatePropertiesFile().delete();
			File uploudDirFile = new File(folder, templateDetails.getUploadDirName());
			try {
				FileUtils.deleteDirectory(uploudDirFile);
			} catch (IOException e) {
				Assert.fail("failed to delete upload directory: " + uploudDirFile.getAbsolutePath() + ", error was: " + e.getMessage());
			}
			// remove from lists
			if (templateDetails.isExpectedToFail()) {
				expectedFailedTemplates.remove(templateName);
			} else {
				expectedToBeAddedTempaltes.remove(templateName);
			}
		}
	}

	public List<String> getExpectedFailedTemplates() {
		return expectedFailedTemplates;
	}

	public List<String> getExpectedToBeAddedTempaltes() {
		return expectedToBeAddedTempaltes;
	}

	public boolean isEmpty() {
		return expectedToBeAddedTempaltes.isEmpty() && expectedFailedTemplates.isEmpty();
	}

	public boolean isFailureExpected() {
		return ((!expectedFailedTemplates.isEmpty() && expectedToBeAddedTempaltes.isEmpty()) || expectedAddTemplatesFailureMessage != null);
	}
	public boolean isPartialFailureExpected() {
		return !expectedFailedTemplates.isEmpty() && !expectedToBeAddedTempaltes.isEmpty();
	}

	public Map<String, TemplateDetails> getTemplates() {
		return templates;
	}

	public String getExpectedAddTemplatesFailureMessage() {
		return expectedAddTemplatesFailureMessage;
	}
	
	public void setExpectedAddTemplatesFailureMessage(final String expectedAddTemplatesFailureMessage) {
		this.expectedAddTemplatesFailureMessage = expectedAddTemplatesFailureMessage;
	}

}
