 /* Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates;

import iTests.framework.utils.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;

public class TemplateCreator {
	protected final static String TEMPLATES_ROOT_PATH = CommandTestUtils.getPath("src/main/resources/templates");
	private final static String BASIC_TEMPLATE_FILE_NAME = "basic_template";

	private final static String TEMPLATE_NAME_STRING = "TEMPLATE_NAME";
	private final static String UPLOAD_DIR_NAME_STRING = "UPLOAD_DIR_NAME";
	private final static String UPLOAD_DIR_NAME_PREFIX = "upload";
	private final static String BOOTSTRAP_MANAGEMENT_FILE_NAME = "bootstrap-management.sh";
	
	private static final String UPLOAD_PROPERTY_NAME = "uploadDir";
	
	public TemplateDetails getNewTempalte() {
		return new TemplateDetails();
	}
	
	public TemplateDetails createTemplate(final String templateName, final File templateFile, 
			final File templateFolder, final String uploadDirName) {
		
		TemplateDetails template = getNewTempalte();
		template.setTemplateFolder(templateFolder);
		
		// name
		String updatedTemplateName = templateName;
		if (updatedTemplateName == null) {
			updatedTemplateName = templateFolder.getName();
		}
		template.setTemplateName(updatedTemplateName);
		
		// template file
		File updatedTemplateFile = templateFile;
		if (updatedTemplateFile == null) {
			updatedTemplateFile = new File(templateFolder, updatedTemplateName + DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX);
		} else {
			if (!updatedTemplateFile.exists()) {
				updatedTemplateFile = new File(templateFolder, updatedTemplateFile.getName()); 
			} else {
				if (!templateFolder.equals(updatedTemplateFile.getParentFile())) {
				try {
					FileUtils.copyFileToDirectory(updatedTemplateFile, templateFolder);
				} catch (IOException e) {
					Assert.fail("caught IOException while tried to copy template's file [" 
							+ updatedTemplateFile.getAbsolutePath() + "] to folder [" + templateFolder.getAbsolutePath() + "]. error was: " + e.getMessage());
				}
				}
			}
		}
		template.setTemplateFile(updatedTemplateFile);
		replaceStringInFile(getBasicTemplateFile(), updatedTemplateFile, TEMPLATE_NAME_STRING, updatedTemplateName);

		// upload directory
		String updatedUploadDirName = uploadDirName;
		if (updatedUploadDirName == null) {
			updatedUploadDirName = UPLOAD_DIR_NAME_PREFIX + "_" + templateFolder.getName();
		}
		template.setUploadDirName(updatedUploadDirName);
		final File uploadFolder = new File(templateFolder, updatedUploadDirName);
		uploadFolder.mkdir();
		
		// bootstrap management file
		final File updatedBootstrapManagementFile = new File(uploadFolder, BOOTSTRAP_MANAGEMENT_FILE_NAME);
		replaceStringInFile(getBasicBootstrapManagementFile(), updatedBootstrapManagementFile,
				UPLOAD_DIR_NAME_STRING, updatedUploadDirName);

		// properties file
		File templatePropsFile = createPropertiesFileForTemplate(template);
		template.setTemplatePropertiesFile(templatePropsFile);
		
		return template;
	}
	
	public File createPropertiesFileForTemplate(final TemplateDetails template) {
		final Properties props = new Properties();
		if (template.getUploadDirName() != null) {
			props.put(UPLOAD_PROPERTY_NAME, template.getUploadDirName());
		}
		File templateFolder = template.getTemplateFolder();
		File templatePropsFile = template.getTemplatePropertiesFile();
		if (templatePropsFile == null) {
			final String templateFileName = template.getTemplateFile().getName();
			final int templateFileNamePrefixEndIndex = templateFileName.indexOf(".");
			final String templateFileNamePrefix = templateFileName.substring(0, templateFileNamePrefixEndIndex);
			final String proeprtiesFileName = templateFileNamePrefix + DSLUtils.PROPERTIES_FILE_SUFFIX;
			templatePropsFile = new File(templateFolder, proeprtiesFileName);
			
		} else {
			if (!templatePropsFile.exists()) {
				templatePropsFile = new File(templateFolder, templatePropsFile.getName());
			} else if (!templateFolder.equals(templatePropsFile.getParentFile())){
				try {
					FileUtils.copyFileToDirectory(templatePropsFile, templateFolder);
				} catch (IOException e) {
					Assert.fail("failed to copy properties file [" + templatePropsFile.getAbsolutePath() + "] to directory [" 
							+ templateFolder.getAbsolutePath() + "]. error was: " + e.getMessage());
				}
			}
		}
		try {
			IOUtils.writePropertiesToFile(props, templatePropsFile);
		} catch (IOException e) {
			Assert.fail("failed to write properties to file [" + templatePropsFile.getAbsolutePath());
		}
		return templatePropsFile;
	}
	
	public void replaceStringInFile(final File readFrom, final File writeTo, final String stringToReplace,
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
	
	public File getBasicTemplateFile() {
		return new File(TEMPLATES_ROOT_PATH, BASIC_TEMPLATE_FILE_NAME);
	}
	
	public File getBasicBootstrapManagementFile() {
		return new File(TEMPLATES_ROOT_PATH, BOOTSTRAP_MANAGEMENT_FILE_NAME);
	}
	
	public TemplateDetails createCustomTemplate(TemplateDetails template) {
		return createTemplate(template.getTemplateName(), template.getTemplateFile(), template.getTemplateFolder(), template.getUploadDirName());
	}
}
