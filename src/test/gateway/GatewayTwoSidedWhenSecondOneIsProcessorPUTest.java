package test.gateway;

import com.gatewayPUs.common.MessageGW;
import com.j_spaces.core.client.UpdateModifiers;
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
import test.utils.AssertUtils;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;
import test.utils.TestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static test.utils.LogUtils.log;

/**
 * @author sagib
 * @since 8.0.3
 */
public class GatewayTwoSidedWhenSecondOneIsProcessorPUTest extends AbstractGatewayTest {

    private static final int ENTRY_SIZE = 100;

    private Admin admin1;
    private Admin admin2;
    private GigaSpace gigaSpace2;
    private GigaSpace gigaSpace1;

    private String group1 = null;
    private String group2 = null;
    private String host1 = null;
    private String host2 = null;

    public GatewayTwoSidedWhenSecondOneIsProcessorPUTest() {
        if (isDevMode()) {
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

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="2", enabled=true)
    public void test() throws Exception {
        initialize();
        case1();
        case2();
        case3();
        case4();
        case5();
        case6();
    }

    public void case1() throws Exception {
        log("starting test case 1");
        setUpTestCase();

        gigaSpace1.write(new MessageGW(1, "Hello, world!"));
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Assert.assertEquals(gigaSpace1.read(new MessageGW(true)), gigaSpace2.read(new MessageGW(true)));
            }
        }, 10000);
    }

    public void case2() throws Exception {
        log("starting test case 2");
        setUpTestCase();

        gigaSpace2.write(new MessageGW(1, "Hello, world!"));
        TestUtils.repetitive(new Runnable() {
            public void run() {
                MessageGW msg1 = gigaSpace1.read(new MessageGW(true));
                Assert.assertNotNull(msg1);
                Assert.assertTrue(msg1.isProcessed());

                MessageGW msg2 = gigaSpace2.read(new MessageGW(true));
                Assert.assertNotNull(msg2);
                Assert.assertTrue(msg2.isProcessed());

                Assert.assertEquals(msg2.getId(), msg1.getId());
            }
        }, (int)DEFAULT_TEST_TIMEOUT);
    }

    public void case3() throws Exception {
        log("starting test case 3");
        setUpTestCase();

        final MessageGW[] msgArray = new MessageGW[ENTRY_SIZE];
        for (int i = 0; i < ENTRY_SIZE; i++) {
            msgArray[i] = new MessageGW(i, "Hello, world!");
        }
        gigaSpace1.writeMultiple(msgArray);

        for (int i = 0; i < ENTRY_SIZE; i++) {
            msgArray[i].setProcessed(true);
        }

        TestUtils.repetitive(new Runnable() {
            public void run() {
                MessageGW[] msgs = gigaSpace2.readMultiple(new MessageGW(true));
                AssertUtils.assertEquals(msgArray.length, msgs.length);
                AssertUtils.assertEquivalenceArrays("readMultiple returned wrong results", msgArray, msgs);
            }
        }, (int)DEFAULT_TEST_TIMEOUT);

    }

    public void case4() throws Exception {
        log("starting test case 4");
        setUpTestCase();

        final MessageGW[] msgArray = new MessageGW[ENTRY_SIZE];
        for (int i = 0; i < ENTRY_SIZE; i++) {
            msgArray[i] = new MessageGW(i, "Hello, world!");
        }
        gigaSpace2.writeMultiple(msgArray);

        for (int i = 0; i < ENTRY_SIZE; i++) {
            msgArray[i].setProcessed(true);
        }

        TestUtils.repetitive(new Runnable() {
            public void run() {
                 MessageGW[] msgs = gigaSpace2.readMultiple(new MessageGW(true));
                AssertUtils.assertEquals(msgArray.length, msgs.length);
                AssertUtils.assertEquivalenceArrays("readMultiple returned wrong results", msgArray, msgs);
            }
        }, (int)DEFAULT_TEST_TIMEOUT);
    }

    public void case5() throws Exception {
        log("starting test case 5");
        setUpTestCase();

        gigaSpace1.write(new MessageGW(1, "Hello, world!"));
        TestUtils.repetitive(new Runnable() {
            public void run() {
                final MessageGW result1 = gigaSpace1.read(new MessageGW(true));
                Assert.assertNotNull(result1);
                final MessageGW result2 = gigaSpace2.read(new MessageGW(true));
                Assert.assertNotNull(result2);
                Assert.assertEquals(result1, result2);
            }
        }, (int)DEFAULT_TEST_TIMEOUT);

        MessageGW updated = new MessageGW(1, "updated");
        gigaSpace2.write(updated, Lease.FOREVER, 0, UpdateModifiers.UPDATE_ONLY);

        final MessageGW template = new MessageGW(1, "updated");
        template.setProcessed(true);

        TestUtils.repetitive(new Runnable() {


            public void run() {
                MessageGW msg2 = gigaSpace2.read(template);
                MessageGW msg1 = gigaSpace1.read(template);
                Assert.assertNotNull(msg1);
                Assert.assertNotNull(msg2);
                Assert.assertEquals(msg1, msg2);
                Assert.assertEquals("updated", msg1.getInfo());
                Assert.assertEquals("updated", msg2.getInfo());
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
        gigaSpace2.writeMultiple(msgArray);

        TestUtils.repetitive(new Runnable() {
            public void run() {
                MessageGW[] msgs1 = gigaSpace1.readMultiple(new MessageGW(true));
                MessageGW[] msgs2 = gigaSpace2.readMultiple(new MessageGW(true));
                AssertUtils.assertEquals(msgs2.length, msgs1.length);
                AssertUtils.assertEquivalenceArrays("readMultiple returned wrong results", msgs2, msgs1);
            }
        }, (int)DEFAULT_TEST_TIMEOUT);

        for (int i = 0; i < ENTRY_SIZE; i++) {
            msgArray[i] = new MessageGW(i, "updated");
        }
        gigaSpace2.writeMultiple(msgArray);

        TestUtils.repetitive(new Runnable() {
            public void run() {
                MessageGW[] msgs1 = gigaSpace1.readMultiple(new MessageGW(true));
                MessageGW[] msgs2 = gigaSpace2.readMultiple(new MessageGW(true));
                AssertUtils.assertEquals(msgArray.length, msgs1.length);
                AssertUtils.assertEquivalenceArrays("readMultiple returned wrong results", msgs2, msgs1);
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
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("gatewayTarget", "LONDON");
        props.put("spaceUrl", "/./israelSpace");
        deploySite(gsm1, sitePrepareAndDeployment("processor", props));
        props.clear();

        props.put("localGatewayName", "LONDON");
        props.put("gatewayTarget", "ISRAEL");
        props.put("spaceUrl", "/./londonSpace");
        deploySite(gsm2, siteDeployment("./apps/gateway/cluster", "londonSpace", props));
        props.clear();

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
        deployGateway(gsm1, siteDeployment("./apps/gateway/gateway-components", "ISRAEL", props));
        props.clear();

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
        deployGateway(gsm2, siteDeployment("./apps/gateway/gateway-components", "LONDON", props));
        props.clear();

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

    public void setUpTestCase() {
        gigaSpace1.clear(null);
        gigaSpace2.clear(null);
        assertGatewayReplicationHasNotingToReplicate(admin1, admin2);
    }
}