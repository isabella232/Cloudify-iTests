package org.cloudifysource.quality.iTests.framework.utils.compute;

import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: elip, noak
 */
public interface ComputeApiHelper {

    Set<MachineDetails> getServersContaining(String partialName) throws CloudProvisioningException;

    MachineDetails getServerById(final String serverId) throws CloudProvisioningException;

    void shutdownServerById(final String serverId) throws CloudProvisioningException;
}
