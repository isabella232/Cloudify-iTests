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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import test.cli.cloudify.cloud.AbstractCloudService;
import framework.utils.IOUtils;
import framework.utils.ScriptUtils;

public class ByonCloudService extends AbstractCloudService {

	private static final String cloudName = "byon";
	private static final String BYON_CLOUD_USER= "tgrid";
	private static final String BYON_CLOUD_PASSWORD = "tgrid";
	private static final String BYON_SERVER_USER= "tgrid";
	private static final String BYON_SERVER_PASSWORD = "tgrid";
	
	private String ipList = System.getProperty("ipList");

	public ByonCloudService(){
		
	}
	
	public ByonCloudService(Map<String,String> additionalPropsToReplace) {
		this.additionalPropsToReplace = additionalPropsToReplace;
	}
	
	public void setIpList(String ipList) {
		this.ipList = ipList;
	}
	
	public String getIpList(String ipList) {
		return ipList;
	}



	@Override
	public void injectAuthenticationDetails() throws IOException {

		// cloud plugin should include recipe that includes secret key 
		File cloudPluginDir = new File(ScriptUtils.getBuildPath() , "tools/cli/plugins/esc/" + cloudName + "/");
		File originalCloudDslFile = new File(cloudPluginDir, cloudName + "-cloud.groovy");
		File backupCloudDslFile = new File(cloudPluginDir, cloudName + "-cloud.backup");

		// first make a backup of the original file
		FileUtils.copyFile(originalCloudDslFile, backupCloudDslFile);
		
		Map<String, String> propsToReplace = new HashMap<String,String>();
		propsToReplace.put("ENTER_CLOUD_USER", BYON_CLOUD_USER);
		propsToReplace.put("ENTER_CLOUD_PASSWORD", BYON_CLOUD_PASSWORD);
		propsToReplace.put("ENTER_SERVER_USER", BYON_SERVER_USER);
		propsToReplace.put("ENTER_SERVER_PASSWORD", BYON_SERVER_PASSWORD);
		propsToReplace.put("0.0.0.0", ipList);
		propsToReplace.put("numberOfManagementMachines", Integer.toString(numberOfManagementMachines));
		
		IOUtils.replaceTextInFile(originalCloudDslFile.getAbsolutePath(), propsToReplace);
		
	}

	@Override
	public String getCloudName() {
		return cloudName;
	}

}