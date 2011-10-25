package test.servicegrid.failover;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.data.Person;
import test.utils.AdminUtils;
import test.utils.LogUtils;

public class SinglePartitionNoBackupClusterFailoverTest extends AbstractTest {
    
    private static final int NUMBER_OF_OBJECTS_TO_WRITE = 1000;
    private GridServiceAgent gsa;
    private GridServiceManager gsm;
    private GridServiceContainer gsc;

    @BeforeMethod
    @Override
    public void beforeTest() {
        super.beforeTest();
    
        LogUtils.log("Waiting for 1 GSA");
        gsa = admin.getGridServiceAgents().waitForAtLeastOne();

        LogUtils.log("Starting 1 GSM");
        gsm = AdminUtils.loadGSM(gsa);
        
        LogUtils.log("Starting 1 GSC");
        gsc = AdminUtils.loadGSC(gsa);
        
    }
    
    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test() {

        LogUtils.log("Deploying myDataGrid (1,0)");
        ProcessingUnit pu = gsm.deploy(new SpaceDeployment("myDataGrid")
            .partitioned(1, 0)
            .setContextProperty("space-config.proxy-settings.connection-retries", "100"));
        
        LogUtils.log("Waiting for space");
        assertTrue("Failed waiting for space", 
                pu.waitForSpace().waitFor(pu.getTotalNumberOfInstances(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        
        GigaSpace gigaSpace = pu.waitForSpace().getGigaSpace();
        
        LogUtils.log("Writing objects to space");
        performWriteAndCount(gigaSpace);
        
        LogUtils.log("Restarting GSC");
        gsc.restart();
        
        LogUtils.log("Writing objects to space");
        performWriteAndCount(gigaSpace);
        
    }
    
    
    
    private void performWriteAndCount(GigaSpace gigaSpace) {
        Person[] persons = new Person[NUMBER_OF_OBJECTS_TO_WRITE];
        for (int i=0; i<persons.length; i++) {
            persons[i] = new Person((long)i, String.valueOf(i));
        }
        gigaSpace.writeMultiple(persons);
        
        int count = gigaSpace.count(new Person());
        assertEquals("Number of person objects does not match", NUMBER_OF_OBJECTS_TO_WRITE, count);
    }
    

}
