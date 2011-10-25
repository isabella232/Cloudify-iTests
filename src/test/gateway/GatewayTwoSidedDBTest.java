package test.gateway;

import com.gatewayPUs.common.MessageGW;
import com.gatewayPUs.common.Stock;
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
import test.utils.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static test.utils.LogUtils.log;

/**
 * @author sagib
 * @since 8.0.3
 */
public class GatewayTwoSidedDBTest extends AbstractGatewayTest {

    private static final int ENTRY_SIZE = 100;

    private Admin admin1;
    private Admin admin2;
    private GigaSpace gigaSpace2;
    private GigaSpace gigaSpace1;
    private int dbPort;
    private int hsqlId;

    private String group1 = null;
    private String group2 = null;
    private String host1 = null;
    private String host2 = null;

    private GridServiceManager gsm1;
    private GridServiceManager gsm2;

    public GatewayTwoSidedDBTest() {
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
        dbPort = 5000 + (int) (Math.random() * 5000);
        while (dbPort == 7000 || dbPort == 7001)
            dbPort = 5000 + (int) (Math.random() * 5000);
        System.out.println("HSQL port " + dbPort);
    }

    @AfterMethod
    public void tearDown() {
        admin1.getMachines().getMachines()[0].getGridServiceAgent().killByAgentId(hsqlId);
        TeardownUtils.teardownAll(admin1, admin2);
        admin1.close();
        admin2.close();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2", enabled=true)
    public void test() throws Exception {
        initialize();

        log("writeMultiple to " + gigaSpace1.getName() +
                " and asserting replication to DB on " + gigaSpace2.getName());

        Stock[] stockArray = new Stock[ENTRY_SIZE];
        for (int i = 0; i < ENTRY_SIZE; i++) {
            stockArray[i] = new Stock(i);
        }
        gigaSpace1.writeMultiple(stockArray);

        assertSuccessfulOperationCount(ENTRY_SIZE, admin1, admin2);

        TestUtils.repetitive(new Runnable() {
            public void run() {
                try {
                    ResultSet rs = DBUtils.runSQLQuery("select * from Stock", host1, dbPort);
                    for (int i = 0; i < ENTRY_SIZE; i++) {
                        rs.next();
                        Assert.assertEquals(i, rs.getInt("ID"));
                    }

                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }, (int)DEFAULT_TEST_TIMEOUT);

        TestUtils.repetitive(new Runnable() {
            public void run() {
                MessageGW[] msgs1 = gigaSpace1.readMultiple(new MessageGW(true));
                MessageGW[] msgs2 = gigaSpace2.readMultiple(new MessageGW(true));
                AssertUtils.assertEquals(msgs2.length, msgs1.length);
                AssertUtils.assertEquivalenceArrays("readMultiple returned wrong results", msgs2, msgs1);
            }
        }, (int)DEFAULT_TEST_TIMEOUT);


    }

    private void initialize() throws InterruptedException {
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

        gsm1 = admin1.getGridServiceManagers().waitForAtLeastOne();
        gsm2 = admin2.getGridServiceManagers().waitForAtLeastOne();


        log("load HSQL DB on machine - " + admin1.getMachines().getMachines()[0]);
        hsqlId = DBUtils.loadHSQLDB(admin1.getMachines().getMachines()[0], "DB", dbPort);

        Thread.sleep(10000);

        log("deploying PUs");
        deployMirror(admin1);
        deployIsraelSite(admin1);
        deployLondonSite(admin2);
        deployIsraelGW(admin1);
        deployLondonGW(admin2);

        admin1.getGridServiceContainers().waitFor(2);
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
        deployGateway(gsm2, siteDeployment("./apps/gateway/gateway-components", "LONDON_GW", props));
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
        deployGateway(gsm1, siteDeployment("./apps/gateway/gateway-components", "ISRAEL_GW", props));
    }

    private void deployLondonSite(Admin admin) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "LONDON");
        props.put("gatewayTarget", "ISRAEL");
        props.put("spaceUrl", "/./londonSpace");
        deploySite(gsm2, siteDeployment("./apps/gateway/cluster", "londonSpace", props));
    }

    private void deployIsraelSite(Admin admin) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("gatewayTarget", "LONDON");
        props.put("spaceUrl", "/./israelSpace");
        props.put("host", host1);
        props.put("port", String.valueOf(dbPort));
        deploySite(gsm1, sitePrepareAndDeployment("processorAsyncPersistent", props));
    }

    private void deployMirror(Admin admin) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("remoteSpace", "israelSpace");
        props.put("spacename", "mirror-service");
        props.put("host", host1);
        props.put("port", String.valueOf(dbPort));
        deploySite(gsm1, sitePrepareAndDeployment("mirror", props));
    }
}