package test.gateway;

import org.junit.Assert;
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
import test.data.Person;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;
import test.utils.TestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class GatewayInDifferentPortTest extends AbstractGatewayTest {

    private Admin admin1;
    private Admin admin2;
    private GigaSpace gigaSpace2;
    private GigaSpace gigaSpace1;

    private String group1 = null;
    private String group2 = null;
    private String host1 = null;
    private String host2 = null;

    public GatewayInDifferentPortTest() {
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

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2", enabled=true)
    public void testGatewayOperations() throws Exception {
        initialize();
        final Person person1 = new Person(1L, "John Doe");
        // israel -> london
        gigaSpace1.write(person1);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Person person11 = gigaSpace2.readById(Person.class, 1L);
                Assert.assertNotNull(person11);
            }
        }, (int)DEFAULT_TEST_TIMEOUT);

        final Person person2 = new Person(2L, "kobi k");
        gigaSpace2.write(person2 );
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Person person22 = gigaSpace1.readById(Person.class, 2L);
                Assert.assertNotNull(person22);
            }
        }, (int)DEFAULT_TEST_TIMEOUT);
    }


    void initialize() {
        admin1 = new AdminFactory().addGroups(group1).createAdmin();
        admin2 = new AdminFactory().addGroups(group2).createAdmin();
        SetupUtils.assertCleanSetup(admin1);
        SetupUtils.assertCleanSetup(admin2);

        GridServiceAgent gsa1 = admin1.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent gsa2 = admin2.getGridServiceAgents().waitForAtLeastOne();

        gsa1.startGridServiceAndWait(new GridServiceManagerOptions());
        gsa1.startGridServiceAndWait(new GridServiceContainerOptions()
                .vmInputArgument("-Dcom.gs.transport_protocol.lrmi.bind-port=6666")
                .vmInputArgument("-Dcom.sun.jini.reggie.initialUnicastDiscoveryPort=9999").overrideVmInputArguments());
        gsa1.startGridServiceAndWait(new GridServiceContainerOptions());
        gsa1.startGridServiceAndWait(new GridServiceContainerOptions());


        gsa2.startGridServiceAndWait(new GridServiceManagerOptions());
        gsa2.startGridServiceAndWait(new GridServiceContainerOptions());
        gsa2.startGridServiceAndWait(new GridServiceContainerOptions());

        final GridServiceManager gsm1 = admin1.getGridServiceManagers().waitForAtLeastOne();
        final GridServiceManager gsm2 = admin2.getGridServiceManagers().waitForAtLeastOne();


        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("gatewayTarget", "LONDON");
        props.put("spaceUrl", "/./israelSpace");
        deploySite(gsm1, siteDeployment("./apps/gateway/cluster", "israelSpace", props));
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
        props.put("localGatewayDiscoveryPort", "9999");
        props.put("localGatewayLrmiPort", "6666");
        props.put("targetGatewayHost", host2);
        props.put("targetGatewayDiscoveryPort", "10002");
        props.put("targetGatewayLrmiPort", "7001");
        deployGateway(gsm1, gatewayDeployment("./apps/gateway/gateway-components", "ISRAEL", props));
        props.clear();

        props.put("localGatewayName", "LONDON");
        props.put("targetGatewayName", "ISRAEL");
        props.put("localClusterUrl", "jini://*/*/londonSpace?groups=" + group2);
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", host2);
        props.put("localGatewayDiscoveryPort", "10002");
        props.put("localGatewayLrmiPort", "7001");
        props.put("targetGatewayHost", host1);
        props.put("targetGatewayDiscoveryPort", "9999");
        props.put("targetGatewayLrmiPort", "6666");
        deployGateway(gsm2, gatewayDeployment("./apps/gateway/gateway-components", "LONDON", props));
        props.clear();

        admin1.getGridServiceContainers().waitFor(3);
        admin2.getGridServiceContainers().waitFor(3);

        Space space1 = admin1.getSpaces().waitFor("israelSpace");
        Space space2 = admin2.getSpaces().waitFor("londonSpace");

        gigaSpace1 = space1.getGigaSpace();
        gigaSpace2 = space2.getGigaSpace();

        // Verify delegators & sinks are connected.
        assertGatewayReplicationConnected(space1, 1);
        assertGatewayReplicationConnected(space2, 1);

    }

    @AfterMethod
    public void tearDown() {
        TeardownUtils.teardownAll(admin1, admin2);
        admin1.close();
        admin2.close();
    }


}
