package test.gateway;

import com.j_spaces.core.client.Modifiers;
import com.j_spaces.core.client.SQLQuery;
import net.jini.core.lease.Lease;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import test.data.Stock;
import test.utils.*;

import java.util.*;
import static test.utils.LogUtils.log;

public class TwoSourcesOneTargetSimpleTest extends AbstractGatewayTest {

    protected Admin admin1;
    protected Admin admin2;
    protected Admin admin3;
    protected GigaSpace gigaSpace2;
    protected GigaSpace gigaSpace1;
    protected GigaSpace gigaSpace3;
    protected GridServiceManager gsmNY;
    protected GridServiceManager gsmISR;
    protected GridServiceManager gsmLON;

    protected String group1 = null;
    protected String group2 = null;
    protected String group3 = null;
    protected String host1 = null;
    protected String host2 = null;
    protected String host3 = null;

    private ThreadBarrier barrier;
    private volatile static boolean stopUpdates = false;
    private final int numberOfUpdateThreads = 4;
    private final int numberOfIds = 40000;
    private int odds = 0;
    private int evens = 0;

    public TwoSourcesOneTargetSimpleTest() {
        if (isDevMode()) {
            group1 = "israel-" + getUserName();
            group2 = "london-" + getUserName();
            group3 = "ny-" + getUserName();
            host1 = "localhost";
            host2 = "localhost";
            host3 = "localhost";
        } else {
            group1 = GROUP1;
            group2 = GROUP2;
            group3 = GROUP3;
            host1 = HOST1;
            host2 = HOST2;
            host3 = HOST3;
        }
    }

    @AfterMethod
    public void tearDown() {
        TeardownUtils.teardownAll(admin1, admin2, admin3);
        admin1.close();
        admin2.close();
        admin3.close();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="3", enabled=true)
    public void test() throws Exception {
        initialize();
        
        log("testWriteFromLondonOnly");
        testWriteFromLondonOnly();
        
        log("testWriteFromBothLondonAndIsrael");
        testWriteFromBothLondonAndIsrael();
        
        log("testWriteFormBothLondonAndIsraelStressTest");
        testWriteFormBothLondonAndIsraelStressTest();

    }

    private void testWriteFormBothLondonAndIsraelStressTest() throws Exception {
        setUpTestCase();

        final List<Thread> updateThreads = new ArrayList<Thread>();
        final List<Integer> testDataIds;

        barrier = new ThreadBarrier(numberOfUpdateThreads + 1);
        testDataIds = fillSpace(numberOfIds, gigaSpace1, gigaSpace2);
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
            if (i % 2 == 0) {
                space = gigaSpace2;
                ids = evenDataIds;
            } else {
                space = gigaSpace1;
                ids = oddDataIds;
            }
            final Thread updateThread = new Thread(new Runnable() {
                public void run() {
                    try {
                    	barrier.await();
                        runUpdates(index, space, ids, (int) ((ids.size() / 2.0) / numberOfUpdateThreads));
                        barrier.await();
                    } catch (Throwable t) {
                        barrier.reset(t);
                    }
                }
            });
            updateThread.start();
            updateThreads.add(updateThread);
        }

        Thread.sleep(20000);
        LogUtils.log("stop threads");
        stopUpdates = true;
        barrier.await();
        barrier.await();

        Thread.sleep(5000);

