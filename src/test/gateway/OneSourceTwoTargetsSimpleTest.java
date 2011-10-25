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

import test.utils.SetupUtils;
import test.utils.TeardownUtils;
import test.utils.TestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static test.utils.LogUtils.log;

public class OneSourceTwoTargetsSimpleTest extends AbstractGatewayTest {

    protected Admin admin1;
    protected Admin admin2;
    protected Admin admin3;
    protected GigaSpace gigaSpace2;
    protected GigaSpace gigaSpace1;
    protected GigaSpace gigaSpace3;
    protected GridServiceManager gsm1;
    protected GridServiceManager gsm2;
    protected GridServiceManager gsm3;

    protected String group1 = null;
    protected String group2 = null;
    protected String group3 = null;
    protected String host1 = null;
    protected String host2 = null;
    protected String host3 = null;

    public OneSourceTwoTargetsSimpleTest() {
        if (System.getenv("SGTEST_DEV") != null) {
            group1 = "israel-" + getUserName();
            group2 = "london-" + getUserName();
        	group3 = "ny-" + getUserName();
            host1 = "localhost";
            host2 = "localhost";
            host3 = "localhost";
            System.setProperty("com.gs.jini_lus.groups", group3);
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
        initPUs();
        initGatewayComponents();

        case1();
        case2();
        case3();
        case4();
        case5();
    }

    public void case1() throws Exception {
        gigaSpace1.clear(null);
        gigaSpace2.clear(null);
        gigaSpace3.clear(null);

        log("starting test case 1");
        gigaSpace3.write(new MessageGW(1, "Hello, world!"));
        log("reading from: " + gigaSpace2);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Assert.assertEquals(gigaSpace3.read(new MessageGW()), gigaSpace2.read(new MessageGW()));
                Assert.assertEquals(gigaSpace3.read(new MessageGW()), gigaSpace1.read(new MessageGW()));
            }
        }, (int)DEFAULT_TEST_TIMEOUT);
    }

    public void case2() throws Exception {
        gigaSpace1.clear(null);
        gigaSpace2.clear(null);
        gigaSpace3.clear(null);
        log("starting test case 2");
        gigaSpace1.write(new MessageGW(1, "Hello, world!"));
        Thread.sleep(2000);

        Assert.assertNull(gigaSpace3.read(new MessageGW()));
        Assert.assertNull(gigaSpace2.read(new MessageGW()));

    }

    public void case3() throws Exception {
        gigaSpace1.clear(null);
        gigaSpace2.clear(null);
        gigaSpace3.clear(null);

        log("starting test case 3");
        gigaSpace2.write(new MessageGW(1, "Hello, world!"));
        Thread.sleep(2000);

        Assert.assertNull(gigaSpace3.read(new MessageGW()));
        Assert.assertNull(gigaSpace1.read(new MessageGW()));

    }

    public void case4() throws Exception {
        gigaSpace1.clear(null);
        gigaSpace2.clear(null);
        gigaSpace3.clear(null);

        log("starting test case 4");
        gigaSpace3.write(new MessageGW(1, "Hello, world!"));
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Assert.assertEquals(gigaSpace3.read(new MessageGW()), gigaSpace2.read(new MessageGW()));
                Assert.assertEquals(gigaSpace3.read(new MessageGW()), gigaSpace1.read(new MessageGW()));
            }
        }, 10000);
        MessageGW msg = gigaSpace2.read(new MessageGW());
        msg.setProcessed(true);
        gigaSpace2.write(msg, Lease.FOREVER, 0, UpdateModifiers.UPDATE_ONLY);

        TestUtils.repetitive(new Runnable() {
            public void run() {
                MessageGW msg = gigaSpace3.read(new MessageGW());
                Assert.assertNotNull(msg);
                Assert.assertEquals(false, msg.isProcessed());

                MessageGW msg1 = gigaSpace1.read(new MessageGW());
                Assert.assertNotNull(msg1);
                Assert.assertEquals(false, msg1.isProcessed());

                MessageGW msg2 = gigaSpace2.read(new MessageGW(true));
                Assert.assertNotNull(msg2);
                Assert.assertEquals(true, msg2.isProcessed());
            }
        }, (int)DEFAULT_TEST_TIMEOUT);
    }

    public void case5() throws Exception {
        gigaSpace1.clear(null);
        gigaSpace2.clear(null);
        gigaSpace3.clear(null);

        log("starting test case 5");
        gigaSpace3.write(new MessageGW(1, "Hello, world!"));
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Assert.assertEquals(gigaSpace3.read(new MessageGW()), gigaSpace2.read(new MessageGW()));
                Assert.assertEquals(gigaSpace3.read(new MessageGW()), gigaSpace1.read(new MessageGW()));
            }
        }, (int)DEFAULT_TEST_TIMEOUT);
        MessageGW msg = gigaSpace3.read(new MessageGW());
        msg.setProcessed(true);
        gigaSpace3.write(msg, Lease.FOREVER, 0, UpdateModifiers.UPDATE_ONLY);

        TestUtils.repetitive(new Runnable() {
            public void run() {
                MessageGW msg = gigaSpace3.read(new MessageGW(true));
                Assert.assertNotNull(msg);
                Assert.assertEquals(true, msg.isProcessed());

                MessageGW msg1 = gigaSpace1.read(new MessageGW(true));
                Assert.assertNotNull(msg1);
                Assert.assertEquals(true, msg1.isProcessed());

                MessageGW msg2 = gigaSpace2.read(new MessageGW(true));
                Assert.assertNotNull(msg2);
                Assert.assertEquals(true, msg2.isProcessed());
            }
        }, (int)DEFAULT_TEST_TIMEOUT);
    }

    protected void initPUs() {
        log("deploying PUs");
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "NY");
        props.put("gatewayTarget", "ISRAEL");
        props.put("gatewayTarget2", "LONDON");
        props.put("spaceUrl", "/./NYSpace");
        deploySite(gsm3, siteDeployment("./apps/gateway/clusterWith2targets", "NYSpace", props));
        props.clear();

        props.put("spaceUrl", "/./israelSpace");
        deploySite(gsm1, siteDeployment("./apps/gateway/clusterWithoutTargets", "ISRAELSpace", props));
        props.clear();

        props.put("spaceUrl", "/./londonSpace");
        deploySite(gsm2, siteDeployment("./apps/gateway/clusterWithoutTargets", "LONDONSpace", props));
        props.clear();

    }


    protected void initGatewayComponents() {
        log("deploying gateway components");

        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "NY");
        props.put("targetGatewayName", "LONDON");
         props.put("target2GatewayName", "ISRAEL");
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", host3);
        props.put("localGatewayDiscoveryPort", "10000");
        props.put("localGatewayLrmiPort", "7000");
        props.put("target2GatewayHost", host1);
        props.put("target2GatewayDiscoveryPort", "10001");
        props.put("target2GatewayLrmiPort", "7001");
        props.put("targetGatewayHost", host2);
        props.put("targetGatewayDiscoveryPort", "10002");
        props.put("targetGatewayLrmiPort", "7002");
        deployGateway(gsm3, siteDeployment("./apps/gateway/gatewayDelegatorWith2TargetsOnly", "NY_DELEGATOR", props));
        props.clear();

        props.put("localGatewayName", "ISRAEL");
        props.put("targetGatewayName", "NY");
        props.put("localClusterUrl", "jini://*/*/israelSpace");
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", host1);
        props.put("localGatewayDiscoveryPort", "10001");
        props.put("localGatewayLrmiPort", "7001");
        props.put("targetGatewayHost", host3);
        props.put("targetGatewayDiscoveryPort", "10000");
        props.put("targetGatewayLrmiPort", "7000");
        deployGateway(gsm1, siteDeployment("./apps/gateway/gatewaySinkOnly", "ISR_SINK", props));
        props.clear();

        props.put("localGatewayName", "LONDON");
        props.put("targetGatewayName", "NY");
        props.put("localClusterUrl", "jini://*/*/londonSpace");
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", host2);
        props.put("localGatewayDiscoveryPort", "10002");
        props.put("localGatewayLrmiPort", "7002");
        props.put("targetGatewayHost", host3);
        props.put("targetGatewayDiscoveryPort", "10000");
        props.put("targetGatewayLrmiPort", "7000");
        deployGateway(gsm2, siteDeployment("./apps/gateway/gatewaySinkOnly", "LON_SINK", props));
        props.clear();

        Space space = admin3.getSpaces().waitFor("NYSpace");
        Space space1 = admin1.getSpaces().waitFor("israelSpace");
        Space space2 = admin2.getSpaces().waitFor("londonSpace");

        gigaSpace3 = space.getGigaSpace();
        gigaSpace1 = space1.getGigaSpace();
        gigaSpace2 = space2.getGigaSpace();


        // Verify delegators connected.
        assertGatewayReplicationConnected(space, 2);
    }


    private void initialize() {
        log("starting initialization");

        admin1 = new AdminFactory().addGroups(group1).createAdmin();
        admin2 = new AdminFactory().addGroups(group2).createAdmin();
        admin3 = new AdminFactory().addGroups(group3).createAdmin();
        SetupUtils.assertCleanSetup(admin1);
        SetupUtils.assertCleanSetup(admin2);
        SetupUtils.assertCleanSetup(admin3);

        GridServiceAgent gsa = admin3.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent gsa1 = admin1.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent gsa2 = admin2.getGridServiceAgents().waitForAtLeastOne();

        gsa.startGridService(new GridServiceManagerOptions());
        gsa.startGridService(new GridServiceContainerOptions());
        gsa.startGridService(new GridServiceContainerOptions());
        gsa.startGridService(new GridServiceContainerOptions());

        gsa1.startGridService(new GridServiceManagerOptions());
        gsa1.startGridService(new GridServiceContainerOptions());
        gsa1.startGridService(new GridServiceContainerOptions());
        gsa1.startGridService(new GridServiceContainerOptions());

        gsa2.startGridService(new GridServiceManagerOptions());
        gsa2.startGridService(new GridServiceContainerOptions());
        gsa2.startGridService(new GridServiceContainerOptions());
        gsa2.startGridService(new GridServiceContainerOptions());

        gsm3 = admin3.getGridServiceManagers().waitForAtLeastOne();
        gsm1 = admin1.getGridServiceManagers().waitForAtLeastOne();
        gsm2 = admin2.getGridServiceManagers().waitForAtLeastOne();

        log("verifying 3 gsc's for each admin");

        admin1.getGridServiceContainers().waitFor(3);
        admin2.getGridServiceContainers().waitFor(3);
        admin3.getGridServiceContainers().waitFor(3);

    }
}
