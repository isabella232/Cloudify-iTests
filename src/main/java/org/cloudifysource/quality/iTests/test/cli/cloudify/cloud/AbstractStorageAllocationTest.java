package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;
import org.cloudifysource.esc.driver.provisioning.storage.VolumeDetails;
import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;
import org.cloudifysource.quality.iTests.framework.utils.compute.ComputeApiHelper;
import org.cloudifysource.quality.iTests.framework.utils.compute.JcloudsComputeApiHelper;
import org.cloudifysource.quality.iTests.framework.utils.storage.Ec2StorageApiHelper;
import org.cloudifysource.quality.iTests.framework.utils.storage.OpenstackStorageApiHelper;
import org.cloudifysource.quality.iTests.framework.utils.storage.StorageAllocationTester;
import org.cloudifysource.quality.iTests.framework.utils.storage.StorageApiHelper;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudService;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.hpgrizzly.HpGrizzlyCloudService;

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
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        //change management template into LARGE_LINUX in ec2 tests
        if (getCloudName().equals("ec2")) {
            String oldMgtTemplate = "managementMachineTemplate \"SMALL_LINUX\"";
            String newMgtTemplate = "managementMachineTemplate \"LARGE_LINUX\"";
            getService().getAdditionalPropsToReplace().put(oldMgtTemplate, newMgtTemplate);

            String oldTemplate = "MEDIUM_UBUNTU";
            String newTemplate = "LARGE_LINUX";
            getService().getAdditionalPropsToReplace().put(oldTemplate, newTemplate);

            String oldImageId = "imageId ubuntuImageId";
            String newImageId = "imageId linuxImageId";
            getService().getAdditionalPropsToReplace().put(oldImageId, newImageId);

            String oldRemoteDirectory = "remoteDirectory \"/home/ubuntu/gs-files\"";
            String newRemoteDirectory = "remoteDirectory \"/home/ec2-user/gs-files\"";
            getService().getAdditionalPropsToReplace().put(oldRemoteDirectory, newRemoteDirectory);

            String oldMemory = "machineMemoryMB 3500";
            String newMemory = "machineMemoryMB 7400";
            getService().getAdditionalPropsToReplace().put(oldMemory, newMemory);

            String oldHardwareId = "hardwareId mediumHardwareId";
            String newHardwareId = "hardwareId \"m1.large\"";
            getService().getAdditionalPropsToReplace().put(oldHardwareId, newHardwareId);

            String oldUserName= "username \"ubuntu\"";
            String newUserName = "username \"ec2-user\"";
            getService().getAdditionalPropsToReplace().put(oldUserName, newUserName);
        }
    }

    @Override
    protected void bootstrap(CloudService service, CloudBootstrapper bootstrapper) throws Exception {
        super.bootstrap(service, bootstrapper);
        storageApiHelper = initStorageHelper();
        computeApiHelper = initComputeHelper();
        storageAllocationTester = new StorageAllocationTester(getRestUrl(), storageApiHelper, getService(), computeApiHelper);
    }

    private ComputeApiHelper initComputeHelper() {
        if (getCloudName().equals("ec2") || getCloudName().equals("hp-folsom") || getCloudName().equals("rackspace") || getCloudName().equals("hp-grizzly")) {
            return new JcloudsComputeApiHelper(getService().getCloud(), getService().getRegion());
        }
        throw new UnsupportedOperationException("Cannot init compute helper for non jclouds providers!");
    }

    private StorageApiHelper initStorageHelper() {
        if (getCloudName().equals("ec2")) {
            return new Ec2StorageApiHelper(getService().getCloud()
                    ,"SMALL_LINUX"
                    ,((Ec2CloudService)getService()).getRegion()
                    ,((Ec2CloudService)getService()).getComputeServiceContext());
        }if (getCloudName().equals("hp-grizzly")) {
            return new OpenstackStorageApiHelper(getService().getCloud()
                    ,"SMALL_LINUX"
                    ,((HpGrizzlyCloudService)getService()).getComputeServiceContext());
        }
        throw new UnsupportedOperationException("Cannot init storage helper for clouds that are not ec2 or Openstack");
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
                    String attachmentId = volumeAttachments.iterator().next();
                    LogUtils.log("Detaching attachment[" + attachmentId + "] before deletion");
                    storageApiHelper.detachVolume(volumeByName.getId(), computeApiHelper.getServerByAttachmentId(attachmentId).getPrivateAddress());
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

    public void waitForVolumeAvailable(final String volumeId) throws TimeoutException, StorageProvisioningException {

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
