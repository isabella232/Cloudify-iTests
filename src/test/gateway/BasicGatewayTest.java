package test.gateway;

import com.j_spaces.core.client.UpdateModifiers;
import net.jini.core.lease.Lease;
import org.junit.Assert;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.transaction.manager.DistributedJiniTxManagerConfigurer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import test.data.Person;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;
import test.utils.TestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Basic gateway test.
 *
 * @author idan
 * @since 8.0.3
 */
public class BasicGatewayTest extends AbstractGatewayTest {

    private Admin admin1;
    private Admin admin2;
    private GigaSpace gigaSpace2;
    private GigaSpace gigaSpace1;

    private String group1 = null;
    private String group2 = null;
    private String host1 = null;
    private String host2 = null;

    public BasicGatewayTest() {
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

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="2", enabled=true)
    public void testGatewayOperations() throws Exception {
        initialize();

        final Person person1 = new Person(1L, "John Doe");
        // israel -> london
        gigaSpace1.write(person1);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Person person2 = gigaSpace2.readById(Person.class, 1L);
                Assert.assertNotNull(person2);
            }
        }, (int) DEFAULT_TEST_TIMEOUT);
        //
        person1.setName("John D");
        gigaSpace1.write(person1, Lease.FOREVER, 0, UpdateModifiers.UPDATE_ONLY);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Person person2 = gigaSpace2.readById(Person.class, 1L);
                Assert.assertNotNull(person2);
                Assert.assertEquals(person1.getName(), person2.getName());

            }
        }, (int) DEFAULT_TEST_TIMEOUT);
        //
        gigaSpace1.take(person1);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Person person2 = gigaSpace2.readIfExistsById(Person.class, 1L, null, NEEDED_SHORT_TIMEOUT);
                Assert.assertNull(person2);
            }
        }, (int) DEFAULT_TEST_TIMEOUT);
        // london -> israel
        gigaSpace2.write(person1);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Person person2 = gigaSpace1.readById(Person.class, 1L);
                Assert.assertNotNull(person2);
            }
        }, (int) DEFAULT_TEST_TIMEOUT);
        //
        person1.setName("John D");
        gigaSpace2.write(person1, Lease.FOREVER, 0, UpdateModifiers.UPDATE_ONLY);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Person person2 = gigaSpace1.readById(Person.class, 1L);
                Assert.assertNotNull(person2);
                Assert.assertEquals(person1.getName(), person2.getName());

            }
        }, (int) DEFAULT_TEST_TIMEOUT);
        //
        gigaSpace2.take(person1);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Person person2 = gigaSpace1.readById(Person.class, 1L);
                Assert.assertNull(person2);
            }
        }, (int) DEFAULT_TEST_TIMEOUT);
        // test write/read/take multiple
        final Person[] ps1 = new Person[5];
        for (int i = 0; i < ps1.length; i++) {
            ps1[i] = new Person((long) (i * 5000), "person-" + (i * 5000));
        }
        gigaSpace1.writeMultiple(ps1);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Person[] result = gigaSpace2.readMultiple(new Person(), Integer.MAX_VALUE);
                Assert.assertNotNull(result);
                Assert.assertEquals(5, result.length);
            }
        }, (int) DEFAULT_TEST_TIMEOUT);
        Assert.assertEquals(ps1.length, gigaSpace1.takeMultiple(new Person()).length);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                int count = gigaSpace2.count(new Person());
                Assert.assertEquals(0, count);
            }
        }, (int) DEFAULT_TEST_TIMEOUT);

        // transaction
        PlatformTransactionManager ptm = new DistributedJiniTxManagerConfigurer().transactionManager();
        GigaSpace txnGigaSpace1 = new GigaSpaceConfigurer(gigaSpace1.getSpace()).transactionManager(ptm)
        .clustered(true).gigaSpace();
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        TransactionStatus status = ptm.getTransaction(definition);
        final Person[] ps = new Person[5];
        for (int i = 0; i < ps.length; i++)
            ps[i] = new Person((long) i, "person-" + i);
        txnGigaSpace1.writeMultiple(ps);
        ptm.commit(status);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                int count = gigaSpace2.count(null);
                Assert.assertEquals(count, ps.length);
            }
        }, (int) DEFAULT_TEST_TIMEOUT);
        
        // clear
        clearClusters();
        
        // several updates within a transaction
        testSeveralUpdatesWithinATransaction();
    }

    private void testSeveralUpdatesWithinATransaction() throws Exception
    {
        final Person p = new Person(555L);
        gigaSpace1.write(p);
        
        // transaction
        PlatformTransactionManager ptm = new DistributedJiniTxManagerConfigurer().transactionManager();
        GigaSpace txnGigaSpace1 = new GigaSpaceConfigurer(gigaSpace1.getSpace()).transactionManager(ptm)
        .clustered(true).gigaSpace();
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setTimeout(99999999);
        TransactionStatus status = ptm.getTransaction(definition);
        
        p.setName("1");
        txnGigaSpace1.write(p);
        p.setName("2");
        txnGigaSpace1.write(p);
        p.setName("3");
        txnGigaSpace1.write(p);
        
        ptm.commit(status);
        
        TestUtils.repetitive(new Runnable() {
            @Override
            public void run() {
                Person result = gigaSpace2.readById(Person.class, 555L);
                Assert.assertNotNull(result);
                Assert.assertEquals(p.getName(), result.getName());
            }
        }, OPERATION_TIMEOUT);
        
        // Attempt to update again (for verifying version in target site is correct)
        p.setName("4");
        txnGigaSpace1.write(p);
        TestUtils.repetitive(new Runnable() {
            @Override
            public void run() {
                Person result = gigaSpace2.readById(Person.class, 555L);
                Assert.assertNotNull(result);
                Assert.assertEquals(p.getName(), result.getName());
            }
        }, OPERATION_TIMEOUT);
    }

    private void clearClusters() {
        gigaSpace1.clear(null);
        TestUtils.repetitive(new Runnable() {
            public void run() {
                int count = gigaSpace2.count(null);
                Assert.assertEquals(count, 0);
            }
        }, (int) DEFAULT_TEST_TIMEOUT);
    }

    private void initialize() {
        admin1 = new AdminFactory().addGroups(group1).createAdmin();
        admin2 = new AdminFactory().addGroups(group2).createAdmin();
        SetupUtils.assertCleanSetup(admin1);
        SetupUtils.assertCleanSetup(admin2);

        GridServiceAgent gsa1 = admin1.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent gsa2 = admin2.getGridServiceAgents().waitForAtLeastOne();

        gsa1.startGridServiceAndWait(new GridServiceManagerOptions());
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
        props.put("localGatewayDiscoveryPort", "10001");
        props.put("localGatewayLrmiPort", "7000");
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
        props.put("targetGatewayDiscoveryPort", "10001");
        props.put("targetGatewayLrmiPort", "7000");
        deployGateway(gsm2, gatewayDeployment("./apps/gateway/gateway-components", "LONDON", props));
        props.clear();

        assertTrue(admin1.getGridServiceContainers().waitFor(3));
        assertTrue(admin2.getGridServiceContainers().waitFor(3));

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
