package test.gateway;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.gigaspaces.annotation.pojo.SpaceVersion;
import com.j_spaces.core.client.Modifiers;
import net.jini.core.lease.Lease;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.transaction.manager.DistributedJiniTxManagerConfigurer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import test.utils.AssertUtils;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;
import test.utils.ThreadBarrier;

import java.util.*;
import static test.utils.LogUtils.log;

public class GatewayTwoSidedTargetGWFailOverMultipleObjectsUnderTxnTest extends AbstractGatewayTest {

    private ThreadBarrier barrier;
    private ThreadBarrier foBarrier = new ThreadBarrier(1);
    private volatile static boolean stopUpdates = false;

    private Admin admin1;
    private Admin admin2;
    private GigaSpace gigaSpace2;
    private GigaSpace gigaSpace1;

    private GigaSpace txnGigaSpace1;
    private GigaSpace txnGigaSpace2;

    private String group1 = null;
    private String group2 = null;
    private String host1 = null;
    private String host2 = null;

    private final int numberOfUpdateThreads = 4;
    private final int numberOfIds = 50000;

    private PlatformTransactionManager ptmLON;
    private PlatformTransactionManager ptmISR;

    public GatewayTwoSidedTargetGWFailOverMultipleObjectsUnderTxnTest() {
        if (isDevMode()) {
            group1 = "israel-" + getUserName();
            group2 = "london-" + getUserName();
            host1 = "localhost";
            host2 = "localhost";
        } else {
            group1 = GROUP1;
            group2 = GROUP2;
            host1 = HOST1;
            host2 = HOST2;
        }
    }

