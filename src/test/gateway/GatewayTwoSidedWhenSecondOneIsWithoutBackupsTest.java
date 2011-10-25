package test.gateway;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static test.utils.LogUtils.log;

/**
 * @author sagib
 * @since 8.0.3
 */
public class GatewayTwoSidedWhenSecondOneIsWithoutBackupsTest extends AbstractGatewayTest {

    private Admin admin1;
    private Admin admin2;
    private GigaSpace gigaSpace2;
    private GigaSpace gigaSpace1;

    private String group1 = null;
    private String group2 = null;
    private String host1 = null;
    private String host2 = null;

    public GatewayTwoSidedWhenSecondOneIsWithoutBackupsTest() {
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
        GigaSpace[] spaces = {gigaSpace1, gigaSpace2};
        GigaSpace[] targets = {gigaSpace2};
        Admin[] admins = {admin1, admin2};

        int caseNum = 0;

        log("case #" + ++caseNum + " write");
        write(gigaSpace1, targets, false);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " writeMultiple");
        writeMultiple(gigaSpace1, targets, false);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " update");
        update(gigaSpace1, targets, false);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " updateMultiple");
        updateMultiple(gigaSpace1, targets, false);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " take");
        take(gigaSpace1, targets, false);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " takeMultiple");
        takeMultiple(gigaSpace1, targets, false);

       targets = new GigaSpace[]{gigaSpace1};

        log("case #" + ++caseNum + " write");
        write(gigaSpace2, targets, false);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " writeMultiple");
        writeMultiple(gigaSpace2, targets, false);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " update");
        update(gigaSpace2, targets, false);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " updateMultiple");
        updateMultiple(gigaSpace2, targets, false);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " take");
        take(gigaSpace2, targets, false);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " takeMultiple");
        takeMultiple(gigaSpace2, targets, false);
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
        deploySite(gsm1, siteDeployment("./apps/gateway/clusterNoBackups", "israelSpace", props));
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
        deployGateway(gsm1, siteDeployment("./apps/gateway/gateway-components", "ISRAEL_GW", props));
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
        deployGateway(gsm2, siteDeployment("./apps/gateway/gateway-components", "LONDON_GW", props));
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