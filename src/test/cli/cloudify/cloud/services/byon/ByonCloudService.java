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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import test.cli.cloudify.cloud.services.AbstractCloudService;
import framework.utils.IOUtils;

public class ByonCloudService extends AbstractCloudService {

	private static final String BYON_CLOUD_NAME = "byon";
	private static final String BYON_CLOUD_USER= "tgrid";
	private static final String BYON_CLOUD_PASSWORD = "tgrid";
	
	private String ipList;

	public ByonCloudService(String uniqueName) {
		super(uniqueName, BYON_CLOUD_NAME);
	}
	
	public ByonCloudService(String uniqueName, Map<String,String> additionalPropsToReplace) {
		super(uniqueName, BYON_CLOUD_NAME);
		this.additionalPropsToReplace = additionalPropsToReplace;
	}
	
	public void setIpList(String ipList) {
		this.ipList = ipList;
	}
	
	public String getIpList(String ipList) {
		return ipList;
	}

	@Override
	public void injectServiceAuthenticationDetails() throws IOException {
	
		Map<String, String> propsToReplace = new HashMap<String,String>();
		propsToReplace.put("ENTER_USER", BYON_CLOUD_USER);
		propsToReplace.put("ENTER_PASSWORD", BYON_CLOUD_PASSWORD);
		propsToReplace.put("cloudify_agent_", this.machinePrefix + "cloudify-agent");
		propsToReplace.put("cloudify_manager", this.machinePrefix + "cloudify-manager");
		propsToReplace.put("// cloudifyUrl", "   cloudifyUrl");
		if (ipList == null) {
			 ipList = System.getProperty("ipList");
		}
		if (StringUtils.isNotBlank(ipList)) {
			propsToReplace.put("0.0.0.0", ipList);
		}
		
		propsToReplace.put("numberOfManagementMachines 1", "numberOfManagementMachines "  + numberOfManagementMachines);
		IOUtils.replaceTextInFile(getPathToCloudGroovy(), propsToReplace);
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