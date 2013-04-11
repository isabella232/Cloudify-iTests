package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud;

import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;
import org.cloudifysource.esc.driver.provisioning.storage.VolumeDetails;
import org.cloudifysource.quality.iTests.framework.utils.*;
import org.cloudifysource.quality.iTests.framework.utils.compute.*;
import org.cloudifysource.quality.iTests.framework.utils.compute.JcloudsComputeApiHelper;
import org.cloudifysource.quality.iTests.framework.utils.storage.Ec2StorageApiHelper;
import org.cloudifysource.quality.iTests.framework.utils.storage.StorageAllocationTester;
import org.cloudifysource.quality.iTests.framework.utils.storage.StorageApiHelper;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudService;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 4/10/13
 * Time: 5:42 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractStorageAllocationTest extends NewAbstractCloudTest {

    private static final long VOLUME_WAIT_TIMEOUT = 3 * 60 * 1000;

    protected StorageApiHelper storageApiHelper;
    protected ComputeApiHelper computeApiHelper;
    protected StorageAllocationTester storageAllocationTester;

    @Override
    protected void bootstrap(CloudService service, CloudBootstrapper bootstrapper) throws Exception {
        super.bootstrap(service, bootstrapper);
        storageApiHelper = initStorageHelper();
        computeApiHelper = initComputeHelper();
        storageAllocationTester = new StorageAllocationTester(getRestUrl(), storageApiHelper, getService(), computeApiHelper);
    }

    private ComputeApiHelper initComputeHelper() {
        if (getCloudName().equals("ec2") || getCloudName().equals("hp") || getCloudName().equals("rackspace")) {
            return new JcloudsComputeApiHelper(getService().getCloud());
        }
        throw new UnsupportedOperationException("Cannot init compute helper for non jclouds providers!");
    }


    private StorageApiHelper initStorageHelper() {
        if (getCloudName().equals("ec2")) {
            return new Ec2StorageApiHelper(getService().getCloud(),
                    "SMALL_LINUX",
                    ((Ec2CloudService)getService()).getRegion(),
                    ((Ec2CloudService)getService()).getComputeServiceContext());
        }
        throw new UnsupportedOperationException("Cannot init storage helper for clouds that are not ec2");
    }

    public void scanForLeakedVolumes(final String name) throws TimeoutException, StorageProvisioningException {

        LogUtils.log("Scanning for leaking volumes with name " + name);
        Set<VolumeDetails> volumesByName = storageApiHelper.getVolumesByPrefix(name);
        Set<VolumeDetails> nonDeletingVolumes = new HashSet<VolumeDetails>();

        for (VolumeDetails volumeByName : volumesByName) {

            final String volumeId = volumeByName.getId();
            final String volumeStatus = storageApiHelper.getVolumeStatus(volumeId);
            final Set<String> volumeAttachments = storageApiHelper.getVolumeAttachments(volumeId);

            if (storageApiHelper.isVolumeDeleting(volumeByName.getId())) {
                LogUtils.log("Found a volume with id " + volumeId + " and status " + volumeStatus
                        + ". this is ok. this volume is being deleted");
            } else {
                LogUtils.log("Found a leaking volume " + volumeByName + ". status is " + volumeStatus);
                if (volumeAttachments != null && !volumeAttachments.isEmpty()) {
                    LogUtils.log("Volume attachments are : " + volumeAttachments);
                    LogUtils.log("Detaching attachment before deletion");
                    storageApiHelper.detachVolume(volumeByName.getId(), volumeAttachments.iterator().next());
                }
                waitForVolumeAvailable(volumeByName.getId());
                LogUtils.log("Deleting volume " + volumeByName.getId());
                storageApiHelper.deleteVolume(volumeByName.getId());
                nonDeletingVolumes.add(volumeByName);
            }
        }
        if (!nonDeletingVolumes.isEmpty()) {
            AssertUtils.assertFail("Found leaking volumes after test ended :" + nonDeletingVolumes);
        }
    }

    public void waitForVolumeAvailable(final String volumeId) throws TimeoutException {

        final long end = System.currentTimeMillis() + VOLUME_WAIT_TIMEOUT;

        while (System.currentTimeMillis() < end) {

            if (storageApiHelper.isVolumeAvailable(volumeId)) {
                return;
            } else {
                try {
                    LogUtils.log("Waiting for volume " + volumeId
                            + " to reach status AVAILABLE + current status is : " + storageApiHelper.getVolumeStatus(volumeId));
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            }
        }
        throw new TimeoutException("Timed out waiting for volume " + volumeId + " to reach status AVAILABLE");
    }

    public void scanForLeakedVolumesCreatedViaTemplate(final String templateName) throws TimeoutException, StorageProvisioningException {
        final String name = getService().getCloud().getCloudStorage().getTemplates().get(templateName).getNamePrefix();
        scanForLeakedVolumes(name);
    }


}
