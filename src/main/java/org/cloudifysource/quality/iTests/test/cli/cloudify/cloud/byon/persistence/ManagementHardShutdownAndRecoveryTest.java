package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.persistence;

import org.cloudifysource.quality.iTests.framework.utils.DisconnectionUtils;
import org.cloudifysource.quality.iTests.framework.utils.IOUtils;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileSystemUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: sagib
 * Date: 3/5/13
 * Time: 5:32 PM
 */
public class ManagementHardShutdownAndRecoveryTest extends AbstractByonManagementPersistencyTest {

    private Machine[] gsmMachines;

    /**
     * Override since we want to shutdown the machine manually.
     * @throws Exception
     */
    @Override
    public void shutdownManagement() throws Exception{
        for (Machine manager : gsmMachines) {
            DisconnectionUtils.restartMachineAndWait(manager.getHostAddress());
        }
    }

    @BeforeClass(alwaysRun = true)
    public void bootstrapAndInit() throws Exception{
        super.bootstrapAndInstallService();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true, groups = SUSPECTED)
    public void testManagementPersistency() throws Exception {
        super.testManagementPersistency();
    }

    @AfterClass(alwaysRun = true)
    public void teardown() throws Exception{
        super.teardownAndDeleteBackupFile();
    }


    @Override
    protected void afterBootstrap() throws Exception {

        super.afterBootstrap();

        // when bootstrap is done. we save the management machines ip to a persistence file.
        prepareManagementPersistencyFile();
    }

    private void prepareManagementPersistencyFile() throws Exception{

        Resource originalFile = new ClassPathResource("apps/cloudify/cloud/byon/management-persistency.txt");
        File tempFile = new File("management-persistency.txt");
        FileSystemUtils.copyRecursively(originalFile.getFile(), tempFile);
        Map<String, String> props = new HashMap<String, String>();

        GridServiceManager[] griServiceManagers = admin.getGridServiceManagers().getManagers();
        gsmMachines = new Machine[griServiceManagers.length];
        for (int i = 0 ; i < griServiceManagers.length ; i++) {
            gsmMachines[i] = griServiceManagers[i].getMachine();
            props.put("MANAGEMENT" + i, gsmMachines[i].getHostAddress());
        }
        IOUtils.replaceTextInFile(tempFile, props);
        backupFilePath = tempFile.getAbsolutePath();
    }

}
