package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack.examples;

import iTests.framework.utils.AssertUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenstackException;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;
import org.cloudifysource.esc.driver.provisioning.storage.VolumeDetails;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageAllocationTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * install a service with 4 instances on the HP cloud. 
 * The 4 agent machines started must start in different availability zones.
 * 
 * @author adaml, noaks
 *
 */
public class OpenstackMultiAZTest extends AbstractStorageAllocationTest {
	
	private static final String SERVICE_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/multi-az");
	private static final String SERVICE_NAME = "groovy";
	private static final String STORAGE_TEMPLATE_NAME = "SMALL_BLOCK";
	

	@Override
	protected String getCloudName() {
		return "hp-grizzly";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void init() throws Exception {
		
		// bootstrap and install a simple service with 4 instances and static storage
		super.bootstrap();
		installServiceAndWait(SERVICE_PATH, SERVICE_NAME);
	}
	
    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        
        // set a list of avalability zones to span
        Map<String, String> propsToReplace = this.cloudService.getAdditionalPropsToReplace();
        propsToReplace.put(Pattern.quote("// availabilityZones ([\"az1\",\"az2\",\"az3\"])"), 
        		"availabilityZones ([\"az1\",\"az2\",\"az3\"])");
    }
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testDistributedAvailabilityZones() throws IOException, InterruptedException, OpenstackException, CloudProvisioningException {
		//assert the 4 agent machines started in different availability zones
		assertServersAndVolumesAZ();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true, dependsOnMethods = { "testDistributedAvailabilityZones" })
    public void testFailoverLocationAware() throws Exception {
        storageAllocationTester.testFailoverLocationAware(SERVICE_NAME, STORAGE_TEMPLATE_NAME);
    }
	

	private void assertServersAndVolumesAZ() throws OpenstackException {		
		assertDistributedServers();	
		assertDistributedVolumes();
	}
	

	private void assertDistributedServers() throws OpenstackException {
		int az1Count = 0;
		int az2Count = 0;
		int az3Count = 0;
		
		final String agentMachinePrefix = this.cloudService.getCloud().getProvider().getMachineNamePrefix();
		Set<MachineDetails> servers;
		try {
			servers = computeApiHelper.getServersContaining(agentMachinePrefix);
		} catch (CloudProvisioningException e) {
			throw new OpenstackException(e);
		}
		
		for (MachineDetails machineDetails : servers) {
			
			final String availabilityZone = machineDetails.getLocationId();
			// this is an agent machine. get the server details.
			/*NovaServer serverDetails = null;
			try {
				serverDetails = computeClient.getServerDetails(machineDetails.getMachineId());
			} catch (final OpenstackException e) {
				throw new OpenstackException("Error requesting server details", e);
			}
			
			if (serverDetails == null) {
				throw new OpenstackException("Error requesting server details for server with ID: " 
						+ novaServer.getId());
			}
			
			final String availabilityZone = serverDetails.getAvailabilityZone();*/
			if (availabilityZone.equals("az1")) {
				az1Count = az1Count + 1;
			} else if (availabilityZone.equals("az2")) {
				az2Count = az2Count + 1;
			} else if (availabilityZone.equals("az3")) {
				az3Count = az3Count + 1;
			}

		}
		
		assertTrue("Expecting 2 machines in az1, found: " + az1Count, az1Count == 2);
		assertTrue("Expecting 1 machines in az2, found: " + az2Count, az2Count == 1);
		assertTrue("Expecting 1 machines in az3, found: " + az3Count, az3Count == 1);
	}
	

	private void assertDistributedVolumes() throws OpenstackException {
		Set<VolumeDetails> volumes = new HashSet<VolumeDetails>();
		try {
			String volumePrefix = cloudService.getCloud().getCloudStorage().getTemplates().get(STORAGE_TEMPLATE_NAME).getNamePrefix();
			volumes = storageApiHelper.getVolumesByPrefix(volumePrefix);
		} catch (StorageProvisioningException e) {
			throw new OpenstackException("Failed to get volumes by prefix: " + e.getMessage(), e);
		}
		
		for (VolumeDetails volume : volumes) {
			Set<String> volumeAttachments;
			String volumeId = volume.getId();
			try {
				volumeAttachments = storageApiHelper.getVolumeAttachments(volumeId);				
			} catch (StorageProvisioningException e) {
				throw new OpenstackException("Failed to get attachments for volume: " + volumeId 
						+ ", reported error: " + e.getMessage(), e);
			}
			
			AssertUtils.assertNotNull("Volume attachments not found for volume: " + volume.getName() 
					+ ", id: " + volumeId, volumeAttachments);
			AssertUtils.assertTrue("Expected 1 volume attachment for volume " + volume.getName() 
					+ ", but found " + volumeAttachments.size(), volumeAttachments.size() == 1);
			
			// check the attached instance
			String serverId = volumeAttachments.iterator().next();
			MachineDetails machineDetails;
			try {
				machineDetails = computeApiHelper.getServerById(serverId);
			} catch (CloudProvisioningException e) {
				throw new OpenstackException("Failed to get instance by id: " + serverId, e);
			}
			AssertUtils.assertNotNull("Failed to get instance by id: " + serverId, machineDetails);

			// check the attached zone
			String instanceLocation = machineDetails.getLocationId();
	        AssertUtils.assertNotNull("Failed to get location for instance id: " + serverId, instanceLocation);
			
	        // compare the volume and attached instance locations
			String volumeLocation = volume.getLocation();
			AssertUtils.assertTrue("Instance location is " + instanceLocation + ", while volume"
					+ " location is " + volumeLocation, instanceLocation.equalsIgnoreCase(volumeLocation));
		}
	}

	@AfterClass(alwaysRun = true)
	protected void clean() throws Exception {
		// uninstall the service, teardown and scan for leaking volumes
		uninstallServiceAndWait(SERVICE_NAME);
		super.teardown();
		super.scanForLeakedVolumesCreatedViaTemplate("SMALL_BLOCK");
	}

    
	@Override
	protected boolean isReusableCloud() {
		return false;
	}

}
