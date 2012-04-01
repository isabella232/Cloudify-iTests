/*******************************************************************************
* Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
******************************************************************************/
package test.cli.cloudify.cloud.byon;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import test.cli.cloudify.cloud.AbstractCloudService;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class ByonCloudService extends AbstractCloudService {

	private static final String cloudName = "byon";
	private static final String BYON_CLOUD_USER= "tgrid";
	private static final String BYON_CLOUD_PASSWORD = "tgrid";
	private static final String BYON_SERVER_USER= "tgrid";
	private static final String BYON_SERVER_PASSWORD = "tgrid";
	private static final String SYS_PROP_IP_LIST = "ipList";

	private static ByonCloudService self = null;

	private ByonCloudService() {};

	public static ByonCloudService getService() {
		if (self == null) {
			self = new ByonCloudService();
		}
		return self;	
	}

	@Override
	public void injectAuthenticationDetails() throws IOException {

		// cloud plugin should include recipe that includes secret key 
		File cloudPluginDir = new File(ScriptUtils.getBuildPath() , "tools/cli/plugins/esc/" + cloudName + "/");
		File originalCloudDslFile = new File(cloudPluginDir, cloudName + "-cloud.groovy");
		File backupCloudDslFile = new File(cloudPluginDir, cloudName + "-cloud.backup");

		// Read file contents
		final String originalDslFileContents = FileUtils.readFileToString(originalCloudDslFile);

		// first make a backup of the original file
		FileUtils.copyFile(originalCloudDslFile, backupCloudDslFile);
		
		LogUtils.log("injecting credentials");
		LogUtils.log("Machines are " + System.getProperty(SYS_PROP_IP_LIST));

		//replace credentials and replace the ipList default 0.0.0.0 with values that are set through a system property
		final String modifiedDslFileContents = originalDslFileContents.replace("ENTER_CLOUD_USER", BYON_CLOUD_USER).
				replace("ENTER_CLOUD_PASSWORD", BYON_CLOUD_PASSWORD).replace("ENTER_SERVER_USER", BYON_SERVER_USER).
				replace("ENTER_SERVER_PASSWORD", BYON_SERVER_PASSWORD).
				replace("0.0.0.0", System.getProperty(SYS_PROP_IP_LIST));
		
		FileUtils.write(originalCloudDslFile, modifiedDslFileContents);
	}

	@Override
	public String getCloudName() {
		return cloudName;
	}

}