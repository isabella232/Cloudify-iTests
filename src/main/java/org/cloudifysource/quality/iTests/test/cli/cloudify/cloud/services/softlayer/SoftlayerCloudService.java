/*
 * ******************************************************************************
 *  * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  ******************************************************************************
 */

package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.softlayer;

import java.io.IOException;
import java.util.Properties;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.JCloudsCloudService;

/**
 * Provides bootstrapping and teardown for softlayer cloud.
 *
 * @author Eli Polonsky
 * @since 3.0.0
 */
public class SoftlayerCloudService extends JCloudsCloudService {

    private static final String EC2_CERT_PROPERTIES = CREDENTIALS_FOLDER + "/cloud/softlayer/softlayer-cred"
            + ".properties";

    private final Properties certProperties = getCloudProperties(EC2_CERT_PROPERTIES);

    private String user = certProperties.getProperty("user");
    private String apiKey = certProperties.getProperty("apiKey");

    public SoftlayerCloudService(final String cloudName) {
        super(cloudName);
    }

    @Override
    public void addOverrides(final Properties overridesProps) {
    }

    @Override
    public void injectCloudAuthenticationDetails() throws IOException {

        // inject credentials
        getProperties().put(USER_PROP, user);
        getProperties().put(API_KEY_PROP, apiKey);

        // inject prefixes
        getAdditionalPropsToReplace().put("machineNamePrefix \"cloudify-agent-\"", "machineNamePrefix \"" + getMachinePrefix() + "-cloudify-agent-\"");
        getAdditionalPropsToReplace().put("managementGroup \"cloudify-manager\"", "managementGroup \"" + getMachinePrefix() + "-cloudify-manager\"");
        getAdditionalPropsToReplace().put("numberOfManagementMachines 1", "numberOfManagementMachines "
                + getNumberOfManagementMachines());

    }

    @Override
    public void setMachinePrefix(final String machinePrefix) {
        super.setMachinePrefix(machinePrefix);
        super.setMachinePrefix(super.getMachinePrefix().substring(0,20));
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }

    @Override
    public String getRegion() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

	@Override
	public boolean isComputeApiHelperSupported() {
		return false;
	}

	@Override
	public boolean isStorageApiHelperSupported() {
		return false;
	}

	@Override
	public boolean isNetworkApiHelperSupported() {
		return false;
	}
}
