package org.cloudifysource.quality.iTests.framework.utils.storage;

import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;
import org.cloudifysource.esc.driver.provisioning.storage.VolumeDetails;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 4/10/13
 * Time: 6:11 PM
 * To change this template use File | Settings | File Templates.
 */
public interface StorageApiHelper {

    VolumeDetails createVolume(String templateName, String location) throws TimeoutException, StorageProvisioningException;

    void attachVolume(String volumeId, String device, String ip) throws TimeoutException, StorageProvisioningException;

    void detachVolume(String volumeId, String ip) throws TimeoutException, StorageProvisioningException;

    void deleteVolume(String volumeId) throws TimeoutException, StorageProvisioningException;

    Set<VolumeDetails> getVolumesByPrefix(final String prefix) throws StorageProvisioningException;

    VolumeDetails getVolumeById(final String volumeId) throws StorageProvisioningException;

    boolean isVolumeDeleting(final String volumeId);

    boolean isVolumeAvailable(final String volumeId);

    String getVolumeStatus(final String volumeId);

    Set<String> getVolumeAttachments(final String volumeId);

    String getVolumeName(final String volumeId) throws StorageProvisioningException;
}
