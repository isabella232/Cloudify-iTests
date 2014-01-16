package org.cloudifysource.quality.iTests.framework.utils.storage;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Optional;
import iTests.framework.utils.LogUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;
import org.cloudifysource.esc.driver.provisioning.storage.VolumeDetails;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.domain.Volume;
import org.jclouds.openstack.nova.v2_0.domain.VolumeAttachment;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.rest.RestContext;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 4/10/13
 * Time: 6:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class OpenstackStorageApiHelper extends JcloudsStorageApiHelper {
    private String region;
    private RestContext<NovaApi, NovaAsyncApi> novaContext;
    private ComputeServiceContext computeContext;

    public OpenstackStorageApiHelper(final Cloud cloud, final String templateName, final ComputeServiceContext computeContext) {
        super(cloud, templateName);
        String hardwareId = cloud.getCloudCompute().getTemplates().get(templateName).getHardwareId();
        this.region = StringUtils.substringBefore(hardwareId, "/");
        this.computeContext = computeContext;
        novaContext = this.computeContext.unwrap();
    }


    @Override
    public Set<VolumeDetails> getVolumesByPrefix(String prefix) throws StorageProvisioningException {
        Set<VolumeDetails> volumes = new HashSet<VolumeDetails>();

        Set<Volume> allVolumes = getAllVolumes();
        LogUtils.log("Retrieved all volumes in region " + region + " : " + allVolumes);
        for (Volume vol : allVolumes) {
            if (vol.getId() != null) {
                String volName = getVolumeName(vol.getId());
                LogUtils.log("Volume with id " + vol.getId() + " is named " + volName);
                if (!StringUtils.isBlank(volName) &&  volName.toLowerCase().contains(prefix)) {
                    VolumeDetails details = new VolumeDetails();
                    details.setName(volName);
                    details.setId(vol.getId());
                    volumes.add(details);
                }
            }
        }
        return volumes;
    }

    @Override
    public VolumeDetails getVolumeById(String volumeId) throws StorageProvisioningException {
        final VolumeDetails volumeDetails = new VolumeDetails();
        Volume volume = getActualVolumeById(volumeId);

        volumeDetails.setId(volume.getId());
        volumeDetails.setName(volume.getName());
        volumeDetails.setSize(volume.getSize());
        volumeDetails.setLocation(volume.getZone());

        return volumeDetails;
    }

    @Override
    public boolean isVolumeDeleting(String volumeId) {
        return false;
    }

    @Override
    public boolean isVolumeAvailable(String volumeId) throws StorageProvisioningException {
        return "available".equalsIgnoreCase(getVolumeStatus(volumeId));
    }

    @Override
    public boolean isVolumeCreating(String volumeId) {
        return false;
    }

    @Override
    public String getVolumeStatus(String volumeId) throws StorageProvisioningException {
        return getActualVolumeById(volumeId).getStatus().value();
    }

    @Override
    public Set<String> getVolumeAttachments(String volumeId) {
        Set<String> _attachments = new HashSet<String>();
        Set<VolumeAttachment> attachments = getVolumeApi().get().listInDetail().iterator().next().getAttachments();
        for (VolumeAttachment volumeAttachment : attachments) {
            if(volumeAttachment.getVolumeId().equalsIgnoreCase(volumeId)){
                _attachments.add(volumeAttachment.getId());
            }
        }
        return _attachments;
    }

    public Set<Volume> getAllVolumes() throws StorageProvisioningException {
        Optional<? extends VolumeApi> volumeApi = getVolumeApi();
        if (!volumeApi.isPresent()) {
            throw new StorageProvisioningException("Failed to list all volumes.");
        }

        return (Set<Volume>) volumeApi.get().list().toSet();
    }

    private Optional<? extends VolumeApi> getVolumeApi() {
        if (novaContext == null) {
            throw new IllegalStateException("Nova context is null");
        }
        return novaContext.getApi().getVolumeExtensionForZone(region);
    }

    private Optional<? extends VolumeAttachmentApi> getAttachmentApi() {
        if (novaContext == null) {
            throw new IllegalStateException("Nova context is null");
        }

        return novaContext.getApi().getVolumeAttachmentExtensionForZone(region);
    }

    public Volume getActualVolumeById(String volumeId) throws StorageProvisioningException {
        Optional<? extends VolumeApi> volumeApi = getVolumeApi();
        if (!volumeApi.isPresent()) {
            throw new StorageProvisioningException("Failed to list all volumes.");
        }

        Volume volume = volumeApi.get().get(volumeId);
        return volume;
    }
}
