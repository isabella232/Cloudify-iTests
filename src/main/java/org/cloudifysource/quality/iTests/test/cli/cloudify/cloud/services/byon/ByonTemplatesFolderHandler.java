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
package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.byon;

import iTests.framework.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.ByonTemplateCreator;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplateDetails;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplatesFolderHandler;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplateCreator;

public class ByonTemplatesFolderHandler extends TemplatesFolderHandler {
	private final String TEMPLATES_ROOT_PATH = CommandTestUtils.getPath("src/main/resources/templates");
	private static final String BYON_BASIC_TEMPLATE_FILE_NAME = "byon_basic_template";
	private final String TEMPLATE_NAME_STRING = "TEMPLATE_NAME";

	private static final String NODE_IP_PROPERTY_NAME = "node_ip";
	private static final String NODE_ID_PROPERTY_NAME = "node_id";

	private final AtomicInteger numOfMachinesInUse;
	private final String[] machines;

	public ByonTemplatesFolderHandler(final File folder, final int numOfMngMachines, final String[] machines) {
		super(folder);
		numOfMachinesInUse = new AtomicInteger(numOfMngMachines);
		this.machines = machines;
	}

	@Override
	public void updatePropertiesFile(final TemplateDetails template) {
		final Properties props = new Properties();
		
		
		String machineIP = template.getMachineIP();
		if (machineIP == null) {
			machineIP = getNextMachineIP(template.isForServiceInstallation());
			template.setMachineIP(machineIP);
		}
		props.put(NODE_IP_PROPERTY_NAME, machineIP);
		props.put(NODE_ID_PROPERTY_NAME, "byon-pc-lab-" + template.getMachineIP() + "{0}");
		File templatePropertiesFile = template.getTemplatePropertiesFile();
		
		try {
			IOUtils.writePropertiesToFile(props, templatePropertiesFile);
		} catch (IOException e) {
			Assert.fail("failed to write properties to file [" + templatePropertiesFile.getAbsolutePath());
		}
	}

	private String getNextMachineIP(final boolean forServiceInstallation) {
		if (forServiceInstallation) {
			final int nextMachine = numOfMachinesInUse.getAndIncrement();
			if (machines.length <= nextMachine) {
				Assert.fail("Cannot allocate machine number " + nextMachine + ", there are only " + machines.length
						+ " machines to use.");
			}
			return machines[nextMachine];
		}
		return machines[0];
	}

	@Override
	public TemplateCreator getTempalteCreator() {
		return new ByonTemplateCreator();
	}

}
