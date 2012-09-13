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
package test.cli.cloudify.cloud.services.byon;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import test.AbstractTest;
import test.cli.cloudify.cloud.services.AbstractCloudService;
import framework.utils.IOUtils;
import framework.utils.LogUtils;
import framework.utils.SSHUtils;

public class ByonCloudService extends AbstractCloudService {

	private static final String BYON_CLOUD_NAME = "byon";
	public static final String BYON_CLOUD_USER= "tgrid";
	public static final String BYON_CLOUD_PASSWORD = "tgrid";
	
	public static final String IP_LIST_PROPERTY = "ipList";
	
	private static final String DEFAULT_MACHINES = "192.168.9.115,192.168.9.116,192.168.9.120,192.168.9.125,192.168.9.126,192.168.9.135";
	
	private String ipList;
	private String[] machines;

	public ByonCloudService(String uniqueName) {
		super(uniqueName, BYON_CLOUD_NAME);
		this.ipList = System.getProperty(IP_LIST_PROPERTY, DEFAULT_MACHINES);
		this.machines = ipList.split(",");
	}
	
	public void setIpList(String ipList) {
		this.ipList = ipList;
	}
	
	public String getIpList() {
		return ipList;
	}
	
	public String[] getMachines() {
		return machines;
	}

	@Override
	public void injectServiceAuthenticationDetails() throws IOException {
	
		Map<String, String> propsToReplace = new HashMap<String,String>();
		propsToReplace.put("ENTER_USER", BYON_CLOUD_USER);
		propsToReplace.put("ENTER_PASSWORD", BYON_CLOUD_PASSWORD);
		propsToReplace.put("cloudify_agent_", this.machinePrefix + "cloudify-agent");
		propsToReplace.put("cloudify_manager", this.machinePrefix + "cloudify-manager");
		propsToReplace.put("// cloudifyUrl", "   cloudifyUrl");
		if (StringUtils.isNotBlank(ipList)) {
			propsToReplace.put("0.0.0.0", ipList);
		}
		
		propsToReplace.put("numberOfManagementMachines 1", "numberOfManagementMachines "  + numberOfManagementMachines);
		IOUtils.replaceTextInFile(getPathToCloudGroovy(), propsToReplace);
	}

	@Override
	public void beforeBootstrap() throws Exception {
		killAllJavaOnAllHosts();
		cleanGSFilesOnAllHosts();
		printBootstrapManagementFile();
	}
	
	private void printBootstrapManagementFile() throws IOException {
		String pathToBootstrap = getPathToCloudFolder() + "/upload/bootstrap-management.sh";
		File bootstrapFile = new File(pathToBootstrap);
		if (!bootstrapFile.exists()) {
			LogUtils.log("Failed to print the cloud configuration file content");
			return;
		}
		String cloudConfigFileAsString = FileUtils.readFileToString(bootstrapFile);
		LogUtils.log("Bootstrap-management file: " + bootstrapFile.getAbsolutePath());
		LogUtils.log(cloudConfigFileAsString);
		
	}

	private void cleanGSFilesOnAllHosts() {
		String command = "rm -rf /tmp/gs-files";
		String[] hosts = this.getMachines();
		for (String host : hosts) {
			try {
				LogUtils.log(SSHUtils.runCommand(host, AbstractTest.OPERATION_TIMEOUT, command, "tgrid", "tgrid"));
			} catch (AssertionError e) {
				LogUtils.log("Failed to clean gs-files on host " + host + " .Reason --> " + e.getMessage());
			}
		}	
	}
	
	private void killAllJavaOnAllHosts() {
		String command = "killall -9 java";
		String[] hosts = this.getMachines();
		for (String host : hosts) {
			try {
				LogUtils.log(SSHUtils.runCommand(host, AbstractTest.OPERATION_TIMEOUT, command, "tgrid", "tgrid"));
			} catch (AssertionError e) {
				LogUtils.log("Failed to clean gs-files on host " + host + " .Reason --> " + e.getMessage());
			}
		}
	}

	@Override
	public String getUser() {
		return BYON_CLOUD_USER;
	}

	@Override
	public String getApiKey() {
		return BYON_CLOUD_PASSWORD;
	}

}