        countOddsAndEvens(gigaSpace3);
        
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Assert.assertEquals(gigaSpace1.count(null) + gigaSpace2.count(null), gigaSpace3.count(null));
                Assert.assertEquals(odds, gigaSpace1.count(null));
                Assert.assertEquals(evens, gigaSpace2.count(null));
            }
        }, (int)DEFAULT_TEST_TIMEOUT);

    }

    private void countOddsAndEvens(GigaSpace gigaSpace) {
        Stock[] stocks = gigaSpace.readMultiple(new Stock());
        for (Stock stock : stocks) {
            if (stock.getStockId() % 2 == 0) {
                evens++;
            } else {
                odds++;
            }
        }

    }

    private List<Integer> fillSpace(int numberOfIds, GigaSpace remoteGigaSpace1, GigaSpace remoteGigaSpace2) {
        List<Integer> testDataIds = new ArrayList<Integer>();
        for (int id = 0; id < numberOfIds; id++) {
            final Stock td = new Stock();
            {
                td.setStockId(id);
                td.setStockName("GS" + id);
            }
            if (id % 2 == 0) {
                remoteGigaSpace2.write(td, Lease.FOREVER, 0, Modifiers.WRITE);
            } else {
                remoteGigaSpace1.write(td, Lease.FOREVER, 0, Modifiers.WRITE);
            }
            testDataIds.add(id);
        }
        return testDataIds;
    }

    private static void runUpdates(final int index, final GigaSpace remoteGigaSpace, final List<Integer> ids, int numberOfTakes) throws InterruptedException {
        System.out.println("runUpdates " + index + " started (size " + ids.size() + ", number of takes " + numberOfTakes + ")");

        while (stopUpdates == false) {
            // SELECT RANDOM OPERATION
            final boolean fullTake = numberOfTakes > 0 && Math.random() < 0.5;

            // SELECT RANDOM ID
            final Integer id;
            synchronized (ids) {
                if (ids.isEmpty()) {
                    // FINISH
                    break;
                } else {
                    if (fullTake) {
                        id = ids.remove(ids.size() - 1);
                    } else {
                        id = ids.get(ids.size() - 1);
                    }
                }
            }

            if (!fullTake) {
                if (Math.random() < 0.7) {
                    final Stock td = new Stock();
                    {
                        td.setStockId(id);
                        td.setStockName("update GS");
                    }
                    remoteGigaSpace.write(td);
                } else {
                    remoteGigaSpace.takeById(Stock.class, id);                }
            } else {
                remoteGigaSpace.takeById(Stock.class, id);
                numberOfTakes--;
            }
        }

        System.out.println("runUpdates " + index + " finished");
    }

    private void testWriteFromBothLondonAndIsrael() {
        setUpTestCase();
        Stock person1 = new Stock(101);
        person1.setStockName("AAAA");
        Stock person2 = new Stock(102);
        person2.setStockName("BBBB");
        gigaSpace1.write(person1);
        gigaSpace2.write(person2);

        TestUtils.repetitive(new Runnable() {
            public void run() {
                SQLQuery<Stock> query1 = new SQLQuery<Stock>(Stock.class, "stockName = ?");
                query1.setParameter(1, "AAAA");
                Assert.assertEquals(gigaSpace3.read(query1), gigaSpace1.read(query1));
                SQLQuery<Stock> query2 = new SQLQuery<Stock>(Stock.class, "stockName = ?");
                query2.setParameter(1, "BBBB");
                Assert.assertEquals(gigaSpace3.read(query2), gigaSpace2.read(query2));
                Assert.assertEquals(2, gigaSpace3.count(null));
                Assert.assertEquals(1, gigaSpace1.count(null));
                Assert.assertEquals(1, gigaSpace2.count(null));

            }
        }, (int)DEFAULT_TEST_TIMEOUT);
    }

    private void testWriteFromLondonOnly() {
        setUpTestCase();
        Stock person1 = new Stock(100);
        person1.setStockName("AAAA");
        gigaSpace2.write(person1);

        TestUtils.repetitive(new Runnable() {
            public void run() {
                SQLQuery<Stock> query = new SQLQuery<Stock>(Stock.class, "stockName = ?");
                query.setParameter(1, "AAAA");
                Assert.assertEquals(gigaSpace3.read(query), gigaSpace2.read(query));
                Assert.assertNull(gigaSpace1.read(query));

            }
        }, (int)DEFAULT_TEST_TIMEOUT);
    }


    private void deployLondonSite(Admin admin) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "LONDON");
        props.put("gatewayTarget", "NY");
        props.put("spaceUrl", "/./londonSpace");
        deploySite(gsmLON, siteDeployment("./apps/gateway/cluster", "londonSpace", props));
    }

    private void deployIsraelSite(Admin admin) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("gatewayTarget", "NY");
        props.put("spaceUrl", "/./israelSpace");
        deploySite(gsmISR, siteDeployment("./apps/gateway/cluster", "israelSpace", props)
                .numberOfInstances(2).numberOfBackups(1).maxInstancesPerVM(1));
    }

    private void deployNYSite(Admin admin) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("spaceUrl", "/./NYSpace");
        deploySite(gsmNY, siteDeployment("./apps/gateway/clusterWithoutTargets", "NYSpace", props)
        .numberOfInstances(2).numberOfBackups(0).maxInstancesPerVM(1));
    }

    private void deployLondonGW(Admin admin) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "LONDON");
        props.put("targetGatewayName", "NY");
        props.put("localClusterUrl", "jini://*/*/londonSpace");
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", gsmLON.getVirtualMachine().getMachine().getHostName());
        props.put("localGatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
        props.put("localGatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
        props.put("targetGatewayHost", gsmNY.getVirtualMachine().getMachine().getHostName());
        props.put("targetGatewayDiscoveryPort", NY_GATEWAY_DISCOVERY_PORT);
        props.put("targetGatewayLrmiPort", NY_GATEWAY_LRMI_PORT);
        deployGateway(gsmLON, siteDeployment("./apps/gateway/gateway-components", "LONDON_GW", props));
    }

    private void deployIsraelGW(Admin admin) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("targetGatewayName", "NY");
        props.put("localClusterUrl", "jini://*/*/israelSpace");
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", gsmISR.getVirtualMachine().getMachine().getHostName());
        props.put("localGatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);
        props.put("localGatewayLrmiPort", ISRAEL_GATEWAY_LRMI_PORT);
        props.put("targetGatewayHost", gsmNY.getVirtualMachine().getMachine().getHostName());
        props.put("targetGatewayDiscoveryPort", NY_GATEWAY_DISCOVERY_PORT);
        props.put("targetGatewayLrmiPort", NY_GATEWAY_LRMI_PORT);
        deployGateway(gsmISR, siteDeployment("./apps/gateway/gateway-components", "ISRAEL_GW", props));
    }

    private void deployNYSink(Admin admin) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "NY");
        props.put("targetGatewayName", "ISRAEL");
        props.put("target2GatewayName", "LONDON");
        props.put("localClusterUrl", "jini://*/*/NYSpace?groups");
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", gsmNY.getVirtualMachine().getMachine().getHostName());
        props.put("localGatewayDiscoveryPort", NY_GATEWAY_DISCOVERY_PORT);
        props.put("localGatewayLrmiPort", NY_GATEWAY_LRMI_PORT);
        props.put("targetGatewayHost", gsmISR.getVirtualMachine().getMachine().getHostName());
        props.put("targetGatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);
        props.put("targetGatewayLrmiPort", ISRAEL_GATEWAY_LRMI_PORT);
        props.put("target2GatewayHost", gsmLON.getVirtualMachine().getMachine().getHostName());
        props.put("target2GatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
        props.put("target2GatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
        deployGateway(gsmNY, siteDeployment("./apps/gateway/gateway2SinksOnly", "NY_SINK", props));
    }


    protected void initComponents() throws Exception {

        log("deploying PUs");
        deployLondonSite(admin2);
        deployIsraelSite(admin1);
        deployNYSite(admin3);

        log("deploying gateway components");
        deployLondonGW(admin2);
        deployIsraelGW(admin1);
        deployNYSink(admin3);

        Space space = admin3.getSpaces().waitFor("NYSpace");
        Space space1 = admin1.getSpaces().waitFor("israelSpace");
        Space space2 = admin2.getSpaces().waitFor("londonSpace");

        gigaSpace3 = space.getGigaSpace();
        gigaSpace1 = space1.getGigaSpace();
        gigaSpace2 = space2.getGigaSpace();

        Thread.sleep(10000);
        // Verify delegators connected.
        assertGatewayReplicationConnected(space1, 1);
        assertGatewayReplicationConnected(space2, 1);
    }


    private void initialize() throws Exception {
        log("starting initialization");

        admin1 = new AdminFactory().addGroups(group1).createAdmin();
        admin2 = new AdminFactory().addGroups(group2).createAdmin();
        admin3 = new AdminFactory().addGroups(group3).createAdmin();
        SetupUtils.assertCleanSetup(admin1);
        SetupUtils.assertCleanSetup(admin2);
        SetupUtils.assertCleanSetup(admin3);

        GridServiceAgent gsaISR = admin1.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent gsaLON = admin2.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent gsaNY = admin3.getGridServiceAgents().waitForAtLeastOne();

        gsaNY.startGridService(new GridServiceManagerOptions());
        gsaNY.startGridService(new GridServiceContainerOptions());
        gsaNY.startGridService(new GridServiceContainerOptions());

        gsaISR.startGridService(new GridServiceManagerOptions());
        gsaISR.startGridService(new GridServiceContainerOptions());
        gsaISR.startGridService(new GridServiceContainerOptions());

        gsaLON.startGridService(new GridServiceManagerOptions());
        gsaLON.startGridService(new GridServiceContainerOptions());
        gsaLON.startGridService(new GridServiceContainerOptions());

        gsmNY = admin3.getGridServiceManagers().waitForAtLeastOne();
        gsmISR = admin1.getGridServiceManagers().waitForAtLeastOne();
        gsmLON = admin2.getGridServiceManagers().waitForAtLeastOne();

        initComponents();

        log("verifying 3 gsc's for each admin");

        admin1.getGridServiceContainers().waitFor(3);
        admin2.getGridServiceContainers().waitFor(3);
        admin3.getGridServiceContainers().waitFor(3);
        
        
        log("finished initialization");

    }

    public void setUpTestCase() {
    	GigaSpace [] spaces = {gigaSpace1, gigaSpace2};
    	Admin [] admins = {admin1, admin2};
    	assertCleanSites(spaces, admins);
        gigaSpace3.clear(null);
        }
}
