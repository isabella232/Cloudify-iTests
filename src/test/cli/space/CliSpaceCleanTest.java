package test.cli.space;


import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.core.GigaSpace;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.cli.common.SpaceMessage;

import com.gigaspaces.cluster.activeelection.SpaceMode;

import framework.utils.CliUtils;

/***
 * Setup: Load 1 GSM and 2 GSC's on 1 machine.
 * Depoly space A partitioned 1,1.
 * Fill space A with 1000 'SpaceMessage' ojbects
 * 
 * Tests: Test "cli space clean" functionality. 
 * Assert that space is clean afterwards
 * 
 * @author Dan Kilman
 *
 */
public class CliSpaceCleanTest extends AbstractTest {
    
    ProcessingUnit datagrid;
    GigaSpace gigaSpace;
    GigaSpace gigaSpaceBackup;
    
    @Override
    @BeforeMethod
    public void beforeTest() {
        
        super.beforeTest();
        
        //Needed so GS.main could work properly
        String key = "com.gs.jini_lus.groups";
        String value = admin.getGroups()[0];
        System.setProperty(key, value);
        

        log("waiting for 1 machine");
        admin.getMachines().waitFor(1);
        
        log("waiting for 1 GSA");
        admin.getGridServiceAgents().waitFor(1);

        GridServiceAgent gsa = admin.getGridServiceAgents().getAgents()[0];
        
        log("starting: 1 GSM and 2 GSC's at 1 machine");
        GridServiceManager gsm = loadGSM(gsa);
        loadGSCs(gsa, 2);
        
        log("Deploying space 'A' partitioned 1,1");
        datagrid = gsm.deploy(new SpaceDeployment("A").partitioned(1, 1).maxInstancesPerVM(1));
        
        assertTrue(datagrid.waitFor(datagrid.getTotalNumberOfInstances()));
        
        GigaSpace aGigaSpace = datagrid.getSpaces()[0].getGigaSpace();
       
        log("Filling space 'A' with 1000 SpaceMessage objects");
        for (int i=0; i<500; i++) {
            aGigaSpace.write(new SpaceMessage("SpaceMessage", 1));
            aGigaSpace.write(new SpaceMessage("SpaceMessage", 2));
        }
        
        SpaceInstance[] aSpaces = datagrid.getSpace().getInstances();
        
        SpaceInstance aSpacePrimaryInstance = aSpaces[0].getMode().equals(SpaceMode.PRIMARY) ? aSpaces[0] : aSpaces[1]; 
        SpaceInstance aSpaceBackupInstance = aSpaces[0].getMode().equals(SpaceMode.BACKUP) ? aSpaces[0] : aSpaces[1]; 

        gigaSpace = aSpacePrimaryInstance.getGigaSpace();
        gigaSpaceBackup = aSpaceBackupInstance.getGigaSpace();

    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceClean() {
        String[] args = {
            "space", "clean",
            "-url", "jini://*/*/A",
            "-c"
        };
        CliUtils.invokeGSMainOn(args);
        
        assertEquals(0, gigaSpace.count(null));
        //Hack needed to count objects in a backup space instance
        try {
            CliUtils.assertCountInTarget(gigaSpaceBackup.getSpace(), 0, null);
        } catch (Exception e) {
            Assert.fail("Error while checking backup space", e);
        }
    }
    
}
