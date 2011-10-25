package test.cli.list;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.CliUtils;

import test.AbstractTest;

/***
 * Setup: 
 * Bring up 2 GSA's on 2 machines, then bring up 2 GSM's and 6 GSC's.
 * Then load Space partitioned 2,1
 * 
 * Tests:
 * validate "list cpu" output 
 * 
 * @author Dan Kilman
 *
 */
public class CliListCpuTest extends AbstractTest {

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
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
    public void testListCpu() {
        
        String mainOutput = CliUtils.invokeGSMainOn("list", "cpu");
        
        String regex = "(?:\\[[0-9]+\\] .+\\t\\t\\t.+\\t.+ )(.+)(?:   \\n)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(mainOutput);
        
        while (matcher.find()) {
            String cpu = matcher.group(1);
            if (cpu.equals("n/a")) {
                continue;
            } else {
                assertEquals('%', cpu.charAt(cpu.length()-1));
                try {
                    float num = Float.parseFloat(cpu.substring(0, cpu.length()-1));
                    assertTrue(0 <= num && num <= 100);
                } catch (NumberFormatException e) {
                    Assert.fail("Expected number format", e);
                }
            }
        }
    }

}
