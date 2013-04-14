package org.cloudifysource.quality.iTests.framework.utils.storage;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.esc.driver.provisioning.storage.BaseStorageDriver;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;
import org.cloudifysource.esc.driver.provisioning.storage.VolumeDetails;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 4/10/13
 * Time: 5:43 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class JcloudsStorageApiHelper implements StorageApiHelper {

    private static final long DEFAULT_STORAGE_OPERATION_TIMEOUT = 1000 * 60; // one minute

    StorageProvisioningDriver driver;

    protected JcloudsStorageApiHelper(final Cloud cloud, final String templateName) {
        try {
            driver = (StorageProvisioningDriver) Class.forName(cloud.getConfiguration().getStorageClassName()).newInstance();
            driver.setConfig(cloud, templateName);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to initialize storage api : " + e.getMessage() , e);
        }
    }

    @Override
    public VolumeDetails createVolume(String templateName, String location) throws TimeoutException, StorageProvisioningException {
        return driver.createVolume(templateName, location, DEFAULT_STORAGE_OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Override
    public void attachVolume(String volumeId, String device, String ip) throws TimeoutException, StorageProvisioningException {
        driver.attachVolume(volumeId, device, ip, DEFAULT_STORAGE_OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Override
    public void detachVolume(String volumeId, String ip) throws TimeoutException, StorageProvisioningException {
        driver.detachVolume(volumeId, ip, DEFAULT_STORAGE_OPERATION_TIMEOUT * 2, TimeUnit.MILLISECONDS);
    }

    @Override
    public void deleteVolume(String volumeId) throws TimeoutException, StorageProvisioningException {
        driver.deleteVolume(null, volumeId, DEFAULT_STORAGE_OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Override
    public String getVolumeName(String volumeId) throws StorageProvisioningException {
        return driver.getVolumeName(volumeId);
    }
}
