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
 *
 * @author sagib
 * @since 8.0.3
 */
public class GatewayRoutingSchemaWithProcessorTest extends AbstractGatewayTest {

    private Admin admin1;
    private Admin admin2;
    private Admin admin3;
    private GigaSpace gigaSpace1;
    private GigaSpace gigaSpace2;
    private GigaSpace gigaSpace3;
    
    private String group1 = null;
    private String group2 = null;
    private String group3 = null;
    private String host1 = null;
    private String host2 = null;
    private String host3 = null;

    public GatewayRoutingSchemaWithProcessorTest() {
        if (isDevMode()) {
            group1 = "israel-" + getUserName();
            group2 = "london-" + getUserName();
            group3 = "ny-" + getUserName();
            host1 = "localhost";
            host2 = "localhost";
            host3 = "localhost";
            System.setProperty("com.gs.jini_lus.groups", group1);
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
    	Admin [] admins = {admin1, admin2, admin3};
        TeardownUtils.teardownAll(admins);
        admin1.close();
        admin2.close();
        admin3.close();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "3", enabled=true)
    public void test() throws Exception {
        initialize();
        GigaSpace [] toClear = {gigaSpace1, gigaSpace2, gigaSpace3};
        GigaSpace [] targets = {gigaSpace3};
        Admin [] admins = {admin1, admin2, admin3};
        
        int caseNum = 0;
        
        log("case #" + ++caseNum);
        write(gigaSpace1, targets, false);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        writeMultiple(gigaSpace1, targets, false);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        update(gigaSpace1, targets, false);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        updateMultiple(gigaSpace1, targets, false);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        take(gigaSpace1, targets, false);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        takeMultiple(gigaSpace1, targets, false);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        write(gigaSpace2, targets, true);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        writeMultiple(gigaSpace2, targets, true);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        update(gigaSpace2, targets, true);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        updateMultiple(gigaSpace2, targets, true);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        take(gigaSpace2, targets, true);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        takeMultiple(gigaSpace2, targets, true);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        targets[0] = gigaSpace2;
        
        log("case #" + ++caseNum);
        writeNotExpectingReplication(gigaSpace1, targets, false);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        updateNotExpectingReplication(gigaSpace1, targets, false);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        writeMultipleNotExpectingReplication(gigaSpace1, targets, false);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        updateMultipleNotExpectingReplication(gigaSpace1, targets, false);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
       
        targets[0] = gigaSpace1;
        
        log("case #" + ++caseNum);
        writeNotExpectingReplication(gigaSpace3, targets, false);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        updateNotExpectingReplication(gigaSpace3, targets, false);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        writeMultipleNotExpectingReplication(gigaSpace3, targets, false);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
        log("case #" + ++caseNum);
        updateMultipleNotExpectingReplication(gigaSpace3, targets, false);
        clearGigaSpaces(toClear);
        assertGatewayReplicationHasNotingToReplicate(admins);
        
    }



    private void initialize() {
        log("initializing..");
        admin1 = new AdminFactory().addGroups(group1).createAdmin();
        admin2 = new AdminFactory().addGroups(group2).createAdmin();
        admin3 = new AdminFactory().addGroups(group3).createAdmin();
        SetupUtils.assertCleanSetup(admin1);
        SetupUtils.assertCleanSetup(admin2);
        SetupUtils.assertCleanSetup(admin3);

        GridServiceAgent gsa1 = admin1.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent gsa2 = admin2.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent gsa3 = admin3.getGridServiceAgents().waitForAtLeastOne();

        gsa1.startGridService(new GridServiceManagerOptions());
        gsa1.startGridService(new GridServiceContainerOptions());
        gsa1.startGridService(new GridServiceContainerOptions());

        gsa2.startGridService(new GridServiceManagerOptions());
        gsa2.startGridService(new GridServiceContainerOptions());
        gsa2.startGridService(new GridServiceContainerOptions());

        gsa3.startGridService(new GridServiceManagerOptions());
        gsa3.startGridService(new GridServiceContainerOptions());
        gsa3.startGridService(new GridServiceContainerOptions());

        final GridServiceManager gsm1 = admin1.getGridServiceManagers().waitForAtLeastOne();
        final GridServiceManager gsm2 = admin2.getGridServiceManagers().waitForAtLeastOne();
        final GridServiceManager gsm3 = admin3.getGridServiceManagers().waitForAtLeastOne();

        log("deploying PUs");
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("gatewayTarget", "NY");
        props.put("spaceUrl", "/./israelSpace");
        deploySite(gsm1, siteDeployment("./apps/gateway/cluster", "israelSpace", props));
        props.clear();

        props.put("localGatewayName", "LONDON");
        props.put("gatewayTarget", "NY");
        props.put("spaceUrl", "/./londonSpace");
        deploySite(gsm2, sitePrepareAndDeployment("processor", props));
        props.clear();
        
        props.put("localGatewayName", "NY");
        props.put("spaceUrl", "/./nySpace");
        deploySite(gsm3, siteDeployment("./apps/gateway/clusterWithoutTargets", "nySpace", props));
        props.clear();

        props.put("localGatewayName", "ISRAEL");
        props.put("targetGatewayName", "NY");
        props.put("localClusterUrl", "jini://*/*/israelSpace?groups=" + group1);
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", host1);
        props.put("localGatewayDiscoveryPort", "10001");
        props.put("localGatewayLrmiPort", "7000");
        props.put("targetGatewayHost", host2);
        props.put("targetGatewayDiscoveryPort", "10002");
        props.put("targetGatewayLrmiPort", "7001");
        props.put("targetDelegateThrough", "LONDON");
        deployGateway(gsm1, siteDeployment("./apps/gateway/gatewayRoutingComponentsWithRelocate", "ISRAEL", props));
        props.clear();

        props.put("localGatewayName", "LONDON");
        props.put("targetGatewayName", "NY");
        props.put("localClusterUrl", "jini://*/*/londonSpace?groups=" + group2);
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", host2);
        props.put("localGatewayDiscoveryPort", "10002");
        props.put("localGatewayLrmiPort", "7001");
        props.put("targetGatewayHost", host3);
        props.put("targetGatewayDiscoveryPort", "10003");
        props.put("targetGatewayLrmiPort", "7002");
        deployGateway(gsm2, siteDeployment("./apps/gateway/gatewayDelegatorOnly", "LONDON", props));
        props.clear();
        
        props.put("localGatewayName", "NY");
        props.put("targetGatewayName", "LONDON");
        props.put("target2GatewayName", "ISRAEL");
        props.put("localClusterUrl", "jini://*/*/nySpace?groups=" + group2);
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", host3);
        props.put("localGatewayDiscoveryPort", "10003");
        props.put("localGatewayLrmiPort", "7002");
        props.put("targetGatewayHost", host2);
        props.put("targetGatewayDiscoveryPort", "10002");
        props.put("targetGatewayLrmiPort", "7001");
        props.put("target2GatewayHost", host1);
        props.put("target2GatewayDiscoveryPort", "10001");
        props.put("target2GatewayLrmiPort", "7000");
        deployGateway(gsm3, siteDeployment("./apps/gateway/gatewayTwoSourcesSinkOnly", "NY", props));
        props.clear();

        admin1.getGridServiceContainers().waitFor(3);
        admin2.getGridServiceContainers().waitFor(3);
        admin3.getGridServiceContainers().waitFor(3);

        Space space1 = admin1.getSpaces().waitFor("israelSpace");
        Space space2 = admin2.getSpaces().waitFor("londonSpace");
        Space space3 = admin3.getSpaces().waitFor("nySpace");

        gigaSpace1 = space1.getGigaSpace();
        gigaSpace2 = space2.getGigaSpace();
        gigaSpace3 = space3.getGigaSpace();

        log("validating gateway components");

        // Verify delegators & sinks are connected.
        assertGatewayReplicationConnected(space1, 1);
        assertGatewayReplicationConnected(space2, 1);

        log("finished initialziation");

    }
}