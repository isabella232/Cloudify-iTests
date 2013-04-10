package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.esc.driver.provisioning.storage.VolumeDetails;
import org.cloudifysource.quality.iTests.framework.utils.*;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.ec2.domain.Volume;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *
 * Base class for storage tests that use just one service.
 *
 * Provides functionality of :
 * 1. Linux testing (deployed on management machine to save time)
 * 2. Ubuntu testing (deployed on a dedicated instance)
 *
 * Important
 * =========
 *
 * The service name and recipe file prefix must match. <br></br>
 * Example : for a service called groovy, a groovy-service.groovy file must exist in the service directory.<br></br>
 *
 * This class uninstalls the service automatically after the test is over if it was not uninstalled during the test.<br></br>
 *
 * This class copies the desired recipe to the recipes/services folder in the distribution. so that installars can install using just the service name.<br></br>
 *
 * The desired recipe folder is determined by the two abstract methods tests are required to implement <br></br>
 *  1. getServiceFolder() <br></br>
 *  2. getPathToServicesFolder() <br></br>
 *
 * The actual test logic should be implemented in the doTest() method. <br></br>
 *
 * For further information please see usage examples using the hierarchy view. <br></br>
 *
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 4/10/13
 * Time: 12:39 PM
 */
public abstract class AbstractEc2OneServiceStorageAllocationTest extends AbstractEc2StorageAllocationTest {

    private String serviceName;

    public abstract String getServiceFolder();
    public abstract String getPathToServicesFolder();
    public abstract void doTest() throws Exception ;

    public String getServiceName() {
        return serviceName;
    }

    @BeforeMethod
    public void prepareService() throws IOException, DSLException, PackagingException {
        String buildRecipesServicesPath = ScriptUtils.getBuildRecipesServicesPath();
        File serviceFolder = new File(buildRecipesServicesPath, getServiceFolder());
        serviceName = ServiceReader.readService(serviceFolder).getName();
        if (serviceFolder.exists()) {
            FileUtils.deleteDirectory(serviceFolder);
        }
        FileUtils.copyDirectoryToDirectory( new File(getPathToServicesFolder() + "/" + getServiceFolder()), new File(buildRecipesServicesPath));
    }

    public void testWriteToStorage(final String folderName) throws IOException, InterruptedException {

        ServiceInstaller installer = new ServiceInstaller(getRestUrl(), serviceName);
        installer.recipePath(folderName);
        installer.setDisableSelfHealing(true);
        installer.install();

        LogUtils.log("creating a new file called foo.txt in the storage volume. " + "running 'touch ~/storage/foo.txt' command on remote machine.");
        invokeCommand(serviceName, "writeToStorage");

        LogUtils.log("listing all files inside mounted storage folder. running 'ls ~/storage/' command");
        String listFilesResult = invokeCommand(serviceName, "listFilesInStorage");

        assertTrue("File was not created in storage volume. Output was " + listFilesResult, listFilesResult.contains("foo.txt"));

        installer.uninstall();
    }

    public void testMount(final String folderName) throws Exception {

        ServiceInstaller installer = new ServiceInstaller(getRestUrl(), serviceName);
        installer.recipePath(folderName);
        installer.setDisableSelfHealing(true);
        installer.install();

        LogUtils.log("Creating a new file called foo.txt in the storage volume. " + "running 'touch ~/storage/foo.txt' command on remote machine.");
        invokeCommand(serviceName, "writeToStorage");

        LogUtils.log("listing all files inside mounted storage folder. running 'ls ~/storage/' command");
        String listFilesResult = invokeCommand(serviceName, "listFilesInStorage");

        assertTrue("File was not created in storage volume. Output was " + listFilesResult, listFilesResult.contains("foo.txt"));

        LogUtils.log("Unmounting volume from file system");
        invokeCommand(serviceName, "unmount");

        Volume vol = storageHelper.getVolumeByName(getVolumePrefixForTemplate("SMALL_BLOCK"));
        storageHelper.detachVolume(vol.getId());

        //asserting the file is not in the mounted directory
        LogUtils.log("listing all files inside mounted storage folder. running 'ls ~/storage/' command");
        listFilesResult = invokeCommand(serviceName, "listFilesInStorage");

        assertTrue("The newly created file is in the mounted directory after detachment", !listFilesResult.contains("foo.txt"));

        LogUtils.log("Reattaching the volume to the service machine");


        Ec2ComputeApiHelper computeHelper = new Ec2ComputeApiHelper(getService().getCloud());

        NodeMetadata agent = computeHelper.getServerByName(getService().getCloud().getProvider().getMachineNamePrefix());
        storageHelper.attachVolume(vol.getId(), agent.getId(), getService().getCloud().getCloudStorage().getTemplates().get("SMALL_BLOCK").getDeviceName());
        invokeCommand(serviceName, "mount");

        //asserting the file is in the mounted directory
        LogUtils.log("listing all files inside mounted storage folder. running 'ls ~/storage/' command");
        listFilesResult = invokeCommand(serviceName, "listFilesInStorage");

        assertTrue("the created file is not in the mounted directory after reattachment", listFilesResult.contains("foo.txt"));    }


