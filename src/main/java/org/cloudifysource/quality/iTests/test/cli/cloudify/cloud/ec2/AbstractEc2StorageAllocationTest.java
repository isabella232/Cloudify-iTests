package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.Ec2StorageApiForRegionHelper;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.jclouds.ec2.domain.Volume;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 *
 * Base class for all storage ec2 tests.
 * <br>
 *     Initializes automatically an instance of {@link Ec2StorageApiForRegionHelper} to provide direct storage api calls to ec2.
 * </br>
 * <br>
 *     Provides methods for scanning for leaked volumes based on volume name or template name.
 * </br>
 *
 *
 *
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 4/9/13
 * Time: 3:36 PM
 */
public abstract class AbstractEc2StorageAllocationTest extends NewAbstractCloudTest {

    private static final long VOLUME_WAIT_TIMEOUT = 3 * 60 * 1000;

    protected Ec2StorageApiForRegionHelper storageHelper;

    @Override
    protected void bootstrap() throws Exception {
        super.bootstrap();
        Ec2CloudService ec2CloudService = (Ec2CloudService)getService();
        this.storageHelper = new Ec2StorageApiForRegionHelper(ec2CloudService.getRegion(), ec2CloudService.getComputeServiceContext());
    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        ((Ec2CloudService)getService()).getAdditionalPropsToReplace().put("cloudify-storage-volume", System.getProperty("user.name") + "-" + this.getClass().getSimpleName().toLowerCase());

    }

    public void scanForLeakedVolumes(final String name) throws TimeoutException {

        LogUtils.log("Scanning for leaking volumes with name " + name);
        Set<Volume> volumesByName = storageHelper.getVolumesByName(name);
        Set<Volume> nonDeletingVolumes = new HashSet<Volume>();

        for (Volume volumeByName : volumesByName) {
            if (volumeByName.getStatus().equals(Volume.Status.DELETING)) {
                LogUtils.log("Found a volume with id " + volumeByName.getId() + " and status " + volumeByName.getStatus()
                        + ". this is ok. this volume is being deleted");
            } else {
                LogUtils.log("Found a leaking volume " + volumeByName + ". status is " + volumeByName.getStatus());
                if (volumeByName.getAttachments() != null && !volumeByName.getAttachments().isEmpty()) {
                    LogUtils.log("Volume attachments are : " + volumeByName.getAttachments());
                    LogUtils.log("Detaching attachment before deletion");
                    storageHelper.detachVolume(volumeByName.getId());
                }
                waitForVolumeStatus(volumeByName, Volume.Status.AVAILABLE);
                LogUtils.log("Deleting volume " + volumeByName.getId());
                storageHelper.deleteVolume(volumeByName.getId());
                nonDeletingVolumes.add(volumeByName);
            }
        }
        if (!nonDeletingVolumes.isEmpty()) {
            AssertUtils.assertFail("Found leaking volumes after test ended :" + nonDeletingVolumes);
        }


    }

    public void scanForLeakedVolumesCreatedViaTemplate(final String templateName) throws TimeoutException {
        final String name = getService().getCloud().getCloudStorage().getTemplates().get(templateName).getNamePrefix();
        scanForLeakedVolumes(name);
    }

    private void waitForVolumeStatus(final Volume vol, final Volume.Status status) throws TimeoutException {

        final long end = System.currentTimeMillis() + VOLUME_WAIT_TIMEOUT;

        while (System.currentTimeMillis() < end) {

            Volume volume = storageHelper.getVolumeById(vol.getId());
            if (volume.getStatus().equals(status)) {
                return;
            } else {
                try {
                    LogUtils.log("Waiting for volume " + vol.getId()
                            + " to reach status " + status + " . current status is : " + volume.getStatus());
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            }

        }
        throw new TimeoutException("Timed out waiting for volume " + vol + " to reach status " + status);

    }

    @Override
    protected String getCloudName() {
        return "ec2";
    }
}
