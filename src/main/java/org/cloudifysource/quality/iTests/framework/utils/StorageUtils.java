package org.cloudifysource.quality.iTests.framework.utils;

/**
 * User: nirb
 * Date: 17/02/13
 */

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.storage.StorageTemplate;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.storage.VolumeDetails;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudService;
import org.jclouds.compute.domain.NodeMetadata;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class StorageUtils {

    //TODO not a specific cloud
    private static StorageProvisioningDriver storageProvisioning;

    private static Cloud cloud;
    private static final long DURATION = 30;

    private static Map<String, Set<String>> serviceToMachines;
    private static Set<String> machinesBeforeInstall;
    private static Set<String> machinesAfterInstall;

    public static void init(final Cloud thatCloud, final String computeTemplateName, final String storageTemplateName, CloudService cloudService) throws Exception{
        cloud = thatCloud;
        storageProvisioning = (StorageProvisioningDriver) Class.forName(cloud.getConfiguration().getStorageClassName()).newInstance();
        storageProvisioning.setConfig(thatCloud, computeTemplateName, storageTemplateName);

        serviceToMachines = new HashMap<String, Set<String>>();
        machinesBeforeInstall = new HashSet<String>();
        machinesAfterInstall = new HashSet<String>();

        JCloudsUtils.createContext(cloudService);
    }

    public static void close(){
        JCloudsUtils.closeContext();
    }

    public static boolean isInitialized(){
        return storageProvisioning != null;
    }

    private static void deleteVolume(final String location, final String volumeId, final long duration, final TimeUnit timeUnit) throws Exception{
        storageProvisioning.deleteVolume(location, volumeId, duration, timeUnit);
    }

    public static void scanAndDeleteLeakedVolumes() throws Exception{

        List<StorageTemplate> storageTemplates = (List<StorageTemplate>) cloud.getCloudStorage().getTemplates().values();
        Set<String> namePrefixes = new HashSet<String>();

        for(StorageTemplate st : storageTemplates){
            namePrefixes.add(st.getNamePrefix());
        }

        //TODO shouldn't be specific to ec2. will be listAll.
//        Set<Volume> allVolumes = storageProvisioning.ebsClient.describeVolumesInRegion((String) null, (String[]) null);
//        Set<VolumeDetails> volumes = new HashSet<VolumeDetails>();
//
//        for (Volume volume : allVolumes) {
//            VolumeDetails volumeDetails = createVolumeDetails(volume);
//            volumes.add(volumeDetails);
//        }
//

//        for(VolumeDetails vd : volumes){
//            for(String prefix : namePrefixes){
//                if(storageDriver.getVolumeName(vd.getId()).startsWith(prefix)){
//                    LogUtils.log("found a leaking volume, Deleting it..");
//                    deleteVolume(vd.getLocation(), vd.getId(), DURATION, TimeUnit.SECONDS);
//                }
//            }
//        }
    }

    public static void beforeServiceInstallation(){

        machinesBeforeInstall.clear();
        Set<? extends NodeMetadata> allNodes = JCloudsUtils.getAllRunningNodes();

        for(NodeMetadata nm : allNodes){
            machinesBeforeInstall.add(nm.getPrivateAddresses().iterator().next());
        }
    }

    public static void afterServiceInstallation(String serviceName){

        machinesAfterInstall.clear();
        Set<? extends NodeMetadata> allNodes = JCloudsUtils.getAllRunningNodes();

        for(NodeMetadata nm : allNodes){
            machinesAfterInstall.add(nm.getPrivateAddresses().iterator().next());
        }

        AssertUtils.assertTrue("installation didn't start any machine", !machinesAfterInstall.isEmpty());

        Set<String> difference = new HashSet<String>(machinesAfterInstall);
        difference.removeAll(machinesBeforeInstall);

        serviceToMachines.put(serviceName, difference);
        machinesAfterInstall.clear();
        machinesBeforeInstall.clear();
    }

    public static void afterServiceUninstallation(String serviceName){
        serviceToMachines.remove(serviceName);
    }

    public static boolean verifyVolumeConfiguration(String serviceFilePath) throws Exception{

        boolean result = true;
        File serviceFile = new File(serviceFilePath);
        Service service = ServiceReader.readService(serviceFile);
        Set<String> machinesIps = serviceToMachines.get(service.getName());

        String templateName = service.getStorage().getTemplate();

        for(String machineIp : machinesIps){
            result = verifyVolumeSize(templateName, machineIp) &&
                    verifyVolumeLocation(templateName, machineIp) &&
                    verifyVolumePrefix(templateName, machineIp);
        }

        return result;
    }
    private static boolean verifyVolumeSize(String templateName, String machineIp) throws Exception{

        String prefix = cloud.getCloudStorage().getTemplates().get(templateName).getNamePrefix();
        int expectedSize = cloud.getCloudStorage().getTemplates().get(templateName).getSize();
        Set<VolumeDetails> volumes = storageProvisioning.listVolumes(machineIp, DURATION, TimeUnit.SECONDS);

        for(VolumeDetails vd : volumes){
            if(vd.getName().startsWith(prefix)){
                if(vd.getSize() != expectedSize){
                    LogUtils.log("the volume " + vd.getId() + " has the wrong size. expected: " + expectedSize + " actual: " + vd.getSize());
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean verifyVolumeLocation(String templateName, String machineIp) throws Exception{

        String expectedLocation = cloud.getCloudCompute().getTemplates().get(templateName).getLocationId();
        Set<VolumeDetails> volumes = storageProvisioning.listVolumes(machineIp, DURATION, TimeUnit.SECONDS);

        for(VolumeDetails vd : volumes){
            if(!expectedLocation.startsWith(vd.getLocation())){
                LogUtils.log("the volume " + vd.getId() + " has the wrong location. expected: " + expectedLocation + " actual: " + vd.getLocation());
                return false;
            }
        }

        return true;
    }

    private static boolean verifyVolumePrefix(String templateName, String machineIp) throws Exception{

        String expectedPrefix = cloud.getCloudStorage().getTemplates().get(templateName).getNamePrefix();
        Set<VolumeDetails> volumes = storageProvisioning.listVolumes(machineIp, DURATION, TimeUnit.SECONDS);

        for(VolumeDetails vd : volumes){
            if(!vd.getName().isEmpty() && !vd.getName().startsWith(expectedPrefix)){
                LogUtils.log("the volume " + vd.getId() + " starts with a wrong prefix. expected: " + expectedPrefix + " actual: " + storageProvisioning.getVolumeName(vd.getId()));
                return false;
            }
        }

        return true;
    }

    public static Map<String, Set<String>> getServicesToMachines(){
        return serviceToMachines;
    }
    public static Set<VolumeDetails> getVolumes(String serviceName) throws Exception {

        Set<String> machinesIps = serviceToMachines.get(serviceName);
        return getVolumes(machinesIps);
    }

    public static Set<VolumeDetails> getVolumes(Set<String> machinesIps) throws Exception {

        Set<VolumeDetails> serviceVolumes = new HashSet<VolumeDetails>();

        for(String machineIp : machinesIps){

            Set<VolumeDetails> machineVolumes = storageProvisioning.listVolumes(machineIp, DURATION, TimeUnit.SECONDS);
            serviceVolumes.addAll(machineVolumes);
        }

        return serviceVolumes;
    }
}

