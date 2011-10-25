package test.cli.space;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.util.Random;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.cli.common.SpaceMessage;
import test.utils.CliUtils;

/***
 * Setup: Load 1 GSM and 2 GSC's on 1 machine.
 * Depoly spaces A partitioned 2,1.
 * Fill space A with 1000 'SpaceMessage' ojbects
 * 
 * Tests: Test "cli space sql" functionality with a count query 
 * 
 * @author Dan Kilman
 *
 */
public class CliSpaceSqlTest extends AbstractTest {
    
    ProcessingUnit datagrid;

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
        
        GigaSpace gigaSpace = datagrid.getSpaces()[0].getGigaSpace();
       
        log("Filling space 'A' with 1000 SpaceMessage objects");
        Random random = new Random();
        for (int i=0; i<1000; i++) {
            gigaSpace.write(new SpaceMessage("SpaceMessage", random.nextInt(1000)));
        }
        
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceSql() {
        
        String[] args = {
          "space", "sql",
          "-url", "jini://*/*/A",
          "-query", "select", "count(*)", "from", "test.cli.common.SpaceMessage"
        };
        
        String mainOutput = CliUtils.invokeGSMainOn(args);
        
        assertEquals("Testing: 1000" ,1, CliUtils.patternCounter("1000", mainOutput));
    }
    
}
