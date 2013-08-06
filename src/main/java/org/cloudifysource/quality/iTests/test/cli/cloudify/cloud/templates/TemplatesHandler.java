/*
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * *****************************************************************************
 */
package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.rest.response.AddTemplatesResponse;
import org.testng.AssertJUnit;

public abstract class TemplatesHandler {
	private final String TEMPLATES_FOLDER_NAME_PREFIX = "templates_";

	private Map<String, TemplatesFolderHandler> templatesFoldersAdded;
	private final String restUrl;
	private final File templatesTempFolder;
	private final List<String> defaultTemplates;
	private AtomicInteger numLastTemplateFolder;
	private Map<String, TemplatesFolderHandler> addedTemplates;

	public TemplatesHandler(final List<String> defaultTemplates, final String restUrl, final File templatesTempFolder) {
		this.defaultTemplates = defaultTemplates;
		numLastTemplateFolder = new AtomicInteger(0);
		this.templatesTempFolder = templatesTempFolder;
		this.restUrl = restUrl;
	}

	/**
	 * creates new folder and numTemplates templates files in that folder.
	 * 
	 * @param numTemplates
	 *            number of templates files to create.
	 * @return the TemplatesFolder.
	 * @throws IOException
	 */
	public TemplatesFolderHandler createFolderWithTemplates(final int numTemplates)
			throws IOException {
		final TemplatesFolderHandler handler = createNewTemplatesFolder();
		handler.addTempaltes(numTemplates);
		return handler;
	}

	public abstract TemplatesFolderHandler createNewTemplatesFolder();

	protected File createNewFolder() {
		final int suffix = numLastTemplateFolder.getAndIncrement();
		final String folderName = TEMPLATES_FOLDER_NAME_PREFIX + suffix;
		final File newFolder = new File(templatesTempFolder, folderName);
		newFolder.mkdirs();
		return newFolder;
	}

	/**
	 * Adds all the templates. Checks that every template that expected to be successfully added, was added.
	 * 
	 * @return
	 */
	public String addTemplatesToCloud(final TemplatesFolderHandler templatesFolder) {
		final File folder = templatesFolder.getFolder();
		final String output = TemplatesCommands.addTemplates(
				restUrl,
				folder.getAbsolutePath(),
				templatesFolder.isFailureExpected() || templatesFolder.isPartialFailureExpected());
		final String expectedFailureMessage = templatesFolder.getExpectedAddTemplatesFailureMessage();
		if (expectedFailureMessage != null) {
			Assert.assertTrue("output does not contain " + expectedFailureMessage + " (output = " + output + ")",
					output.contains(expectedFailureMessage));
		}
		afterAddTempaltes(templatesFolder);
		return output;
	}

	private void afterAddTempaltes(final TemplatesFolderHandler templatesFolder) {
		if (templatesFoldersAdded == null) {
			templatesFoldersAdded = new HashMap<String, TemplatesFolderHandler>();
		}
		templatesFoldersAdded.put(templatesFolder.getFolder().getName(), templatesFolder);
		if (addedTemplates == null) {
			addedTemplates = new HashMap<String, TemplatesFolderHandler>();
		}
		for (final String temlplateName : templatesFolder.getExpectedToBeAddedTempaltes()) {
			addedTemplates.put(temlplateName, templatesFolder);
		}

	}

	public AddTemplatesResponse addTemplatesToCloudUsingRestAPI(final TemplatesFolderHandler templatesFolder) {
		File zipFile = templatesFolder.getFolder();
		try {
			if (!templatesFolder.getFolder().getName().endsWith(".zip")) {
				zipFile = Packager.createZipFile("templates", templatesFolder.getFolder());
			}
		} catch (final IOException e) {
			AssertJUnit.fail("failed to zip templates folder: " + e.getMessage());
		}
		final AddTemplatesResponse response =
				TemplatesCommandsRestAPI.addTemplates(restUrl, zipFile, templatesFolder.isFailureExpected(),
						templatesFolder.getExpectedAddTemplatesFailureMessage());
		if (response != null) {
			// partial failure or success
			final List<String> successfullyAddedTempaltes = response.getSuccessfullyAddedTempaltes();
			AssertJUnit.assertTrue(ListUtils.subtract(templatesFolder.getExpectedToBeAddedTempaltes(),
					successfullyAddedTempaltes).isEmpty());
			final List<String> failedTemplatesList =
					new LinkedList<String>(response.getFailedToAddTempaltes().keySet());
			AssertJUnit.assertTrue(ListUtils
					.subtract(templatesFolder.getExpectedFailedTemplates(), failedTemplatesList).isEmpty());
			afterAddTempaltes(templatesFolder);
		}
		return response;
	}

