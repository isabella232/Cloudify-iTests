package test.gateway;

import com.j_spaces.core.client.Modifiers;
import net.jini.core.lease.Lease;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import test.AbstractTest;
import test.data.Stock;
import test.utils.AssertUtils;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;
import test.utils.TestUtils;

import java.util.*;
import static test.utils.LogUtils.log;

/**
 * @author sagib
 * @since 8.0.3
 */
public class GatewayDoubleTwoSidedDelegatedBootstrapTwiceAfterFOTest extends AbstractGatewayTest {

    private Admin adminISR;
    private Admin adminLON;
    private GigaSpace gigaSpaceLON;
    private GigaSpace gigaSpaceISR;

    private String groupISR = null;
    private String groupLON = null;

    private GridServiceManager gsmISR;
    private GridServiceManager gsmLON;
    private GridServiceAgent gsaISR;
    private GridServiceAgent gsaLON;

    private final int numberOfIds = 10;

    protected static final String TEST_LONDON_GATEWAY_DISCOVERY_PORT = "7777";
    protected static final String TEST_LONDON_GATEWAY_LRMI_PORT = "6666";
    protected static final String TEST_ISRAEL_GATEWAY_LRMI_PORT = "4444";
    protected static final String TEST_ISRAEL_GATEWAY_DISCOVERY_PORT = "5555";

    public GatewayDoubleTwoSidedDelegatedBootstrapTwiceAfterFOTest() {
        if (isDevMode()) {
            groupISR = "israel-" + getUserName();
            groupLON = "london-" + getUserName();
        } else {
            groupISR = GROUP1;
            groupLON = GROUP2;
        }

    }

    @AfterMethod
    public void tearDown() {
        TeardownUtils.teardownAll(adminISR, adminLON);
        adminISR.close();
        adminLON.close();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="2", enabled=true)
    public void test() throws Exception {

        initialize();

        final List<Integer> testDataIds;

        testDataIds = fillSpace(numberOfIds, gigaSpaceISR);

        final List<Integer> stockIds = new ArrayList<Integer>();
        for (int i = 0; i < testDataIds.size(); i++) {
            stockIds.add(testDataIds.get(i));

        }

        Thread.sleep(5000);
        Assert.assertEquals(0, gigaSpaceLON.count(null));

        // Verify delegators & sinks are connected.
        Space space1 = adminISR.getSpaces().waitFor("israelSpace");
        Space space2 = adminLON.getSpaces().waitFor("londonSpace");

        for (GridServiceContainer gsc : adminLON.getGridServiceContainers()) {
            gsc.kill();
        }

        gsaLON.startGridService(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.zones=LON-GW-ZONE")
                .vmInputArgument("-Dcom.gs.transport_protocol.lrmi.bind-port="+TEST_LONDON_GATEWAY_LRMI_PORT)
                .vmInputArgument("-Dcom.sun.jini.reggie.initialUnicastDiscoveryPort="+TEST_LONDON_GATEWAY_DISCOVERY_PORT)
                .overrideVmInputArguments());
        gsaLON.startGridService(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.zones=LON-ZONE")
                .overrideVmInputArguments());
        gsaLON.startGridService(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.zones=LON-ZONE")
                .overrideVmInputArguments());

        AbstractTest.assertTrue(adminLON.getGridServiceContainers().waitFor(3));
        adminLON.getProcessingUnits().getProcessingUnit("LONDON").waitFor(adminLON.getProcessingUnits()
                .getProcessingUnit("LONDON").getTotalNumberOfInstances());

        Thread.sleep(30000);

        bootstrap(adminLON, "LONDON", "ISRAEL");

        // Verify delegators & sinks are connected.
        space1 = adminISR.getSpaces().waitFor("israelSpace");
        space2 = adminLON.getSpaces().waitFor("londonSpace");

        assertGatewayReplicationConnected(space1, 1);
        assertGatewayReplicationConnected(space2, 1);


        TestUtils.repetitive(new Runnable() {

            public void run() {
                AssertUtils.assertEquivalenceArrays("", gigaSpaceLON.readMultiple(new Stock()), gigaSpaceISR.readMultiple(new Stock()));
            }

        }, (int)DEFAULT_TEST_TIMEOUT);


    }

    private List<Integer> fillSpace(int numberOfIds, GigaSpace remoteGigaSpace) {
        List<Integer> testDataIds = new ArrayList<Integer>();
        for (int id = 1; id <= numberOfIds; ++id) {
            final Stock td = new Stock();
            {
                td.setStockId(id);
                td.setStockName("GS" + id);
            }
            remoteGigaSpace.write(td, Lease.FOREVER, 0, Modifiers.WRITE);

            testDataIds.add(id);
        }
        return testDataIds;
    }


    public void setUpTestCase() {
        gigaSpaceLON.clear(null);
        gigaSpaceISR.clear(null);

        assertGatewayReplicationHasNotingToReplicate(adminISR, adminLON);
    }


