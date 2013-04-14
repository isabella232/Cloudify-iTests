package org.cloudifysource.quality.iTests.framework.utils.compute;

import org.cloudifysource.esc.driver.provisioning.MachineDetails;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 4/10/13
 * Time: 6:35 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ComputeApiHelper {

    Set<MachineDetails> getServersByPrefix(String machineNamePrefix);

    MachineDetails getServerByAttachmentId(final String serverId);
}