	/**
	 * Removes all template from the cloud and from handler.
	 * 
	 * @param templatesFolder
	 * @param templateName
	 * @param expectToFail
	 * @param failedExpectedOutput
	 * @return output
	 * @throws IOException
	 */
	public String removeTemplateFromCloud(final TemplatesFolderHandler templatesFolder, final String templateName,
			final boolean expectToFail, final String failedExpectedOutput)
			throws IOException {
		final String output = TemplatesCommands.removeTemplate(restUrl, templateName, expectToFail);

		if (!expectToFail) {
			AssertJUnit.assertTrue(output.contains("Template " + templateName + " removed successfully"));

		} else if (failedExpectedOutput != null) {
			AssertJUnit.assertTrue(output.contains(failedExpectedOutput));
		}
		if (!expectToFail) {
			afterTemplateRemoved(templateName);
		}
		return output;
	}

	public void removeTemplatesFromCloud(final List<String> listTemplates) {
		TemplatesCommands.removeTemplates(restUrl, listTemplates);
		for (final String templateName : listTemplates) {
			afterTemplateRemoved(templateName);
		}
	}

	public void afterTemplateRemoved(final String templateName) {
		if (addedTemplates != null) {
			final TemplatesFolderHandler folderHandler = addedTemplates.remove(templateName);
			if (folderHandler != null) {
				folderHandler.removeTemplate(templateName);
				if (folderHandler.getTemplates().isEmpty() && templatesFoldersAdded != null) {
					templatesFoldersAdded.remove(folderHandler.getFolder().getName());
				}
			}
		}
	}

	public void removeAllAddedTemplatesFromCloud() throws IOException {
		final Set<Entry<String, TemplatesFolderHandler>> entrySet = addedTemplates.entrySet();
		final Set<Entry<String, TemplatesFolderHandler>> clonedSet =
				new HashSet<Map.Entry<String, TemplatesFolderHandler>>(entrySet);
		for (final Entry<String, TemplatesFolderHandler> entry : clonedSet) {
			final String templateName = entry.getKey();
			final TemplatesFolderHandler templateFolderHandler = entry.getValue();
			removeTemplateFromCloud(templateFolderHandler, templateName, false, null);
		}
	}

	public void removeTemplatesFromCloudUsingRestAPI(final TemplatesFolderHandler templatesFolder,
			final String templateName, final boolean expectToFail, final String failureMsgContains) {
		TemplatesCommandsRestAPI.removeTemplate(restUrl, templateName, expectToFail, failureMsgContains);
		if (!expectToFail) {
			afterTemplateRemoved(templateName);
		}
	}

	public void assertExpectedList() {
		assertExpectedList("", "");
	}

	public void assertExpectedList(final String username, final String password) {
		TemplatesCommandsRestAPI.assertExpectedList(restUrl, getExpectedTemplatesExist(), username, password);
	}

	private List<String> getExpectedTemplatesExist() {
		final List<String> templateNames = new LinkedList<String>();
		templateNames.addAll(defaultTemplates);
		if (templatesFoldersAdded != null) {
			for (final TemplatesFolderHandler templateFolder : templatesFoldersAdded.values()) {
				final List<String> expectedTemplatesExist = templateFolder.getExpectedToBeAddedTempaltes();
				if (expectedTemplatesExist != null) {
					templateNames.addAll(expectedTemplatesExist);
				}
			}
		}
		return templateNames;
	}

	public void clean() throws IOException {
		templatesFoldersAdded = null;
		numLastTemplateFolder = new AtomicInteger(0);
		templatesFoldersAdded = new HashMap<String, TemplatesFolderHandler>();
		addedTemplates = new HashMap<String, TemplatesFolderHandler>();
		FileUtils.deleteDirectory(templatesTempFolder);
	}

}
