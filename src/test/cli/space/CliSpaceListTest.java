package test.cli.space;


import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.util.Iterator;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.cli.common.SpaceMessage;
import test.utils.CliUtils;

/***
 * 
 * Setup: Load 1 GSM and 2 GSC's on 1 machine.
 * Depoly spaces A partitioned 2,1.
 * Fill space A with 1000 'SpaceMessage' ojbects
 * 
 * Tests: 
 * Test "cli space list" functionality. 
 * 1. check output for expected results against regular expressions 
 *    located in test/cli/resources/cli-space-commands.xml
 * 2. assert expected amount of objects in the cluster
 *  
 * @author Dan Kilman
 *
 */
public class CliSpaceListTest extends AbstractTest {
    
    ProcessingUnit datagrid;
    String mainOutput;
    
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
        
        log("Deploying space 'A' partitioned 2,1");
        datagrid = gsm.deploy(new SpaceDeployment("A").partitioned(2, 1).maxInstancesPerVM(1));
        
        assertTrue(datagrid.waitFor(datagrid.getTotalNumberOfInstances()));
        
        GigaSpace gigaSpace = datagrid.getSpaces()[0].getGigaSpace();
       
        log("Filling space 'A' with 1000 SpaceMessage objects");
        Random random = new Random();
        for (int i=0; i<1000; i++) {
            gigaSpace.write(new SpaceMessage("SpaceMessage", random.nextInt(1000)));
        }
        
    }
    
    /*
     *  1000 objects were put in 2,1 partition, so we expect 2000 objects in general
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceListObjectCount() {
        
        String mainOutput = CliUtils.invokeGSMainOn("space","list");
        
        Pattern pattern = Pattern.compile("(?:Objects count: )([0-9]+)");
        Matcher matcher = pattern.matcher(mainOutput);
        
        int objectCount = 0;
        while (matcher.find()) {
            String current = matcher.group(1);
            objectCount += Integer.parseInt(current);
        }
        
        assertEquals(2000, objectCount);
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", dataProvider="dataProvider")
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
        return new CliUtils.CommandPatternIterator("test/cli/resources/cli-space-commands.xml");
    }
    
}
