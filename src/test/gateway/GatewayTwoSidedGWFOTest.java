package test.gateway;

import com.gatewayPUs.common.MessageGW;
import com.gigaspaces.cluster.activeelection.SpaceMode;
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
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import test.utils.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static test.utils.LogUtils.log;

/**
 * @author sagib
 * @since 8.0.3
 */
public class GatewayTwoSidedGWFOTest extends AbstractGatewayTest {

    private static final int ENTRY_SIZE = 100;

    private Admin admin1;
    private Admin admin2;
    private GigaSpace gigaSpace2;
    private GigaSpace gigaSpace1;

    private String group1 = null;
    private String group2 = null;
    private String host1 = null;
    private String host2 = null;

    public GatewayTwoSidedGWFOTest() {
        if (System.getenv("SGTEST_DEV") != null) {
            group1 = "israel-" + getUserName();
            group2 = "london-" + getUserName();
            host1 = "localhost";
            host2 = "localhost";
            System.setProperty("com.gs.jini_lus.groups", group1);
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
        case1();
        case2();
        case3();
        case4();
        case5();
        case6();
        case7();
        case8();
    }

    public void case1() throws Exception {
        log("starting test case 1");
        setUpTestCase();

        undeploy(admin2, "LONDON_GW");
        gigaSpace1.write(new MessageGW(1, "Hello, world!"));


        TestUtils.repetitive(new Runnable() {
            public void run() {
                Assert.assertEquals(null, gigaSpace2.read(new MessageGW()));
            }
        }, (int)DEFAULT_TEST_TIMEOUT);

        deployLondonGW(admin2);

        assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(admin1, admin2);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Assert.assertEquals(gigaSpace1.read(new MessageGW()), gigaSpace2.read(new MessageGW()));
            }
        }, (int)DEFAULT_TEST_TIMEOUT);
    }

    public void case2() throws Exception {
        log("starting test case 2");
        setUpTestCase();

        undeploy(admin1, "ISRAEL_GW");
        gigaSpace1.write(new MessageGW(1, "Hello, world!"));


        TestUtils.repetitive(new Runnable() {
            public void run() {
                Assert.assertEquals(null, gigaSpace2.read(new MessageGW()));
            }
        }, (int)DEFAULT_TEST_TIMEOUT);

        deployIsraelGW(admin1);

        assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(admin1, admin2);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Assert.assertEquals(gigaSpace1.read(new MessageGW()), gigaSpace2.read(new MessageGW()));
            }
        }, (int)DEFAULT_TEST_TIMEOUT);
    }

    public void case3() throws Exception {
        log("starting test case 3");
        setUpTestCase();

        killGSC(admin2, "LONDON_GW");

        gigaSpace1.write(new MessageGW(1, "Hello, world!"));

        assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(admin1, admin2);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Assert.assertEquals(gigaSpace1.read(new MessageGW()), gigaSpace2.read(new MessageGW()));
            }
        }, (int)DEFAULT_TEST_TIMEOUT);
    }

    public void case4() throws Exception {
        log("starting test case 4");
        setUpTestCase();

        killGSC(admin1, "ISRAEL_GW");

        gigaSpace1.write(new MessageGW(1, "Hello, world!"));

        assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(admin1, admin2);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Assert.assertEquals(gigaSpace1.read(new MessageGW()), gigaSpace2.read(new MessageGW()));
            }
        }, (int)DEFAULT_TEST_TIMEOUT);
    }

    public void case5() throws Exception {
        log("starting test case 5");
        setUpTestCase();

        final MessageGW[] msgArray = new MessageGW[ENTRY_SIZE];
        for (int i = 0; i < ENTRY_SIZE; i++) {
            msgArray[i] = new MessageGW(i, "Hello, world!");
        }
        gigaSpace1.writeMultiple(msgArray, Lease.FOREVER, Modifiers.NO_RETURN_VALUE);

        killGSC(admin2, "LONDON_GW");

        for (int i = 0; i < ENTRY_SIZE; i++) {
            msgArray[i].setProcessed(true);
        }

        assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(admin1, admin2);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                MessageGW[] msgs1 = gigaSpace1.readMultiple(new MessageGW(true));
                MessageGW[] msgs2 = gigaSpace2.readMultiple(new MessageGW(true));
                AssertUtils.assertEquivalenceArrays("readMultiple returned wrong results", msgs1, msgs2);
            }
        }, (int)DEFAULT_TEST_TIMEOUT);

    }

    public void case6() throws Exception {
        log("starting test case 6");
        setUpTestCase();

        final MessageGW[] msgArray = new MessageGW[ENTRY_SIZE];
        for (int i = 0; i < ENTRY_SIZE; i++) {
            msgArray[i] = new MessageGW(i, "Hello, world!");
        }
        gigaSpace1.writeMultiple(msgArray, Lease.FOREVER, Modifiers.NO_RETURN_VALUE);

        killGSC(admin1, "ISRAEL_GW");

        for (int i = 0; i < ENTRY_SIZE; i++) {
            msgArray[i].setProcessed(true);
        }

        assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(admin1, admin2);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                MessageGW[] msgs1 = gigaSpace1.readMultiple(new MessageGW(true));
                MessageGW[] msgs2 = gigaSpace2.readMultiple(new MessageGW(true));
                AssertUtils.assertEquivalenceArrays("readMultiple returned wrong results", msgs1, msgs2);
            }
        }, (int)DEFAULT_TEST_TIMEOUT);

    }

    public void case7() throws Exception {
        log("starting test case 7");
        setUpTestCase();

        final MessageGW[] msgArray = new MessageGW[ENTRY_SIZE];
        for (int i = 0; i < ENTRY_SIZE; i++) {
            msgArray[i] = new MessageGW(i, "Hello, world!");
        }
        gigaSpace1.writeMultiple(msgArray, Lease.FOREVER, Modifiers.NO_RETURN_VALUE);

        restartSitePrimaries(admin2, "londonSpace");

        assertPrimaries(admin2, 1);
        assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(admin1, admin2);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                MessageGW[] msgs1 = gigaSpace1.readMultiple(new MessageGW(true));
                MessageGW[] msgs2 = gigaSpace2.readMultiple(new MessageGW(true));
                AssertUtils.assertEquals(msgs1.length, msgs2.length);
                AssertUtils.assertEquivalenceArrays("readMultiple returned wrong results", msgs1, msgs2);
            }
        }, (int)DEFAULT_TEST_TIMEOUT);
    }

    public void case8() throws Exception {
        log("starting test case 8");
        setUpTestCase();

        final MessageGW[] msgArray = new MessageGW[ENTRY_SIZE];
        for (int i = 0; i < ENTRY_SIZE; i++) {
            msgArray[i] = new MessageGW(i, "Hello, world!");
        }
        gigaSpace1.writeMultiple(msgArray, Lease.FOREVER, Modifiers.NO_RETURN_VALUE);

        restartSitePrimaries(admin1, "israelSpace");

        assertPrimaries(admin1, 1);
        assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(admin1, admin2);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                MessageGW[] msgs1 = gigaSpace1.readMultiple(new MessageGW(true));
                MessageGW[] msgs2 = gigaSpace2.readMultiple(new MessageGW(true));
                AssertUtils.assertEquals(msgs1.length, msgs2.length);
                AssertUtils.assertEquivalenceArrays("readMultiple returned wrong results", msgs1, msgs2);
            }
        }, (int)DEFAULT_TEST_TIMEOUT);
    }


    private void initialize() {
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
        deployIsraelSite(admin1);
        deployLondonSite(admin2);
        deployIsraelGW(admin1);
        deployLondonGW(admin2);

        admin1.getGridServiceContainers().waitFor(3);
        admin2.getGridServiceContainers().waitFor(3);


        Space space1 = admin1.getSpaces().waitFor("israelSpace");
        Space space2 = admin2.getSpaces().waitFor("londonSpace");

        gigaSpace1 = space1.getGigaSpace();
        gigaSpace2 = space2.getGigaSpace();

        log("validating gateway components");


        // Verify delegators & sinks are connected.
        assertGatewayReplicationConnected(space1, 1);
        assertGatewayReplicationConnected(space2, 1);

        log("finished initialziation");
    }

    private void deployLondonGW(Admin admin) {
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
        deployGateway(admin.getGridServiceManagers().waitForAtLeastOne(), siteDeployment("./apps/gateway/gateway-components", "LONDON_GW", props));
    }

    private void deployIsraelGW(Admin admin) {
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
        deployGateway(admin.getGridServiceManagers().waitForAtLeastOne(), siteDeployment("./apps/gateway/gateway-components", "ISRAEL_GW", props));
    }

    private void deployLondonSite(Admin admin) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "LONDON");
        props.put("gatewayTarget", "ISRAEL");
        props.put("spaceUrl", "/./londonSpace");
        deploySite(admin.getGridServiceManagers().waitForAtLeastOne(), siteDeployment("./apps/gateway/cluster", "londonSpace", props));
    }

    private void deployIsraelSite(Admin admin) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("gatewayTarget", "LONDON");
        props.put("spaceUrl", "/./israelSpace");
        deploySite(admin.getGridServiceManagers().waitForAtLeastOne(), siteDeployment("./apps/gateway/cluster", "israelSpace", props));
    }

    public void setUpTestCase() {
        gigaSpace1.clear(null);
        gigaSpace2.clear(null);
        assertGatewayReplicationHasNotingToReplicate(admin1, admin2);
    }

    private void killGSC(Admin admin, String puiName) {
        for (GridServiceContainer gsc : admin.getGridServiceContainers()) {
            for (ProcessingUnitInstance pui : gsc.getProcessingUnitInstances()) {
                if (pui.getName().equals(puiName)) {
                    gsc.kill();
                }
            }
        }
    }

    private void undeploy(Admin admin, String puName) {
        for (GridServiceContainer gsc : admin.getGridServiceContainers()) {
            for (ProcessingUnitInstance pui : gsc.getProcessingUnitInstances()) {
                if (pui.getName().equals(puName)) {
                    pui.getProcessingUnit().undeploy();
                }
            }
        }
    }

    private void restartSitePrimaries(Admin admin, String puName) {
        for (ProcessingUnitInstance pui : admin.getProcessingUnits().getProcessingUnit(puName).getInstances()) {
            if (pui.getSpaceInstance().getMode().compareTo(SpaceMode.PRIMARY) == 0) {
                pui.restart();
            }
        }

    }


}