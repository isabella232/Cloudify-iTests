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

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;

public class SimpleServiceCreator {
	
	public static final String UPLOAD_ENV_NAME =  "UPLOAD_NAME";
	public static final String SERVICES_ROOT_PATH = CommandTestUtils.getPath("src/main/resources/templates/services");
	public static final String SERVICE_NAME_PROPERTY_NAME = "serviceName";

	private static final String TEMP_SERVICES_DIR_PATH = SERVICES_ROOT_PATH + File.separator + "services.tmp";
	private static final String SERVICE_FILE_NAME = "basic_service"; 
	private static final String TEMPLATE_NAME_PROPERTY_NAME = "templateName";

	private File servicesTempFolder;
	
	public File createServiceDir(String serviceName, String templateName) throws IOException {
		servicesTempFolder = new File(TEMP_SERVICES_DIR_PATH);
		if (!servicesTempFolder.exists()) {
			servicesTempFolder.mkdir();
		}
		File serviceFolder = new File(servicesTempFolder,  serviceName);
		serviceFolder.mkdir();
		File serviceFile = new File(SERVICES_ROOT_PATH, SERVICE_FILE_NAME);
		File tempServiceFile = new File(serviceFolder, serviceName + DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX);
		FileUtils.copyFile(serviceFile, tempServiceFile);

		Properties props = new Properties();
		props.put(SERVICE_NAME_PROPERTY_NAME, serviceName);
		props.put(TEMPLATE_NAME_PROPERTY_NAME, templateName);
		String proeprtiesFileName = serviceName + "-service" + DSLUtils.PROPERTIES_FILE_SUFFIX;
		File servicePropsFile = new File(serviceFolder, proeprtiesFileName);
		IOUtils.writePropertiesToFile(props, servicePropsFile);

		return serviceFolder;
	}
	
	public void clean() throws IOException {
		if (servicesTempFolder != null) {
			FileUtils.deleteDirectory(servicesTempFolder);
		}
	}
}
