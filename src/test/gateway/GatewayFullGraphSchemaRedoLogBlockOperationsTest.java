package test.gateway;

import static test.utils.LogUtils.log;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Assert;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.RedoLogCapacityExceededException;
import org.openspaces.core.WriteMultipleException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;
import test.utils.TestUtils;

import com.gatewayPUs.common.Stock;

/**
 * @author sagib
 * @since 8.0.3
 */
public class GatewayFullGraphSchemaRedoLogBlockOperationsTest extends AbstractGatewayTest {

    private static final int CAPACITY = 100 * 1000;
    
    private Admin adminISR;
    private Admin adminLON;
    private Admin adminNY;
    private GigaSpace gigaSpace1;
    private GigaSpace gigaSpace2;
    private GigaSpace gigaSpace3;

    private String groupISR = null;
    private String groupLON = null;
    private String groupNY = null;
    private String hostISR = null;
    private String hostLON = null;
    private String hostNY = null;

    private GridServiceManager gsmNY;
    private GridServiceManager gsmISR;
    private GridServiceManager gsmLON;

    public GatewayFullGraphSchemaRedoLogBlockOperationsTest() {
        if (isDevMode()) {
            groupISR = "israel-" + getUserName();
            groupLON = "london-" + getUserName();
            groupNY = "ny-" + getUserName();
            hostISR = "localhost";
            hostLON = "localhost";
            hostNY = "localhost";
        } else {
            groupISR = GROUP1;
            groupLON = GROUP2;
            groupNY = GROUP3;
            hostISR = HOST1;
            hostLON = HOST2;
            hostNY = HOST3;
        }
    }

    @AfterMethod
    public void tearDown() {
        Admin[] admins = {adminLON, adminNY, adminISR};
        TeardownUtils.teardownAll(admins);
        adminLON.close();
        adminISR.close();
        adminNY.close();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="3", enabled=true)
    public void test() throws Exception {
        initialize();
        int caseNum = 0;

        log("case #" + ++caseNum + " write " + (CAPACITY + 1000) + " to " + gigaSpace1.getName());
        testBootstrapAfterSiteAndGWIsDown();
        testBootstrapAfterGWIsDown();
    }

    private void testBootstrapAfterSiteAndGWIsDown() throws Exception {
        setUpTestCase(gigaSpace1, gigaSpace2);
        Stock[] stocks = new Stock[CAPACITY];
        for (int i = 0; i < stocks.length; i++) {
            stocks[i] = new Stock(i);
        }
        gigaSpace1.writeMultiple(stocks);
        stocks = new Stock[1000];
        for (int i = 0; i < stocks.length; i++) {
            stocks[i] = new Stock(i);
        }
        try {
            gigaSpace1.writeMultiple(stocks);
        } catch (Exception e) {
            Assert.assertTrue(((WriteMultipleException) e).getResults()[0].getError() instanceof RedoLogCapacityExceededException);
        }

        Thread.sleep(5000);
        log("asserting replicated to " + gigaSpace2.getName());
        TestUtils.repetitive(new Runnable() {

            public void run() {
                Assert.assertEquals(CAPACITY, gigaSpace2.count(new Stock()));
            }

        }, (int)DEFAULT_TEST_TIMEOUT);

        log("deploying NY site");
        gsmNY = adminNY.getGridServiceManagers().waitForAtLeastOne();
        deployNYSite();
        deployNYGW();
        Space space3 = adminNY.getSpaces().waitFor("nySpace");
        gigaSpace3 = space3.getGigaSpace();

        Thread.sleep(30000);

        log("asserting replicated to " + gigaSpace3.getName());
        TestUtils.repetitive(new Runnable() {

            public void run() {
                Assert.assertEquals(CAPACITY, gigaSpace3.count(new Stock()));
                Assert.assertEquals(CAPACITY, gigaSpace2.count(new Stock()));
            }

        }, (int)DEFAULT_TEST_TIMEOUT);
    }

