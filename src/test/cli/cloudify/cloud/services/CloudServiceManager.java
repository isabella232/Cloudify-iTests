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
package test.cli.cloudify.cloud.services;

import test.cli.cloudify.cloud.services.azure.MicrosoftAzureCloudService;
import test.cli.cloudify.cloud.services.byon.ByonCloudService;
import test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import test.cli.cloudify.cloud.services.ec2.Ec2WinCloudService;
import test.cli.cloudify.cloud.services.hp.HpCloudService;
import test.cli.cloudify.cloud.services.rackspace.RackspaceCloudService;

public class CloudServiceManager {

	private static CloudServiceManager instance = null;
	
	private CloudServiceManager() {
		// Exists only to defeat instantiation.
	}

	public static CloudServiceManager getInstance() {
		if (instance == null) {
			instance = new CloudServiceManager();
		}
		return instance;
	}

	public CloudService getCloudService(String cloudName) {
		return createCloudService(cloudName);
	}

	private CloudService createCloudService(String cloudName) {
		CloudService cloudService = null;

		if ("byon".equalsIgnoreCase(cloudName)) {
			cloudService = new ByonCloudService();
		} else if ("byon-xap".equalsIgnoreCase(cloudName)) {
			cloudService = new ByonCloudService();
			((ByonCloudService)cloudService).setNoWebServices(true);
		} else if ("ec2".equalsIgnoreCase(cloudName)) {
			cloudService = new Ec2CloudService();
		} else if ("ec2-win".equalsIgnoreCase(cloudName)) {
			cloudService = new Ec2WinCloudService();
		} else if ("hp".equalsIgnoreCase(cloudName)) {
			cloudService = new HpCloudService();
		} else if ("rackspace".equalsIgnoreCase(cloudName)) {
			cloudService = new RackspaceCloudService();
		} else if ("azure".equalsIgnoreCase(cloudName)) {
			cloudService = new MicrosoftAzureCloudService();
		}

		return cloudService;
	}
}
