package test.gateway.security;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.UrlSpaceConfigurer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.data.Person;
import test.gateway.AbstractGatewayTest;
import test.utils.LogUtils;
import test.utils.ScriptUtils;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;
import test.utils.TestUtils;

/**
 * 
 *
 * @author idan
 * @since 8.0.4
 */
public class GatewaySecuredClustersTest extends AbstractGatewayTest {
    private Admin admin1;
    private Admin admin2;
    private GigaSpace gigaSpace1;
    private GigaSpace gigaSpace2;

    private String group1 = null;
    private String group2 = null;
    private String host1 = null;
    private String host2 = null;
    private GridServiceManager gsm1;
    private GridServiceManager gsm2;
    
    private static final String USERNAME = "Master";
    private static final String PASSWORD = "master";

    public GatewaySecuredClustersTest() {
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

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
    public void test() throws Exception {
        initialize();
        
        LogUtils.log("Test started.");
        Thread.sleep(TEST_OPERATION_TIMEOUT * 2);

        final Person[] persons = new Person[ENTRY_SIZE];
        for (int i = 0; i < persons.length; i++) {
            persons[i] = new Person((long) i);
        }
        
        LogUtils.log("Writing objects to Israel..");
        
        gigaSpace1.writeMultiple(persons);
        
        LogUtils.log("Verifying nothing was replicated to London..");
        
        Assert.assertEquals(0, gigaSpace2.count(null));        
        
        ProcessingUnit sink = admin2.getProcessingUnits().waitFor("LONDON", TEST_OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        sink.waitFor(1);
        
        LogUtils.log("London bootstrap from Israel..");
        
        bootstrap(admin2, "LONDON", "ISRAEL");

        LogUtils.log("Boostrap done.");
        
        // Verify delegators & sinks are connected.
        Space space1 = admin1.getSpaces().waitFor("israelSpace", 30, TimeUnit.SECONDS);
        Space space2 = admin2.getSpaces().waitFor("londonSpace", 30, TimeUnit.SECONDS);
        
        LogUtils.log("Verify WAN replication between sites is connected..");
        
        assertGatewayReplicationConnected(space1, 1);
        assertGatewayReplicationConnected(space2, 1);

        TestUtils.repetitive(new Runnable() {

            public void run() {
                Person[] result = gigaSpace2.readMultiple(new Person());
                Assert.assertEquals(persons.length, result.length);
            }
            
        }, TEST_OPERATION_TIMEOUT);

        LogUtils.log("Write object to each of the sites and verify WAN replication is working..");
        
        // Verify WAN replication
        final Person person = new Person(99999L, "abcd");
        gigaSpace1.write(person);
        
        TestUtils.repetitive(new Runnable() {

            public void run() {
                Person result = gigaSpace2.readById(Person.class, person.getId());
                Assert.assertNotNull(result);
            }
            
        }, TEST_OPERATION_TIMEOUT);
        
        final Person p2 = new Person(12121212L, "aaaa");
        gigaSpace2.write(p2);

        TestUtils.repetitive(new Runnable() {

            public void run() {
                Person result = gigaSpace1.readById(Person.class, p2.getId());
                Assert.assertNotNull(result);
            }
            
        }, TEST_OPERATION_TIMEOUT);

        LogUtils.log("Test completed.");

    }
    
    private void initialize() {
        admin1 = new AdminFactory().addGroups(group1).createAdmin();
        admin2 = new AdminFactory().addGroups(group2).createAdmin();
        SetupUtils.assertCleanSetup(admin1);
        SetupUtils.assertCleanSetup(admin2);

        GridServiceAgent gsa1 = admin1.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent gsa2 = admin2.getGridServiceAgents().waitForAtLeastOne();
        AbstractTest.assertNotNull("Could not find agent on group: " + admin1.getGroups()[0], gsa1);
        AbstractTest.assertNotNull("Could not find agent on group: " + admin2.getGroups()[0], gsa2);

        final String securityConfigFilePath = ScriptUtils.getBuildPath()+"/config/security/spring-test-security.properties";

        gsa1.startGridServiceAndWait(new GridServiceManagerOptions().vmInputArgument("-Dcom.gs.security.properties-file=" + securityConfigFilePath));
        gsa1.startGridServiceAndWait(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.security.properties-file=" + securityConfigFilePath));
        gsa1.startGridServiceAndWait(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.security.properties-file=" + securityConfigFilePath));
        
        gsa2.startGridServiceAndWait(new GridServiceManagerOptions().vmInputArgument("-Dcom.gs.security.properties-file=" + securityConfigFilePath));
        gsa2.startGridServiceAndWait(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.security.properties-file=" + securityConfigFilePath));
        gsa2.startGridServiceAndWait(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.security.properties-file=" + securityConfigFilePath));

        gsm1 = admin1.getGridServiceManagers().waitForAtLeastOne(20, TimeUnit.SECONDS);
        AbstractTest.assertNotNull(gsm1);
        gsm2 = admin2.getGridServiceManagers().waitForAtLeastOne(20, TimeUnit.SECONDS);
        AbstractTest.assertNotNull(gsm2);

        deploySecuredCluster("ISRAEL", "LONDON", "israelSpace", "/./israelSpace", gsm1);
        deploySecuredCluster("LONDON", "ISRAEL", "londonSpace", "/./londonSpace", gsm2);

        deployIsraelGateway();
        deployLondonGateway();

        assertTrue(admin1.getGridServiceContainers().waitFor(3, 5, TimeUnit.MINUTES));
        assertTrue(admin2.getGridServiceContainers().waitFor(3, 5, TimeUnit.MINUTES));

        gigaSpace1 = new GigaSpaceConfigurer(new UrlSpaceConfigurer("jini://*/*/israelSpace?groups=" + group1).userDetails(USERNAME, PASSWORD).space()).gigaSpace();
        gigaSpace2 = new GigaSpaceConfigurer(new UrlSpaceConfigurer("jini://*/*/londonSpace?groups=" + group2).userDetails(USERNAME, PASSWORD).space()).gigaSpace();
                
    }

    private void deployLondonGateway() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "LONDON");
        props.put("targetGatewayName", "ISRAEL");
        props.put("localClusterUrl", "jini://*/*/londonSpace?groups=" + group2);
        props.put("localGatewayHost", host2);
        props.put("localGatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
        props.put("localGatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
        props.put("targetGatewayHost", host1);
        props.put("targetGatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);        
        props.put("targetGatewayLrmiPort", ISRAEL_GATEWAY_LRMI_PORT);
        props.put("security.username", USERNAME);
        props.put("security.password", PASSWORD);
        deployGateway(gsm2, gatewayDeployment("./apps/gateway/security/gatewaySecurity1", "LONDON", props));
        props.clear();
    }

    private void deployIsraelGateway() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("targetGatewayName", "LONDON");
        props.put("localClusterUrl", "jini://*/*/israelSpace?groups=" + group1);
        props.put("localGatewayHost", host1);
        props.put("localGatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);
        props.put("localGatewayLrmiPort", ISRAEL_GATEWAY_LRMI_PORT);
        props.put("targetGatewayHost", host2);
        props.put("targetGatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
        props.put("targetGatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
        props.put("username1", USERNAME);
        props.put("password1", PASSWORD);
        props.put("username2", USERNAME);
        props.put("password2", PASSWORD);
        deployGateway(gsm1, gatewayDeployment("./apps/gateway/security/gatewaySecurity2", "ISRAEL", props));
        props.clear();
    }

    private void deploySecuredCluster(String localGatewayName,
            String targetGatewayName, String puName, String spaceUrl,
            GridServiceManager gsm) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", localGatewayName);
        props.put("gatewayTarget", targetGatewayName);
        props.put("spaceUrl", spaceUrl);
        ProcessingUnitDeployment siteDeployment = siteDeployment("./apps/gateway/cluster",
                puName, props);
        siteDeployment.secured(true);
        deploySite(gsm, siteDeployment);
        props.clear();
    }
    
    @AfterMethod
    public void tearDown() {
        TeardownUtils.teardownAll(admin1, admin2);
        admin1.close();
        admin2.close();
    }
}
