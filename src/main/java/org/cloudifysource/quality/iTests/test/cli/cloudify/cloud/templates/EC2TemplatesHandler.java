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
import java.util.List;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.EC2TemplatesFolderHandler;

/**
 * 
 * @author yael
 *
 */
public class EC2TemplatesHandler extends TemplatesHandler {

	public EC2TemplatesHandler(List<String> defaultTemplates, String restUrl, File templatesTempFolder) {
		super(defaultTemplates, restUrl, templatesTempFolder);
	}

	@Override
	public EC2TemplatesFolderHandler createNewTemplatesFolderHandler() {
		File folder = createNewFolder();
		return new EC2TemplatesFolderHandler(folder);
	}

}
