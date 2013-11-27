package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;

public abstract class TemplatesFolderHandler {
	protected File folder;
	protected List<String> expectedFailedTemplates;
	protected List<String> expectedToBeAddedTempaltes;
	protected Map<String, TemplateDetails> templates;
	private AtomicInteger numLastAddedTemplate;
	
	public TemplatesFolderHandler(final File folder) {
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
		templateDetails.setExpectedToFailOnAdd(true);
		return addCustomTemplate(templateDetails);
	}
	
	public TemplateDetails createAndAddDefaultTempalte() 
			throws IOException {
		TemplateDetails templateDetails = new TemplateDetails();
		return addCustomTemplate(templateDetails);
	}
	
	public TemplateDetails addCustomTemplate(final TemplateDetails template) {
		
		// get and set template name
		final int suffix = numLastAddedTemplate.getAndIncrement();
		String updatedTemplateName = template.getTemplateName();
		if (updatedTemplateName == null) {
			updatedTemplateName = folder.getName() + "_" + suffix;
			template.setTemplateName(updatedTemplateName);
		}
		template.setTemplateFolder(folder);
		// create the template
		TemplateDetails createdTemplate = 
				getTempalteCreator().createCustomTemplate(template);
		createdTemplate.setForServiceInstallation(template.isForServiceInstallation());

		// updates properties if needed
		updatePropertiesFile(createdTemplate);
		
		templates.put(updatedTemplateName, createdTemplate);
		if (template.isExpectedToFailOnAdd()) {
			expectedFailedTemplates.add(updatedTemplateName);
		} else {
			expectedToBeAddedTempaltes.add(updatedTemplateName);
		}
		
		return createdTemplate;
	}

	public abstract void updatePropertiesFile(final TemplateDetails template);
	public abstract TemplateCreator getTempalteCreator();
	
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
			if (templateDetails.isExpectedToFailOnAdd()) {
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
		return (!expectedFailedTemplates.isEmpty() && expectedToBeAddedTempaltes.isEmpty());
	}
	public boolean isPartialFailureExpected() {
		return !expectedFailedTemplates.isEmpty() && !expectedToBeAddedTempaltes.isEmpty();
	}

	public Map<String, TemplateDetails> getTemplates() {
		return templates;
	}

}
