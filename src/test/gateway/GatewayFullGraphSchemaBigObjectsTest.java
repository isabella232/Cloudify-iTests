package test.gateway;

import com.gigaspaces.annotation.pojo.*;
import com.gigaspaces.metadata.index.SpaceIndexType;
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
import test.AbstractTest;
import test.utils.AssertUtils;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;
import test.utils.TestUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static test.utils.LogUtils.log;

/**
 * @author sagib
 * @since 8.0.3
 */
public class GatewayFullGraphSchemaBigObjectsTest extends AbstractGatewayTest {

    private static final int ENTRY_SIZE = 10;
    private static final int MB = 1024*1024;
    private static final int KB = 1024;
    
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

    public GatewayFullGraphSchemaBigObjectsTest() {
        if (isDevMode()) {
            groupISR = "isreal-" + getUserName();
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
        Admin[] admins = {adminISR, adminLON, adminNY};
        TeardownUtils.teardownAll(admins);
        adminISR.close();
        adminLON.close();
        adminNY.close();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="3", enabled=true)
    public void test() throws Exception {
        initialize();
        GigaSpace[] spaces = {gigaSpace1, gigaSpace2, gigaSpace3};
        GigaSpace[] targets = {gigaSpace2, gigaSpace3};
        Admin[] admins = {adminISR, adminLON, adminNY};

        int caseNum = 0;

        log("case #" + ++caseNum + " write");
        write(gigaSpace1, targets, KB);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " write");
        write(gigaSpace3, new GigaSpace[]{gigaSpace1, gigaSpace2}, KB);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " writeMultiple");
        writeMultiple(gigaSpace1, targets, KB);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " update");
        update(gigaSpace1, targets, KB);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " take");
        take(gigaSpace1, targets, KB);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " take");
        take(gigaSpace2,  new GigaSpace[]{gigaSpace1, gigaSpace3}, KB);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " takeMultiple");
        takeMultiple(gigaSpace1, targets, KB);

        log("case #" + ++caseNum + " write");
        write(gigaSpace1, targets, MB);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " write");
        write(gigaSpace3,  new GigaSpace[]{gigaSpace1, gigaSpace2}, MB);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " writeMultiple");
        writeMultiple(gigaSpace1, targets, MB);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " update");
        update(gigaSpace1, targets, MB);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " take");
        take(gigaSpace1, targets, MB);
        assertCleanSites(spaces, admins);

        log("case #" + ++caseNum + " takeMultiple");
        takeMultiple(gigaSpace1, targets, MB);

        log("case #" + ++caseNum + " takeMultiple");
        takeMultiple(gigaSpace2,  new GigaSpace[]{gigaSpace1, gigaSpace3}, MB);

    }

    protected void write(final GigaSpace source, GigaSpace[] targets, int sizeInBytes) {
        source.write(new BigObject(1, sizeInBytes));
        for (final GigaSpace target : targets) {
            log("writing to " + source + " replicating to " + target);
            TestUtils.repetitive(new Runnable() {
                public void run() {
                    BigObject obj1 = source.read(new BigObject());
                    BigObject obj2 = target.read(new BigObject());
                    Assert.assertNotNull(obj1);
                    Assert.assertNotNull(obj2);
                    Assert.assertEquals(obj1, obj2);
                }
            }, (int)DEFAULT_TEST_TIMEOUT);
        }
    }

    protected void update(final GigaSpace source, GigaSpace[] targets, final int sizeInBytes) {
        write(source, targets, sizeInBytes);
        source.write(new BigObject(1, "updated"));
        for (final GigaSpace target : targets) {
            log("updating  on " + source + " replicating to " + target);
            TestUtils.repetitive(new Runnable() {
                public void run() {
                    BigObject bigObject1 = source.read(new BigObject());
                    BigObject bigObject2 = target.read(new BigObject());
                    Assert.assertEquals(bigObject1, bigObject2);
                }
            }, (int)DEFAULT_TEST_TIMEOUT);
        }
    }

    protected void take(final GigaSpace source, GigaSpace[] targets, final int sizeInBytes) {
        write(source, targets, sizeInBytes);
        source.take(new BigObject());
        for (final GigaSpace target : targets) {
            log("taking from " + source + " asserting take replicated to " + target);
            TestUtils.repetitive(new Runnable() {
                public void run() {
                    Assert.assertNull(target.read(new BigObject()));
                }
            }, (int)DEFAULT_TEST_TIMEOUT);
        }
    }

    protected void writeMultiple(final GigaSpace source, GigaSpace[] targets, final int sizeInBytes) {
        final BigObject[] bigObjectArray = new BigObject[ENTRY_SIZE];
        for (int i = 0; i < ENTRY_SIZE; i++) {
            bigObjectArray[i] = new BigObject(i, sizeInBytes);
        }
        source.writeMultiple(bigObjectArray);
        for (final GigaSpace target : targets) {
            log("writing multiple to " + source + " replicating to " + target);
            TestUtils.repetitive(new Runnable() {
                public void run() {
                    Assert.assertEquals(ENTRY_SIZE,
                            target.readMultiple(new BigObject()).length);
                    BigObject[] sourceArray = source.readMultiple(new BigObject());
                    BigObject[] targetArray = target.readMultiple(new BigObject());
                    AssertUtils.assertEquivalenceArrays("checking that write and read arrays are the same", sourceArray, targetArray);
                }
            }, (int)DEFAULT_TEST_TIMEOUT);
        }
    }

    protected void takeMultiple(final GigaSpace source, GigaSpace[] targets, final int sizeInBytes) {
        final BigObject[] bigObjectArray = new BigObject[ENTRY_SIZE];
        for (int i = 0; i < ENTRY_SIZE; i++) {
            bigObjectArray[i] = new BigObject(i, sizeInBytes);
        }
        writeMultiple(source, targets, sizeInBytes);
        source.takeMultiple(new BigObject());
        for (final GigaSpace target : targets) {
            log("taking multiple from " + source + " asserting take replicated to " + target);
            TestUtils.repetitive(new Runnable() {
                public void run() {
                    BigObject[] bigObjectArray = target.readMultiple((new BigObject()));
                    Assert.assertEquals(0, bigObjectArray.length);
                }
            }, (int)DEFAULT_TEST_TIMEOUT);
        }
    }

    private void initialize() throws Exception {
        log("initializing..");
        adminISR = new AdminFactory().addGroups(groupISR).createAdmin();
        adminLON = new AdminFactory().addGroups(groupLON).createAdmin();
        adminNY = new AdminFactory().addGroups(groupNY).createAdmin();
        SetupUtils.assertCleanSetup(adminLON);
        SetupUtils.assertCleanSetup(adminNY);
        SetupUtils.assertCleanSetup(adminISR);

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

        final GridServiceManager gsmISR = adminISR.getGridServiceManagers().waitForAtLeastOne();
        AbstractTest.assertNotNull(gsmISR);
        final GridServiceManager gsmLON = adminLON.getGridServiceManagers().waitForAtLeastOne();
        AbstractTest.assertNotNull(gsmLON);
        final GridServiceManager gsmNY = adminNY.getGridServiceManagers().waitForAtLeastOne();
        AbstractTest.assertNotNull(gsmNY);

        log("deploying PUs");
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("gatewayTarget", "LONDON");
        props.put("gatewayTarget2", "NY");
        props.put("spaceUrl", "/./israelSpace");
        deploySite(gsmISR, siteDeployment("./apps/gateway/clusterWith2targets", "israelSpace", props)
                .numberOfInstances(1).numberOfBackups(1));
        props.clear();

        props.put("localGatewayName", "LONDON");
        props.put("gatewayTarget", "ISRAEL");
        props.put("gatewayTarget2", "NY");
        props.put("spaceUrl", "/./londonSpace");
        deploySite(gsmLON, siteDeployment("./apps/gateway/clusterWith2targets", "londonSpace", props)
                .numberOfInstances(1).numberOfBackups(1));
        props.clear();

        props.put("localGatewayName", "NY");
        props.put("gatewayTarget", "ISRAEL");
        props.put("gatewayTarget2", "LONDON");
        props.put("spaceUrl", "/./nySpace");
        deploySite(gsmNY, siteDeployment("./apps/gateway/clusterWith2targets", "nySpace", props)
                .numberOfInstances(1).numberOfBackups(1));
        props.clear();

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
        props.clear();

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
        props.clear();

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
        deployGateway(gsmNY, siteDeployment("./apps/gateway/gatewayDoubleLink", "NY", props));
        props.clear();

        Thread.sleep(10000);

        adminISR.getGridServiceContainers().waitFor(3);
        adminLON.getGridServiceContainers().waitFor(3);
        adminNY.getGridServiceContainers().waitFor(3);


        Space space1 = adminISR.getSpaces().waitFor("israelSpace");
        Space space2 = adminLON.getSpaces().waitFor("londonSpace");
        Space space3 = adminNY.getSpaces().waitFor("nySpace");

        gigaSpace1 = space1.getGigaSpace();
        gigaSpace2 = space2.getGigaSpace();
        gigaSpace3 = space3.getGigaSpace();


        log("validating gateway components");


        // Verify delegators & sinks are connected.
        assertGatewayReplicationConnected(space1, 3, 2);
        assertGatewayReplicationConnected(space2, 3, 2);
        assertGatewayReplicationConnected(space3, 3, 2);

        log("finished initialziation");

    }

    @SpaceClass
    public static class BigObject {

        private Integer id;
        private String info;
        private int version;
        private byte[] content;

        public BigObject() {
        }


        public BigObject(Integer id) {
            this.id = id;
        }

         public BigObject(Integer id, Integer sizeInBytes) {
            this.id = id;
            setContent(sizeInBytes);
        }

        public BigObject(String info) {
            this.info = info;
        }

        public BigObject(Integer id, String info) {
            this.id = id;
            this.info = info;
        }

        @SpaceRouting
        @SpaceId
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getInfo() {
            return info;
        }

        public void setInfo(String info) {
            this.info = info;
        }

        public byte[] getContent() {
            return content;
        }

        @SpaceIndex(type = SpaceIndexType.BASIC)
        public void setContent(int sizeInBytes) {
            this.content = new byte[sizeInBytes];
        }

        @Override
        public String toString() {
            return "BigObject{" +
                    "id=" + id +
                    ", info='" + info + '\'' +
                    ", version=" + version +
                    '}';
        }

        @SpaceVersion
        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BigObject bigObject = (BigObject) o;

            if (!Arrays.equals(content, bigObject.content)) return false;
            if (id != null ? !id.equals(bigObject.id) : bigObject.id != null) return false;
            if (info != null ? !info.equals(bigObject.info) : bigObject.info != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (info != null ? info.hashCode() : 0);
            result = 31 * result + (content != null ? Arrays.hashCode(content) : 0);
            return result;
        }
    }
}