    private void testBootstrapAfterGWIsDown() throws Exception {
        setUpTestCase(gigaSpace1, gigaSpace2, gigaSpace3);
        undeployNYGW();
        Stock[] stocks = new Stock[CAPACITY];
        for (int i = 0; i < stocks.length; i++) {
            stocks[i] = new Stock(i);
        }
        gigaSpace1.writeMultiple(stocks);

        stocks = new Stock[1000];
        for (int i = 0; i < stocks.length; i++) {
            stocks[i] = new Stock(i);
        }
        try {
            gigaSpace1.writeMultiple(stocks);
        } catch (Exception e) {
            Assert.assertTrue(((WriteMultipleException) e).getResults()[0].getError() instanceof RedoLogCapacityExceededException);
        }

        Thread.sleep(5000);

        log("asserting replicated to " + gigaSpace2.getName());
        TestUtils.repetitive(new Runnable() {

            public void run() {
                Assert.assertEquals(CAPACITY, gigaSpace2.count(new Stock()));
            }

        }, OPERATION_TIMEOUT);

        log("deploying NY GW");
        deployNYGW();
        gsmNY = adminNY.getGridServiceManagers().waitForAtLeastOne();


        Space space3 = adminNY.getSpaces().waitFor("nySpace");
        gigaSpace3 = space3.getGigaSpace();

        Thread.sleep(30000);

        log("asserting replicated to " + gigaSpace3.getName());
        TestUtils.repetitive(new Runnable() {

            public void run() {
                Assert.assertEquals(CAPACITY, gigaSpace3.count(new Stock()));
                Assert.assertEquals(CAPACITY, gigaSpace2.count(new Stock()));
            }

        }, (int)DEFAULT_TEST_TIMEOUT);
    }


    private void initialize() {
        log("initializing..");
        adminISR = new AdminFactory().addGroups(groupISR).createAdmin();
        adminLON = new AdminFactory().addGroups(groupLON).createAdmin();
        adminNY = new AdminFactory().addGroups(groupNY).createAdmin();
        SetupUtils.assertCleanSetup(adminISR);
        SetupUtils.assertCleanSetup(adminLON);
        SetupUtils.assertCleanSetup(adminNY);

        GridServiceAgent gsaISR = adminISR.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent gsaLON = adminLON.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent gsaNY = adminNY.getGridServiceAgents().waitForAtLeastOne();

        gsaISR.startGridService(new GridServiceManagerOptions());
        gsaISR.startGridService(new GridServiceContainerOptions());
        gsaISR.startGridService(new GridServiceContainerOptions());

        gsaLON.startGridService(new GridServiceManagerOptions());
        gsaLON.startGridService(new GridServiceContainerOptions());
        gsaLON.startGridService(new GridServiceContainerOptions());

        gsaNY.startGridService(new GridServiceManagerOptions());
        gsaNY.startGridService(new GridServiceContainerOptions());
        gsaNY.startGridService(new GridServiceContainerOptions());

        gsmISR = adminISR.getGridServiceManagers().waitForAtLeastOne();
        AbstractTest.assertNotNull(gsmISR);
        gsmLON = adminLON.getGridServiceManagers().waitForAtLeastOne();
        AbstractTest.assertNotNull(gsmLON);
        gsmNY = adminNY.getGridServiceManagers().waitForAtLeastOne();
        AbstractTest.assertNotNull(gsmNY);

        log("deploying PUs");
        deployIsraelSite();
        deployLondonSite();
        deployIsraelGW();
        deployLondonGW();

        AbstractTest.assertTrue(adminISR.getGridServiceContainers().waitFor(3));
        AbstractTest.assertTrue(adminLON.getGridServiceContainers().waitFor(3));

        Space space1 = adminISR.getSpaces().waitFor("israelSpace");
        Space space2 = adminLON.getSpaces().waitFor("londonSpace");

        gigaSpace1 = space1.getGigaSpace();
        gigaSpace2 = space2.getGigaSpace();

        log("finished initialziation");

    }