    public void testStorageVolumeMounted(final String folderName, final String expectedMountOutput) throws IOException, InterruptedException {

        ServiceInstaller installer = new ServiceInstaller(getRestUrl(), serviceName);
        installer.recipePath(folderName);
        installer.setDisableSelfHealing(true);
        installer.install();

        LogUtils.log("Listing all mounted devices. running command 'mount -l' on remote machine");
        String listMountedResult = invokeCommand(serviceName, "listMount");

        assertTrue("device is not in the mounted devices list: " + listMountedResult,
                listMountedResult.contains(expectedMountOutput));

        installer.uninstall();
    }


    public void testInstallWithStorage(final String folderName) throws IOException, InterruptedException {

        ServiceInstaller installer = new ServiceInstaller(getRestUrl(), serviceName);
        installer.recipePath(folderName);
        installer.setDisableSelfHealing(true);
        installer.install();

        LogUtils.log("Searching for volumes created by the service installation");
        // the install should have created and attached a volume with a name prefix of the class name. see customizeCloud below.
        Volume ourVolume = storageHelper.getVolumeByName(getVolumePrefixForTemplate("SMALL_BLOCK"));

        AssertUtils.assertNotNull("could not find the required volume after install service", ourVolume);
        LogUtils.log("Found volume : " + ourVolume);
        // also check it is attached.
        AssertUtils.assertEquals("the volume should have one attachments", 1, ourVolume.getAttachments().size());

        // TODO elip - assert Volume configuration?

        installer.uninstall();
    }

    public void testDeleteOnExitFalse(final String folderName) throws IOException, InterruptedException {

        ServiceInstaller installer = new ServiceInstaller(getRestUrl(), serviceName);
        installer.recipePath(folderName);
        installer.setDisableSelfHealing(true);
        installer.install();

        LogUtils.log("Searching for volumes created by the service installation");
        // the install should have created and attached a volume with a name prefix of the class name. see customizeCloud below.
        Volume ourVolume = storageHelper.getVolumeByName(getVolumePrefixForTemplate("SMALL_BLOCK"));

        AssertUtils.assertNotNull("could not find the required volume after install service", ourVolume);
        LogUtils.log("Found volume : " + ourVolume);
        // also check it is attached.
        AssertUtils.assertEquals("the volume should have one attachments", 1, ourVolume.getAttachments().size());

        // TODO elip - assert Volume configuration?

        installer.uninstall();

        LogUtils.log("Searching for volumes created by the service after uninstall");
        // the install should have created and attached a volume with a name prefix of the class name. see customizeCloud below.
        ourVolume = storageHelper.getVolumeByName(getVolumePrefixForTemplate("SMALL_BLOCK"));

        AssertUtils.assertNotNull("could not find the required volume after install service", ourVolume);
        LogUtils.log("Found volume : " + ourVolume);
        storageHelper.deleteVolume(ourVolume.getId());

    }

    public String getVolumePrefixForTemplate(final String templateName) {
        return getService().getCloud().getCloudStorage().getTemplates().get(templateName).getNamePrefix();
    }

    protected void setTemplate(final String computeTemplateName, boolean useManagement) throws Exception {
        File serviceFile = new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + getServiceFolder(), serviceName + "-service.groovy");
        Map<String, String> props = new HashMap<String,String>();
        props.put("ENTER_TEMPLATE", computeTemplateName);
        if (!useManagement) {
            props.put("useManagement true", "useManagement false");
        }
        IOUtils.replaceTextInFile(serviceFile.getAbsolutePath(), props);
    }

    public void testUbuntu() throws Exception {
        setTemplate("SMALL_UBUNTU", false);
        doTest();

    }

    public void testLinux(final boolean useManagement) throws Exception {
        setTemplate("SMALL_LINUX", useManagement);
        doTest();
    }

    public void testLinux() throws Exception {
        testLinux(true);
    }

    @AfterMethod
    public void deleteService() throws IOException {
        FileUtils.deleteDirectory(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + getServiceFolder()));
    }

    @AfterMethod
    public void uninstallServiceIfFound() throws IOException, InterruptedException {
        uninstallServiceIfFound(serviceName);
    }
}
