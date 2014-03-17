package org.cloudifysource.quality.iTests.framework.utils.storage;

import iTests.framework.utils.LogUtils;

import java.util.HashSet;
import java.util.Set;

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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

/**
 * User: elip, noak
 */
public class OpenstackStorageApiHelper extends JcloudsStorageApiHelper {
    private String region;
    private RestContext<NovaApi, NovaAsyncApi> novaContext;
    private ComputeServiceContext computeContext;

    public OpenstackStorageApiHelper(final Cloud cloud, final String computeTemplateName, final ComputeServiceContext computeContext) {
        super(cloud, computeTemplateName);
        String hardwareId = cloud.getCloudCompute().getTemplates().get(computeTemplateName).getHardwareId();
        this.region = StringUtils.substringBefore(hardwareId, "/");
        this.computeContext = computeContext;
        novaContext = this.computeContext.unwrap();
    }


    @Override
    public Set<VolumeDetails> getVolumesByPrefix(String prefix) throws StorageProvisioningException {
        Set<VolumeDetails> volumes = new HashSet<VolumeDetails>();

        Set<Volume> matchingVolumes = getActualVolumesByPrefix(prefix);
        LogUtils.log("Retrieved all volumes with prefix " + prefix);
        for (Volume volume : matchingVolumes) {
            String volumeName = volume.getName();
            LogUtils.log("Volume with id " + volume.getId() + " is named " + volumeName);
            VolumeDetails details = new VolumeDetails();
            details.setName(volumeName);
            details.setId(volume.getId());
            details.setLocation(volume.getZone());
            details.setSize(volume.getSize());
            volumes.add(details);
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
    public Set<String> getVolumeAttachments(final String volumeId) throws StorageProvisioningException {

        Set<String> _attachments = new HashSet<String>();
        Volume volume = getActualVolumeById(volumeId);
        
        if (volume != null) {
        	Set<VolumeAttachment> volAttachments = volume.getAttachments();
        	if (volAttachments != null && volAttachments.size() > 0) {
        		_attachments.add(volAttachments.iterator().next().getServerId());        		
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

    
    public Volume getActualVolumeById(final String volumeId) throws StorageProvisioningException {
    	Volume volume = null;
    	
        Optional<? extends VolumeApi> volumeApi = getVolumeApi();
        if (volumeApi == null || !volumeApi.isPresent()) {
            throw new StorageProvisioningException("Failed to get volumes API");
        }

        Optional<? extends Volume> optionalVolume = volumeApi.get().listInDetail().firstMatch(new Predicate<Volume>() {
			@Override
			public boolean apply(Volume volume) {
				return (volumeId.equalsIgnoreCase(volume.getId()));
			}
		});
        
        if (optionalVolume != null && optionalVolume.isPresent()) {
        	volume = optionalVolume.get();
        }
        
        return volume;
    }
    
    
    public Set<Volume> getActualVolumesByPrefix(final String volumePrefix) throws StorageProvisioningException {
    	Set<Volume> volumesSet = new HashSet<Volume>();
    	
        Optional<? extends VolumeApi> volumeApi = getVolumeApi();
        if (volumeApi == null || !volumeApi.isPresent()) {
            throw new StorageProvisioningException("Failed to get volumes API");
        }
        
        FluentIterable<? extends Volume> volumesIterable = volumeApi.get().listInDetail().filter(new Predicate<Volume>() {
			@Override
			public boolean apply(Volume volume) {
				return (volume.getName().startsWith(volumePrefix));
			}
		});
        
        if (volumesIterable != null) {
        	volumesSet.addAll(volumesIterable.toSet());
        }
        
        return volumesSet;
    }
}
