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
package org.cloudifysource.quality.iTests.framework.utils.compute;

import com.google.common.base.Predicate;
import com.google.inject.Module;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.jclouds.JCloudsDeployer;
import org.codehaus.plexus.util.StringUtils;
import org.jclouds.compute.config.ComputeServiceProperties;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 4/11/13
 * Time: 9:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class JcloudsComputeApiHelper implements ComputeApiHelper {

    private JCloudsDeployer deployer;
    private String region;

    public JcloudsComputeApiHelper(final Cloud cloud, final String region) {
        try {
            this.deployer = new JCloudsDeployer(cloud.getProvider().getProvider(), cloud.getUser().getUser(),
                    cloud.getUser().getApiKey(), setupProperties(), new HashSet<Module>());
            this.region = region;
        } catch (final Exception e) {
            throw new RuntimeException("Failed to initialize compute helper : " + e.getMessage(), e);
        }
    }


    @Override
    public Set<MachineDetails> getServersContaining(final String machineNamePrefix) {

        Predicate<ComputeMetadata> predicate = new Predicate<ComputeMetadata>() {

            public boolean apply(final ComputeMetadata input) {
                boolean nameFound = false;
                final NodeMetadata node = (NodeMetadata) input;
                if (StringUtils.isNotBlank(node.getName())) {
                    nameFound = node.getName().contains(machineNamePrefix) && (node.getStatus() == NodeMetadata.Status.RUNNING);
                }
                return nameFound;
            }

        };

        Set<? extends NodeMetadata> servers = deployer.getServers(predicate);
        Set<MachineDetails> machineDetailsSet = new HashSet<MachineDetails>();
        for (NodeMetadata node: servers) {
            MachineDetails machineDetails = new MachineDetails();
            machineDetails.setPublicAddress(node.getPublicAddresses().iterator().next());
            machineDetailsSet.add(machineDetails);
        }
        return machineDetailsSet;
    }

    @Override
    public MachineDetails getServerById(String attachmentId) {
        NodeMetadata serverByID = deployer.getServerByID(region + "/" + attachmentId);
        MachineDetails machineDetails = new MachineDetails();
        machineDetails.setPublicAddress(serverByID.getPrivateAddresses().iterator().next());
        machineDetails.setPrivateAddress(serverByID.getPrivateAddresses().iterator().next());
        return machineDetails;
    }

    @Override
    public void shutdownServerById(String serverId) {
        deployer.shutdownMachine(region + "/" + serverId);
    }

    private Properties setupProperties() {
        Properties properties = new Properties();
        properties.setProperty(ComputeServiceProperties.POLL_INITIAL_PERIOD, "5000");
        properties.setProperty(ComputeServiceProperties.POLL_MAX_PERIOD, "20000");
        return properties;
    }
}
