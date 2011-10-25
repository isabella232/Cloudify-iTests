package test.cli.list;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.util.Iterator;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.CliUtils;


/***
 * 
 * This test gets its data input from test/cli/resources/cli-list-commands.xml
 * Each command is denoted by a command tag with the 'args' attribute being the actual 
 * command tested.

 * Each pattern tag inside command tags represents a regualr expression ('regex' attribute)
 * that is to be matched against the command output and is expected to appear a certain number
 * of times ('expected-amount' attribute)
 * 
 * @author Dan Kilman
 *
 */
public class CliListTest extends AbstractTest {

    @Override
    @BeforeMethod
    public void beforeTest() {
        
        super.beforeTest();
        
        //Needed so GS.main could work properly
        String key = "com.gs.jini_lus.groups";
        String value = admin.getGroups()[0];
        System.setProperty(key, value);
        
        log("waiting for 2 machines");
        admin.getMachines().waitFor(2);

        log("waiting for 2 GSA's");
        admin.getGridServiceAgents().waitFor(2);
        
        GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
        GridServiceAgent gsaA = agents[0];
        GridServiceAgent gsaB = agents[1];
        
        //Start GSM A, GSC A1, GSM B, GSC B1
        log("starting: 2 GSM and 6 GSC at 2 machines");
        GridServiceManager gsmA = loadGSM(gsaA); //GSM A
        loadGSM(gsaB); //GSM B
        loadGSCs(gsaA, 3); //GSC A1 ... A3
        loadGSCs(gsaB, 3); //GSC B1 ... B3
        
        //Deploy Cluster A to GSM A
        log("deploy Cluster A, partitioned 2,1 via GSM A");
        ProcessingUnit puA = gsmA.deploy(new SpaceDeployment("A").partitioned(2, 1).maxInstancesPerVM(1));
        
        assertTrue(puA.waitFor(puA.getTotalNumberOfInstances()));
        
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2", dataProvider="dataProvider")
    public void test(String args, Object[] patterns) throws Exception {
        
        String mainOutput = CliUtils.invokeGSMainOn(args.split(" "));

        for (int i=0; i < patterns.length; i++) {
            String[] patternPair = (String[]) patterns[i];
            String regex = patternPair[0];
            int expectedAmount = 0;
            
            try {
                expectedAmount = Integer.parseInt(patternPair[1]);
            } catch (NumberFormatException e) {
                Assert.fail("Error parsing xml file", e);
            }
            
            log("Testing against '" + regex + "' and expecting it to appear " + expectedAmount + " times");
            assertEquals("When testing: " + regex, expectedAmount, CliUtils.patternCounter(regex, mainOutput));
        }
    
    }
    
    @DataProvider(name = "dataProvider") 
    public Iterator<Object[]> dataProvider() throws Exception {
        return new CliUtils.CommandPatternIterator("test/cli/resources/cli-list-commands.xml");
    }
    
}
