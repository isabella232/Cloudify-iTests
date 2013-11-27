/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates;

import java.io.File;

public class EC2TemplateCreator extends TemplateCreator {

	private static final String EC2_BASIC_TEMPLATE_FILE_NAME = "ec2_basic_template";
	private static final String EC2_BOOTSTRAP_MANAGEMENT_FILE_NAME = "bootstrap-management-with-curl.sh";

	@Override
	public TemplateDetails getNewTempalte() {
		return new EC2TemplateDetails();
	}
	
	@Override
	public File getBasicTemplateFile() {
		return new File(TEMPLATES_ROOT_PATH, EC2_BASIC_TEMPLATE_FILE_NAME);
	}
	
	@Override
	public File getBasicBootstrapManagementFile() {
		return new File(TEMPLATES_ROOT_PATH, EC2_BOOTSTRAP_MANAGEMENT_FILE_NAME);
	}
	
	@Override
	public TemplateDetails createCustomTemplate(TemplateDetails template) {
		EC2TemplateDetails ec2Template =  (EC2TemplateDetails) template;
		EC2TemplateDetails createdTemplate = (EC2TemplateDetails) super.createCustomTemplate(ec2Template);
		createdTemplate.setHardwareId(ec2Template.getHardwareId());
		createdTemplate.setImageId(ec2Template.getImageId());
		createdTemplate.setKeyFile(ec2Template.getKeyFile());
		createdTemplate.setKeyPair(ec2Template.getKeyPair());
		createdTemplate.setLocationId(ec2Template.getLocationId());
		createdTemplate.setMachineMemoryMB(ec2Template.getMachineMemoryMB());
		createdTemplate.setExpectedToFailOnAdd(ec2Template.isExpectedToFailOnAdd());
		createdTemplate.setUsername(ec2Template.getUsername());
		createdTemplate.setRemoteDirectory(ec2Template.getRemoteDirectory());
		return createdTemplate;
	}

}
