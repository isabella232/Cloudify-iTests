/*******************************************************************************
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
 *******************************************************************************/
package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2;

import iTests.framework.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import junit.framework.Assert;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.EC2TemplateCreator;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.EC2TemplateDetails;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplateCreator;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplateDetails;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplatesFolderHandler;

public class EC2TemplatesFolderHandler extends TemplatesFolderHandler {
	
	// properties
	private static final String IMAGE_ID_PROPERTY_NAME = "imageId";
	private static final String LOCATION_ID_PROPERTY_NAME = "locationId";
	private static final String HARDWARE_ID_PROPERTY_NAME = "hardwareId";
	private static final String MACHINE_MEMORY_MB = "machineMemoryMB";
	private static final String KEY_FILE_PROPERTY_NAME = "keyFile";
	private static final String KEY_PAIR_PROPERTY_NAME = "keyPair";
	private static final String REMOTE_DIRECTORY_PROPERTY_NAME = "remoteDirectory";
	private static final String USERNAME_PROPERTY_NAME = "username";


	public EC2TemplatesFolderHandler(final File folder) {
		super(folder);
	}

	@Override
	public void updatePropertiesFile(final TemplateDetails template) {
		final EC2TemplateDetails ec2Template = (EC2TemplateDetails) template;
		final Properties props = new Properties();
		props.put(MACHINE_MEMORY_MB, ec2Template.getMachineMemoryMB());
		props.put(IMAGE_ID_PROPERTY_NAME, ec2Template.getImageId());
		props.put(LOCATION_ID_PROPERTY_NAME, ec2Template.getLocationId());
		props.put(HARDWARE_ID_PROPERTY_NAME, ec2Template.getHardwareId());
		props.put(KEY_FILE_PROPERTY_NAME, ec2Template.getKeyFile());
		props.put(KEY_PAIR_PROPERTY_NAME, ec2Template.getKeyPair());
		props.put(REMOTE_DIRECTORY_PROPERTY_NAME, ec2Template.getRemoteDirectory());
		props.put(USERNAME_PROPERTY_NAME, ec2Template.getUsername());

		final File templatePropertiesFile = ec2Template.getTemplatePropertiesFile();
		try {
			IOUtils.writePropertiesToFile(props, templatePropertiesFile);
		} catch (final IOException e) {
			Assert.fail("failed to write properties to file [" + templatePropertiesFile.getAbsolutePath());
		}
	}

	@Override
	public TemplateCreator getTempalteCreator() {
		return new EC2TemplateCreator();
	}

}
