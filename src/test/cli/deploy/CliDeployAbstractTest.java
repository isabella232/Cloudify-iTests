package test.cli.deploy;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.machine.Machine;
import org.testng.annotations.BeforeMethod;

import test.AbstractTest;

/*
 * Abstract class for setup shared across cli.deploy tests
 */
public abstract class CliDeployAbstractTest extends AbstractTest {
    
    @Override
    @BeforeMethod
    public void beforeTest() {
        
        super.beforeTest();
        
        //Needed so GS.main could work properly
        String key = "com.gs.jini_lus.groups";
        String value = admin.getGroups()[0];
        System.setProperty(key, value);
        
        //1 GSM and 2 GSC at 1 machines
        log("waiting for 1 machine");
        admin.getMachines().waitFor(1);

        log("waiting for 1 GSA");
        admin.getGridServiceAgents().waitFor(1);
        
        GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
        GridServiceAgent gsaA = agents[0];
        
        Machine machineA = gsaA.getMachine();
        
        //Start GSM A, GSC A1, A2
        log("starting: 1 GSM and 2 GSC's");
        loadGSM(machineA); //GSM A
        loadGSCs(machineA, 2); //GSC A1, GSC A2

    }
    
}
