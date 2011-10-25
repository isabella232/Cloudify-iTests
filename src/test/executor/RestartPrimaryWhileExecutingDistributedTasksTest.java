package test.executor;

import com.gigaspaces.async.AsyncFuture;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import test.AbstractTest;
import test.data.Stock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;


public class RestartPrimaryWhileExecutingDistributedTasksTest extends AbstractTest {

    private Machine machineA;
    private int numberOfExecutes;
    private static final long OBJECTS = 40000;

    @BeforeMethod
    public void setup() {
        //1 GSM and 2 GSC at 2 machines
        log("waiting for 1 machine");
        admin.getMachines().waitFor(1);

        log("waiting for 1 GSA");
        admin.getGridServiceAgents().waitFor(1);

        GridServiceAgent gsa = admin.getGridServiceAgents().getAgents()[0];


        machineA = gsa.getMachine();

        //Start GSM A, GSC A1, GSC A2
        log("starting: 1 GSM and 2 GSC at 1 machine");
        GridServiceManager gsmA = loadGSM(machineA); //GSM A
        loadGSCs(machineA, 2); //GSC A1, GSC A2


        //Deploy Cluster A to GSM A
        log("deploying: 1 cluster A , partitioned 2,1");
        ProcessingUnit puA = gsmA.deploy(new SpaceDeployment("A").numberOfInstances(2)
                .numberOfBackups(1).maxInstancesPerVM(1));
        assertTrue(puA.waitFor(puA.getTotalNumberOfInstances()));
    }

    @AfterMethod
    void tearDown(){
        admin.getProcessingUnits().getProcessingUnit("A").undeploy();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test1() throws Exception {
        final GigaSpace gigaSpace = admin.getProcessingUnits().getProcessingUnit("A").getSpace().getGigaSpace();
        for (int i = 0; i < OBJECTS; i++) {
            Stock stock = new Stock(i);
            stock.setRate(1);
            gigaSpace.write(stock);
        }

        admin.getProcessingUnits().getProcessingUnit("A").getPartitions()[0]
                .getPrimary().getGridServiceContainer().restart();


        List<AsyncFuture<Long>> futures = new ArrayList<AsyncFuture<Long>>();
        for (int i = 0; i < numberOfExecutes; ++i) {
            futures.add(gigaSpace.execute(new PayloadSumDistributedTask(new Stock())));
        }

        for (int i = 0; i < numberOfExecutes; ++i) {
            Assert.assertEquals(futures.get(i).get(10, TimeUnit.SECONDS).longValue(), OBJECTS);
        }

    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test2() throws Exception {
        final GigaSpace gigaSpace = admin.getProcessingUnits().getProcessingUnit("A").getSpace().getGigaSpace();
        for (int i = 0; i < OBJECTS; i++) {
            Stock stock = new Stock(i);
            stock.setRate(1);
            gigaSpace.write(stock);
        }

        admin.getProcessingUnits().getProcessingUnit("A").getPartitions()[0]
                .getPrimary().getPartition().getPrimary().restart();
        admin.getProcessingUnits().getProcessingUnit("A").getPartitions()[1]
                .getPrimary().getPartition().getPrimary().restart();

        List<AsyncFuture<Long>> futures = new ArrayList<AsyncFuture<Long>>();
        for (int i = 0; i < numberOfExecutes; ++i) {
            futures.add(gigaSpace.execute(new PayloadSumDistributedTask(new Stock())));
        }

        for (int i = 0; i < numberOfExecutes; ++i) {
            Assert.assertEquals(futures.get(i).get(10, TimeUnit.SECONDS).longValue(), OBJECTS);
        }

    }

}