    @AfterMethod
    public void tearDown() {
        TeardownUtils.teardownAll(admin1, admin2);
        admin1.close();
        admin2.close();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2", enabled=true)
    public void test() throws Exception {
        initialize();

        barrier = new ThreadBarrier(numberOfUpdateThreads + 1);

        final List<Thread> updateThreads = new ArrayList<Thread>();
        final List<Integer> testDataIds;

        testDataIds = fillSpace(numberOfIds, txnGigaSpace1, ptmISR);

        assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(admin1);
        final List<Integer> evenDataIds = new ArrayList<Integer>();
        final List<Integer> oddDataIds = new ArrayList<Integer>();
        for (int i = 0; i < testDataIds.size(); i++) {
            if (i % 2 == 0) {
                evenDataIds.add(testDataIds.get(i));
            } else {
                oddDataIds.add(testDataIds.get(i));
            }
        }

        // THREADS
        for (int i = 0; i < numberOfUpdateThreads; ++i) {
            final int index = i;
            final GigaSpace space;
            final List<Integer> ids;
            final PlatformTransactionManager ptm;
            if (i % 2 == 0) {
                space = txnGigaSpace2;
                ids = evenDataIds;
                ptm = ptmLON;
            } else {
                space = txnGigaSpace1;
                ids = oddDataIds;
                ptm = ptmISR;
            }
            final Thread updateThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        runUpdates(index, space, ptm, ids, (int) ((ids.size() / 2.0) / numberOfUpdateThreads));
                        barrier.await();
                    } catch (Throwable t) {
                        barrier.reset(t);
                    }
                }
            });
            updateThread.start();
            updateThreads.add(updateThread);
        }
        Thread.sleep(10000);

        final Thread restartThread = new Thread(new Runnable() {
            public void run() {
                try {
                    restartAndWait(admin2, "LONDON_GW");
                } catch (Throwable t) {
                    foBarrier.reset(t);
                }
            }
        });
        restartThread.start();

        System.out.println("Stopping update threads");
        stopUpdates = true;
        barrier.await();
        System.out.println("All update threads stopped");

        foBarrier.inspect();

        assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(admin1, admin2);

        Stock[] msgs1 = gigaSpace1.readMultiple(new Stock());
        Stock[] msgs2 = gigaSpace2.readMultiple(new Stock());
        AssertUtils.assertEquals(msgs2.length, msgs1.length);
        AssertUtils.assertEquivalenceArrays("readMultiple returned wrong results", msgs2, msgs1);


    }

    private List<Integer> fillSpace(int numberOfIds, GigaSpace remoteGigaSpace, PlatformTransactionManager ptm) {
        List<Integer> testDataIds = new ArrayList<Integer>();
        for (int id = 0; id < numberOfIds / 1000; id++) {
            Stock[] stocks = new Stock[1000];
            DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
            definition.setTimeout(30000);
            TransactionStatus status = ptm.getTransaction(definition);
            for (int i = 0; i < 1000; i++) {
                final Stock td = new Stock();
                {
                    td.setId(i + (id * 1000));
                    td.setInfo("Info" + Integer.valueOf(i + (id * 1000)));
                }
                stocks[i] = td;
                testDataIds.add(i + (id * 1000));
            }
            remoteGigaSpace.writeMultiple(stocks, Lease.FOREVER, Modifiers.WRITE);
            ptm.commit(status);
        }
        return testDataIds;
    }

    private static void runUpdates(final int index, final GigaSpace remoteGigaSpace, final PlatformTransactionManager ptm, final List<Integer> ids, int numberOfTakes) throws InterruptedException {
        System.out.println("runUpdates " + index + " started (size " + ids.size() + ", number of takes " + numberOfTakes + ")");

        while (stopUpdates == false) {
            // SELECT RANDOM OPERATION
            final boolean fullTake = numberOfTakes > 0 && Math.random() < 0.5;

            // SELECT RANDOM ID
            final Integer[] id = new Integer[5];
            synchronized (ids) {
                if (ids.isEmpty() && ids.size() < 5) {
                    break;
                } else {
                    if (fullTake) {
                        for (int i = 0; i < 5; i++)
                            id[i] = ids.remove(ids.size() - 1);
                    } else {
                        for (int i = 0; i < 5; i++)
                            id[i] = ids.get(ids.size() - (i + 1));
                    }
                }
            }
            DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
            definition.setTimeout(30000);
            TransactionStatus status = ptm.getTransaction(definition);
            try {
                if (!fullTake) {
                    if (Math.random() < 0.7) {
                        for (int i = 0; i < 5; i++) {
                            final Stock td = new Stock();
                            {
                                td.setId(id[i]);
                                td.setInfo("update Info");
                            }
                            remoteGigaSpace.write(td);
                        }
                        ptm.commit(status);
                    } else {
                        for (int i = 0; i < 5; i++) {
                            remoteGigaSpace.takeById(Stock.class, id[i]);
                        }
                        ptm.commit(status);
                    }
                } else {

                    for (int i = 0; i < 5; i++) {
                        remoteGigaSpace.takeById(Stock.class, id[i]);
                    }

                    ptm.commit(status);
                    numberOfTakes -= 5;
                }
            } catch (Exception e) {
                e.printStackTrace();
                ptm.rollback(status);
            }
        }

        System.out.println("runUpdates " + index + " finished");
    }

    private void initialize() throws Exception {
        ptmLON = new DistributedJiniTxManagerConfigurer().commitTimeout(30000).transactionManager();
        ptmISR = new DistributedJiniTxManagerConfigurer().commitTimeout(30000).transactionManager();

        log("initializing..");
        admin1 = new AdminFactory().addGroups(group1).createAdmin();
        admin2 = new AdminFactory().addGroups(group2).createAdmin();
        SetupUtils.assertCleanSetup(admin1);
        SetupUtils.assertCleanSetup(admin2);

        GridServiceAgent gsa1 = admin1.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent gsa2 = admin2.getGridServiceAgents().waitForAtLeastOne();

        gsa1.startGridService(new GridServiceManagerOptions());
        gsa1.startGridService(new GridServiceContainerOptions());
        gsa1.startGridService(new GridServiceContainerOptions());

        gsa2.startGridService(new GridServiceManagerOptions());
        gsa2.startGridService(new GridServiceContainerOptions());
        gsa2.startGridService(new GridServiceContainerOptions());

        final GridServiceManager gsm1 = admin1.getGridServiceManagers().waitForAtLeastOne();
        final GridServiceManager gsm2 = admin2.getGridServiceManagers().waitForAtLeastOne();

        log("deploying PUs");
        deployIsraelSite(gsm1);
        deployLondonSite(gsm2);
        deployIsraelGW(gsm1);
        deployLondonGW(gsm2);

        admin1.getGridServiceContainers().waitFor(3);
        admin2.getGridServiceContainers().waitFor(3);


        Space space1 = admin1.getSpaces().waitFor("israelSpace");
        Space space2 = admin2.getSpaces().waitFor("londonSpace");

        gigaSpace1 = space1.getGigaSpace();
        gigaSpace2 = space2.getGigaSpace();

        txnGigaSpace1 = new GigaSpaceConfigurer(gigaSpace1.getSpace()).transactionManager(ptmISR).clustered(true).gigaSpace();
        txnGigaSpace2 = new GigaSpaceConfigurer(gigaSpace2.getSpace()).transactionManager(ptmLON).clustered(true).gigaSpace();

        log("validating gateway components");


        // Verify delegators & sinks are connected.
        assertGatewayReplicationConnected(space1, 1);
        assertGatewayReplicationConnected(space2, 1);

        log("finished initialziation");
    }

    private void deployLondonGW(GridServiceManager gsm2) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "LONDON");
        props.put("targetGatewayName", "ISRAEL");
        props.put("localClusterUrl", "jini://*/*/londonSpace?groups=" + group2);
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", host2);
        props.put("localGatewayDiscoveryPort", "10002");
        props.put("localGatewayLrmiPort", "7001");
        props.put("targetGatewayHost", host1);
        props.put("targetGatewayDiscoveryPort", "10001");
        props.put("targetGatewayLrmiPort", "7000");
        deployGateway(gsm2, siteDeployment("./apps/gateway/gateway-components", "LONDON_GW", props));
    }

    private void deployIsraelGW(GridServiceManager gsm1) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("targetGatewayName", "LONDON");
        props.put("localClusterUrl", "jini://*/*/israelSpace?groups=" + group1);
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", host1);
        props.put("localGatewayDiscoveryPort", "10001");
        props.put("localGatewayLrmiPort", "7000");
        props.put("targetGatewayHost", host2);
        props.put("targetGatewayDiscoveryPort", "10002");
        props.put("targetGatewayLrmiPort", "7001");
        deployGateway(gsm1, siteDeployment("./apps/gateway/gateway-components", "ISRAEL_GW", props));
    }

    private void deployLondonSite(GridServiceManager gsm2) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "LONDON");
        props.put("gatewayTarget", "ISRAEL");
        props.put("spaceUrl", "/./londonSpace");
        deploySite(gsm2, siteDeployment("./apps/gateway/cluster", "londonSpace", props));
    }

    private void deployIsraelSite(GridServiceManager gsm1) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("gatewayTarget", "LONDON");
        props.put("spaceUrl", "/./israelSpace");
        deploySite(gsm1, siteDeployment("./apps/gateway/cluster", "israelSpace", props));
    }

    private void restartAndWait(Admin admin, String puiName) {
        for (GridServiceContainer gsc : admin.getGridServiceContainers()) {
            for (ProcessingUnitInstance pui : gsc.getProcessingUnitInstances()) {
                if (pui.getName().equals(puiName)) {
                    pui.restartAndWait();
                }
            }
        }
    }

    @SpaceClass
    public static class Stock {

        private Integer id;
        private String info;
        private int version;

        public Stock() {
        }


        public Stock(Integer id) {
            this.id = id;
        }

        public Stock(String info) {
            this.info = info;
        }

        public Stock(Integer id, String info) {
            this.id = id;
            this.info = info;
        }

        @SpaceRouting
        @SpaceId
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getInfo() {
            return info;
        }

        public void setInfo(String info) {
            this.info = info;
        }

        public String toString() {
            return "id[" + id + "] info[" + info + "] version[" + version + "]";
        }

        @SpaceVersion
        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Stock stock = (Stock) o;

            if (version != stock.version) return false;
            if (id != null ? !id.equals(stock.id) : stock.id != null) return false;
            if (info != null ? !info.equals(stock.info) : stock.info != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (info != null ? info.hashCode() : 0);
            result = 31 * result + version;
            return result;
        }
    }


}
