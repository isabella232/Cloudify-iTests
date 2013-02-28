package org.cloudifysource.quality.iTests.framework.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.ec2.EC2ApiMetadata;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.domain.Tag;
import org.jclouds.ec2.domain.Volume;
import org.jclouds.ec2.features.TagApi;
import org.jclouds.ec2.options.DetachVolumeOptions;
import org.jclouds.ec2.services.ElasticBlockStoreClient;
import org.jclouds.ec2.util.TagFilterBuilder;
import org.jclouds.rest.ResourceNotFoundException;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

public class Ec2StorageApiForRegionHelper {
	
	private String region;
	private ComputeServiceContext context;
	private TagApi tagApi;
	private ElasticBlockStoreClient client;
	
	public Ec2StorageApiForRegionHelper(final String region, final ComputeServiceContext computeContext) {
		this.region = region;
		this.context = computeContext;
		this.tagApi = getTagApi();
		this.client = EC2Client.class.cast(context.unwrap(EC2ApiMetadata.CONTEXT_TOKEN).getApi()).getElasticBlockStoreServices();

	}
	
	public String getVolumeName(Volume vol) {
		FluentIterable<Tag> filter = tagApi.filter(new TagFilterBuilder().resourceId(vol.getId()).build());
		ImmutableList<Tag> immutableList = filter.toImmutableList();
		for (Tag tag : immutableList) {
			if (tag.getKey().equals("Name")) {
				return tag.getValue().get();
			}
		}
		return null;
	}
	
	public void detachVolume(final String volumeId) {
		client.detachVolumeInRegion(region, volumeId, true,(DetachVolumeOptions[]) null);
	}
	
	public Volume createVolume(final String availabilityZone, final int size, final String name) {
		Volume vol = client.createVolumeInAvailabilityZone(availabilityZone, size);
		
		Map<String, String> tags = new HashMap<String, String>();
		tags.put("Name", name);
		tagApi.applyToResources(tags, Arrays.asList(vol.getId()));
		
		return vol;
	}
	
	public Set<Volume> getAllVolumes() {
		return client.describeVolumesInRegion(this.region, (String[])null);	
	}
	
	public Volume getVolumeByName(final String volumeName) {
		Set<Volume> allVolumes = getAllVolumes();
		// we should be able to find our volume
		Volume ourVolume = null;
		for (Volume vol : allVolumes) {
			if (vol.getId() != null) {
				String volName = getVolumeName(vol);
				if (!StringUtils.isBlank(volName) &&  volName.toLowerCase().contains(volumeName)) {
					ourVolume = vol;
					break;
				}
			}
		}
		return ourVolume;
	}
	
	public Volume getVolume(final String volumeId) {
		Set<Volume> volumes;
		try {
			volumes = client.describeVolumesInRegion(region, volumeId);
		} catch (final ResourceNotFoundException e) {
			return null;
		}
		if (volumes == null || volumes.isEmpty()) {
			return null;
		}
		return volumes.iterator().next();
	}
	
	public void deleteVolume(final String volumeId) {
		client.deleteVolumeInRegion(region, volumeId);
	}
	
	private TagApi getTagApi() {
		if (this.tagApi == null) {
			this.tagApi = EC2Client.class.cast(this.context.unwrap(EC2ApiMetadata.CONTEXT_TOKEN)
					.getApi()).getTagApiForRegion(this.region).get();
		} 
		return this.tagApi;
	}
}
