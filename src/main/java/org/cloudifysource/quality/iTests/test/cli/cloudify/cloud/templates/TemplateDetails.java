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

public class TemplateDetails {
	private String templateName;
	private File templateFile;
	private File templateFolder;
	private File templatePropertiesFile;
	private String uploadDirName;
	private String machineIP;
	private boolean isForServiceInstallation;
	private boolean expectedToFail;

	public final String getTemplateName() {
		return templateName;
	}

	public final void setTemplateName(final String templateName) {
		this.templateName = templateName;
	}

	public final File getTemplateFile() {
		return templateFile;
	}

	public final void setTemplateFile(final File templateFile) {
		this.templateFile = templateFile;
	}

	public final File getTemplateFolder() {
		return templateFolder;
	}

	public final void setTemplateFolder(final File templateFolder) {
		this.templateFolder = templateFolder;
	}

	public final File getTemplatePropertiesFile() {
		return templatePropertiesFile;
	}

	public final void setTemplatePropertiesFile(final File templatePropertiesFile) {
		this.templatePropertiesFile = templatePropertiesFile;
	}

	public final String getUploadDirName() {
		return uploadDirName;
	}

	public final void setUploadDirName(final String uploadDirName) {
		this.uploadDirName = uploadDirName;
	}

	public final String getMachineIP() {
		return machineIP;
	}

	public final void setMachineIP(final String machineIP) {
		this.machineIP = machineIP;
	}

	public final boolean isForServiceInstallation() {
		return isForServiceInstallation;
	}

	public final void setForServiceInstallation(final boolean isForServiceInstallation) {
		this.isForServiceInstallation = isForServiceInstallation;
	}

	public final boolean isExpectedToFail() {
		return expectedToFail;
	}

	public final void setExpectedToFail(final boolean expectedToFail) {
		this.expectedToFail = expectedToFail;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof TemplateDetails)) {
			return false;
		}
		final TemplateDetails templateDetails = (TemplateDetails) obj;
		return this.templateName.equals(templateDetails.getTemplateName());
	}
}
