package org.cloudifysource.quality.iTests.framework.utils.storage;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.storage.VolumeDetails;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 4/10/13
 * Time: 6:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class OpenstackStorageApiHelper extends JcloudsStorageApiHelper {

    protected OpenstackStorageApiHelper(final Cloud cloud,
                                        final String templateName) {
        super(cloud, templateName);
        throw new UnsupportedOperationException("Openstack Storage API is currently not supported");
    }

    @Override
    public Set<VolumeDetails> getVolumesByPrefix(String prefix) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public VolumeDetails getVolumeById(String volumeId) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isVolumeDeleting(String volumeId) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isVolumeAvailable(String volumeId) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getVolumeStatus(String volumeId) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<String> getVolumeAttachments(String volumeId) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
