package test.cli.space;


import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.util.Random;

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
import test.utils.CliUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;

/***
 * 
 * Setup: Load 1 GSM and 2 GSC's on 1 machine.
 * Depoly spaces A and B partitioned 1,1.
 * Fill space A with 1000 'SpaceMessage' ojbects
 * 
 * Tests: 
 * a. Test "cli space copy" functionality. 
 * copy objects from 'A' primary instance to 'B' primary instance and check
 * result is as expected.
 * 
 * b. Test "cli space copy -move" functionality.
 * move objects from 'A' primary instance to 'B' primary instance and check
 * result is as expected.
 * 
 * @author Dan Kilman
 *
 */
public class CliSpaceCopyTest extends AbstractTest {
    
    ProcessingUnit datagrid1;
    ProcessingUnit datagrid2;
    GigaSpace aGigaSpace;
    GigaSpace bGigaSpace;
    GigaSpace aGigaSpaceBackup;
    GigaSpace bGigaSpaceBackup;
    
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
        
        log("Deploying spaces 'A' and 'B' partitioned 1,1");
        datagrid1 = gsm.deploy(new SpaceDeployment("A").partitioned(1, 1).maxInstancesPerVM(1));
        datagrid2 = gsm.deploy(new SpaceDeployment("B").partitioned(1, 1).maxInstancesPerVM(1));
        
        assertTrue(datagrid1.waitFor(datagrid1.getTotalNumberOfInstances()));
        assertTrue(datagrid2.waitFor(datagrid1.getTotalNumberOfInstances()));
        
        GigaSpace gigaSpace = datagrid1.getSpaces()[0].getGigaSpace();
       
        log("Filling space 'A' with 1000 SpaceMessage objects");
        Random random = new Random();
        for (int i=0; i<1000; i++) {
            gigaSpace.write(new SpaceMessage("SpaceMessage", random.nextInt(1000)));
        }
        
        SpaceInstance[] aSpaces = datagrid1.getSpace().getInstances();
        SpaceInstance[] bSpaces = datagrid2.getSpace().getInstances();
        
        SpaceInstance aSpacePrimaryInstance = aSpaces[0].getMode().equals(SpaceMode.PRIMARY) ? aSpaces[0] : aSpaces[1]; 
        SpaceInstance aSpaceBackupInstance = aSpaces[0].getMode().equals(SpaceMode.BACKUP) ? aSpaces[0] : aSpaces[1]; 
        SpaceInstance bSpacePrimaryInstance = bSpaces[0].getMode().equals(SpaceMode.PRIMARY) ? bSpaces[0] : bSpaces[1]; 
        SpaceInstance bSpaceBackupInstance = bSpaces[0].getMode().equals(SpaceMode.BACKUP) ? bSpaces[0] : bSpaces[1]; 

        aGigaSpace = aSpacePrimaryInstance.getGigaSpace();
        bGigaSpace = bSpacePrimaryInstance.getGigaSpace();
        aGigaSpaceBackup = aSpaceBackupInstance.getGigaSpace();
        bGigaSpaceBackup = bSpaceBackupInstance.getGigaSpace();
      
    }
    
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceCopy() {

        String[] args = { 
                "space", "copy", 
                "jini://*/A_container1/A",
                "jini://*/B_container1/B" 
        };
        CliUtils.invokeGSMainOn(args);

        // Assert 'space copy'
        assertEquals(1000, aGigaSpace.count(null));
        assertEquals(1000, bGigaSpace.count(null));

        // nasty hack to count space backup instance
        try {
            CliUtils.assertCountInTarget(aGigaSpaceBackup.getSpace(), 1000, new Object());
            CliUtils.assertCountInTarget(bGigaSpaceBackup.getSpace(), 0, new Object());
        } catch (Exception e) {
            Assert.fail("While trying to count backup space instance", e);
        }

    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceCopyMove() {
        String[] args = { 
                "space", "copy", 
                "jini://*/A_container1/A",
                "jini://*/B_container1/B", 
                "-move"
        };
        CliUtils.invokeGSMainOn(args);

        // Assert 'space copy'
        assertEquals(0, aGigaSpace.count(null));
        assertEquals(1000, bGigaSpace.count(null));

        // nasty hack to count space backup instance
        try {
            CliUtils.assertCountInTarget(aGigaSpaceBackup.getSpace(), 1000, new Object());
            CliUtils.assertCountInTarget(bGigaSpaceBackup.getSpace(), 0, new Object());
        } catch (Exception e) {
            Assert.fail("While trying to count backup space instance", e);
        }
    }

    
}