    private void deployLondonGW() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "LONDON");
        props.put("targetGatewayName", "ISRAEL");
        props.put("target2GatewayName", "NY");
        props.put("localClusterUrl", "jini://*/*/londonSpace");
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", hostLON);
        props.put("localGatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
        props.put("localGatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
        props.put("targetGatewayHost", hostISR);
        props.put("targetGatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);
        props.put("targetGatewayLrmiPort", ISRAEL_GATEWAY_LRMI_PORT);
        props.put("target2GatewayHost", hostNY);
        props.put("target2GatewayDiscoveryPort", NY_GATEWAY_DISCOVERY_PORT);
        props.put("target2GatewayLrmiPort", NY_GATEWAY_LRMI_PORT);
        deployGateway(gsmLON, siteDeployment("./apps/gateway/gatewayDoubleLink", "LONDON", props));
    }

    private void deployIsraelGW() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("targetGatewayName", "LONDON");
        props.put("target2GatewayName", "NY");
        props.put("localClusterUrl", "jini://*/*/israelSpace");
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", hostISR);
        props.put("localGatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);
        props.put("localGatewayLrmiPort", ISRAEL_GATEWAY_LRMI_PORT);
        props.put("targetGatewayHost", hostLON);
        props.put("targetGatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
        props.put("targetGatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
        props.put("target2GatewayHost", hostNY);
        props.put("target2GatewayDiscoveryPort", NY_GATEWAY_DISCOVERY_PORT);
        props.put("target2GatewayLrmiPort", NY_GATEWAY_LRMI_PORT);
        deployGateway(gsmISR, siteDeployment("./apps/gateway/gatewayDoubleLink", "ISRAEL", props));
    }

    private void deployLondonSite() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "LONDON");
        props.put("gatewayTarget", "ISRAEL");
        props.put("gatewayTarget2", "NY");
        props.put("spaceUrl", "/./londonSpace");
        deploySite(gsmLON, siteDeployment("./apps/gateway/clusterWith2targets", "londonSpace", props));
    }

    private void deployIsraelSite() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("gatewayTarget", "LONDON");
        props.put("gatewayTarget2", "NY");
        props.put("spaceUrl", "/./israelSpace");
        deploySite(gsmISR, siteDeployment("./apps/gateway/clusterRedoLogBlockOperations", "israelSpace", props));
    }

    private void deployNYSite() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "NY");
        props.put("gatewayTarget", "ISRAEL");
        props.put("gatewayTarget2", "LONDON");
        props.put("spaceUrl", "/./nySpace");
        deploySite(gsmNY, siteDeployment("./apps/gateway/clusterWith2targets", "nySpace", props));
    }

    private void undeployNYGW() {
        gsmNY.undeploy("NY-GW");
    }

    private void deployNYGW() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "NY");
        props.put("targetGatewayName", "ISRAEL");
        props.put("target2GatewayName", "LONDON");
        props.put("localClusterUrl", "jini://*/*/nySpace");
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", hostNY);
        props.put("localGatewayDiscoveryPort", NY_GATEWAY_DISCOVERY_PORT);
        props.put("localGatewayLrmiPort", NY_GATEWAY_LRMI_PORT);
        props.put("targetGatewayHost", hostISR);
        props.put("targetGatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);
        props.put("targetGatewayLrmiPort", ISRAEL_GATEWAY_LRMI_PORT);
        props.put("target2GatewayHost", hostLON);
        props.put("target2GatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
        props.put("target2GatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
        deployGateway(gsmNY, siteDeployment("./apps/gateway/gatewayDoubleLink", "NY-GW", props));
        props.clear();
    }

    public void setUpTestCase(GigaSpace... gigaSpaces) {
        for (GigaSpace gigaSpace : gigaSpaces) {
            gigaSpace.clear(null);
        }
        assertGatewayReplicationHasNotingToReplicate(adminISR, adminLON, adminNY);
    }
}