    private void initialize() throws Exception {
        log("initializing..");
        adminISR = new AdminFactory().addGroups(groupISR).createAdmin();
        adminLON = new AdminFactory().addGroups(groupLON).createAdmin();

        SetupUtils.assertCleanSetup(adminISR);
        SetupUtils.assertCleanSetup(adminLON);

        gsaISR = adminISR.getGridServiceAgents().waitForAtLeastOne();
        gsaLON = adminLON.getGridServiceAgents().waitForAtLeastOne();

        gsaISR.startGridService(new GridServiceManagerOptions());
        gsaISR.startGridService(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.zones=ISR-GW-ZONE")
                .vmInputArgument("-Dcom.gs.transport_protocol.lrmi.bind-port=" + TEST_ISRAEL_GATEWAY_LRMI_PORT)
                .vmInputArgument("-Dcom.sun.jini.reggie.initialUnicastDiscoveryPort="+TEST_ISRAEL_GATEWAY_DISCOVERY_PORT)
                .overrideVmInputArguments());
        gsaISR.startGridService(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.zones=ISR-ZONE")
                .overrideVmInputArguments());
        gsaISR.startGridService(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.zones=ISR-ZONE")
                .overrideVmInputArguments());

        gsaLON.startGridService(new GridServiceManagerOptions());
        gsaLON.startGridService(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.zones=LON-GW-ZONE")
                .vmInputArgument("-Dcom.gs.transport_protocol.lrmi.bind-port="+TEST_LONDON_GATEWAY_LRMI_PORT)
                .vmInputArgument("-Dcom.sun.jini.reggie.initialUnicastDiscoveryPort="+TEST_LONDON_GATEWAY_DISCOVERY_PORT)
                .overrideVmInputArguments());
        gsaLON.startGridService(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.zones=LON-ZONE")
                .overrideVmInputArguments());
        gsaLON.startGridService(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.zones=LON-ZONE")
                .overrideVmInputArguments());


        gsmISR = adminISR.getGridServiceManagers().waitForAtLeastOne();
        gsmLON = adminLON.getGridServiceManagers().waitForAtLeastOne();

        log("deploying PUs");
        deployIsraelSite();
        deployLondonSite();
        deployIsraelGW();
        deployLondonGW();

        adminISR.getGridServiceContainers().waitFor(3);
        adminLON.getGridServiceContainers().waitFor(3);

        Space space1 = adminISR.getSpaces().waitFor("israelSpace");
        Space space2 = adminLON.getSpaces().waitFor("londonSpace");


        gigaSpaceISR = space1.getGigaSpace();
        gigaSpaceLON = space2.getGigaSpace();

        log("finished initialziation");

    }

    private void deployLondonGW() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "LONDON");
        props.put("targetGatewayName", "ISRAEL");
        props.put("localClusterUrl", "jini://*/*/londonSpace");
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", gsmLON.getVirtualMachine().getMachine().getHostName());
        props.put("localGatewayDiscoveryPort", TEST_LONDON_GATEWAY_DISCOVERY_PORT);
        props.put("localGatewayLrmiPort", TEST_LONDON_GATEWAY_LRMI_PORT);
        props.put("targetGatewayHost", gsmISR.getVirtualMachine().getMachine().getHostName());
        props.put("targetGatewayDiscoveryPort", TEST_ISRAEL_GATEWAY_DISCOVERY_PORT);
        props.put("targetGatewayLrmiPort", TEST_ISRAEL_GATEWAY_LRMI_PORT);
        deployGateway(gsmLON, siteDeployment("./apps/gateway/gatewayBootstrap", "LONDON-GW", props));
    }


    private void deployIsraelGW() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("targetGatewayName", "LONDON");
        props.put("localClusterUrl", "jini://*/*/israelSpace");
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", gsmISR.getVirtualMachine().getMachine().getHostName());
        props.put("localGatewayDiscoveryPort", TEST_ISRAEL_GATEWAY_DISCOVERY_PORT);
        props.put("localGatewayLrmiPort", TEST_ISRAEL_GATEWAY_LRMI_PORT);
        props.put("targetGatewayHost", gsmLON.getVirtualMachine().getMachine().getHostName());
        props.put("targetGatewayDiscoveryPort", TEST_LONDON_GATEWAY_DISCOVERY_PORT);
        props.put("targetGatewayLrmiPort", TEST_LONDON_GATEWAY_LRMI_PORT);
        deployGateway(gsmISR, siteDeployment("./apps/gateway/gateway-components", "ISRAEL-GW", props));
    }


    private void deployLondonSite() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "LONDON");
        props.put("gatewayTarget", "ISRAEL");
        props.put("spaceUrl", "/./londonSpace");
        props.put("zone", "LON-ZONE");
        deploySite(gsmLON, siteDeployment("./apps/gateway/clusterWithLondonZone", "LONDON", props)
                .numberOfInstances(1).numberOfBackups(1));
    }

    private void deployIsraelSite() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("gatewayTarget", "LONDON");
        props.put("spaceUrl", "/./israelSpace");
        props.put("zone", "ISR-ZONE");
        deploySite(gsmISR, siteDeployment("./apps/gateway/clusterWithIsraelZone", "ISRAEL", props)
                .numberOfInstances(1).numberOfBackups(1));
    }
}