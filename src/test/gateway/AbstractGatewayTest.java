package test.gateway;


import static test.utils.LogUtils.log;

import java.io.File;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gateway.Gateway;
import org.openspaces.admin.gateway.GatewaySinkSource;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.ReplicationStatus;
import org.openspaces.admin.space.ReplicationTarget;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpacePartition;
import org.openspaces.core.GigaSpace;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import test.AbstractTest;
import test.utils.AssertUtils;
import test.utils.DeploymentUtils;
import test.utils.TestUtils;

import com.gatewayPUs.common.MessageGW;
import com.gigaspaces.cluster.activeelection.SpaceMode;

public class AbstractGatewayTest extends AbstractTest {

	protected static final int SLEEP_TIMEOUT = 30000;
	protected static final int ADMIN_TIMEOUT = 60000;
	protected static final int OPERATION_TIMEOUT = 45000;
	protected static final int ENTRY_SIZE = 100;
	protected static final int TEST_OPERATION_TIMEOUT = 10000;
	protected static final int NEEDED_SHORT_TIMEOUT = 30000;
	
	protected static final String GROUP1 = "sgtest_WAN_pc-lab44";
	protected static final String GROUP2 = "sgtest_WAN_pc-lab23";
	protected static final String GROUP3 = "sgtest_WAN_pc-lab42";
	protected static final String HOST1 = "192.168.9.64";
	protected static final String HOST2 = "192.168.9.43";
	protected static final String HOST3 = "192.168.9.62";

	protected static final String LONDON_GATEWAY_DISCOVERY_PORT = "10001";
	protected static final String LONDON_GATEWAY_LRMI_PORT = "7001";
	protected static final String NY_GATEWAY_DISCOVERY_PORT = "10002";
	protected static final String NY_GATEWAY_LRMI_PORT = "7002";
	protected static final String ISRAEL_GATEWAY_LRMI_PORT = "7000";
	protected static final String ISRAEL_GATEWAY_DISCOVERY_PORT = "10000";

	@BeforeMethod
	@Override
	public void beforeTest() {
		// do nothing... 
	}

	@AfterMethod
	@Override
	public void afterTest() {
		// do nothing
	}

	protected static boolean isDevMode() {
		return !getUserName().equals("tgrid");
	}

	static protected String getUserName() {
		return System.getProperty("user.name");
	}

	protected ProcessingUnitDeployment siteDeployment(String path, String name, Map<String, String> contextProperties) {
		ProcessingUnitDeployment puDeployment = new ProcessingUnitDeployment(new File(path)).name(name);
		if(System.getProperty("runMultiSource") != null)
			contextProperties.put("cluster-config.groups.group.repl-policy.processing-type", "multi-source");
		for (String key : contextProperties.keySet()) {
			puDeployment.setContextProperty(key, contextProperties.get(key));
		}
		return puDeployment;
	}

	protected ProcessingUnitDeployment sitePrepareAndDeployment(String name, Map<String, String> contextProperties) {
		DeploymentUtils.prepareApp("gatewayPUs");
		ProcessingUnitDeployment puDeployment = new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("gatewayPUs", name)).name(name);
		if(System.getProperty("runMultiSource") != null)
			contextProperties.put("cluster-config.groups.group.repl-policy.processing-type", "multi-source");
		for (String key : contextProperties.keySet()) {
			puDeployment.setContextProperty(key, contextProperties.get(key));
		}
		return puDeployment;
	}

	protected ProcessingUnitDeployment siteDeployment(String path, String name, Map<String, String> contextProperties, int numberOfPartitions, int numberOfBackups) {
		ProcessingUnitDeployment puDeployment = siteDeployment(path, name, contextProperties);
		puDeployment.numberOfInstances(numberOfPartitions);
		puDeployment.numberOfBackups(numberOfBackups);
		return puDeployment;
	}

	protected void deploySite(GridServiceManager gsm, ProcessingUnitDeployment processingUnitDeployment) {
		ProcessingUnit pu = gsm.deploy(processingUnitDeployment);
		assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));
	}

	protected ProcessingUnitDeployment gatewayDeployment(String path, String name, Map<String, String> contextProperties) {
		ProcessingUnitDeployment gatewayDeployment = new ProcessingUnitDeployment(new File(path)).name(name);
		for (String key : contextProperties.keySet()) {
			gatewayDeployment.setContextProperty(key, contextProperties.get(key));
		}
		return gatewayDeployment;
	}

	protected void deployGateway(GridServiceManager gsm, ProcessingUnitDeployment processingUnitDeployment) {
		deploySite(gsm, processingUnitDeployment);
	}

	protected void assertGatewayReplicationConnected(Space space, final int numberOfGatewayTargets) {
		for (final SpacePartition spaceInstance : space.getPartitions()) {
			TestUtils.repetitive(new Runnable() {

				public void run() {
					int gatewayReplicationsCount = 0;
					for (ReplicationTarget target : spaceInstance.getPrimary().getReplicationTargets()) {
						if (target.getMemberName().startsWith("gateway:")) {
							Assert.assertEquals(ReplicationStatus.ACTIVE, target.getReplicationStatus());
							gatewayReplicationsCount++;
						}
					}
					Assert.assertEquals(numberOfGatewayTargets, gatewayReplicationsCount);
				}

			}, (int)DEFAULT_TEST_TIMEOUT);
		}
	}

	protected void assertGatewayReplicationDisconnected(Space space, final int numberOfGatewayTargets) {
		for (final SpacePartition spaceInstance : space.getPartitions()) {
			TestUtils.repetitive(new Runnable() {

				public void run() {
					int gatewayReplicationsCount = 0;
					for (ReplicationTarget target : spaceInstance.getPrimary().getReplicationTargets()) {
						if (target.getMemberName().startsWith("gateway:")) {
							Assert.assertEquals(ReplicationStatus.DISCONNECTED, target.getReplicationStatus());
							gatewayReplicationsCount++;
						}
					}
					Assert.assertEquals(numberOfGatewayTargets, gatewayReplicationsCount);
				}

			}, (int)DEFAULT_TEST_TIMEOUT);
		}
	}


	protected void assertGatewayReplicationConnected(Space space, final int replicationTargetsNum, final int gatewayReplicationNum) {
		for (final SpacePartition spaceInstance : space.getPartitions()) {
			TestUtils.repetitive(new Runnable() {

				public void run() {
					Assert.assertEquals(replicationTargetsNum, spaceInstance.getPrimary().getReplicationTargets().length);
					int gatewayReplicationsCount = 0;
					for (ReplicationTarget target : spaceInstance.getPrimary().getReplicationTargets()) {
						if (target.getMemberName().startsWith("gateway:")) {
							Assert.assertEquals(ReplicationStatus.ACTIVE, target.getReplicationStatus());
							gatewayReplicationsCount++;
						}
					}
					Assert.assertEquals(gatewayReplicationNum, gatewayReplicationsCount);
				}

			}, (int)DEFAULT_TEST_TIMEOUT);
		}
	}

	public void assertGatewayReplicationHasNotingToReplicate(final Admin... admins) {
		TestUtils.repetitive(new Runnable() {
			public void run() {
				for (Admin admin : admins) {
					for (Space space : admin.getSpaces()) {
						if (space.getInstances()[0].getSpaceUrl().get("schema").equals("mirror"))
							continue;
						for (final SpacePartition spaceInstance : space
								.getPartitions()) {
							Assert.assertEquals("redo log not empty - " + spaceInstance.getPrimary()
									.getStatistics().getReplicationStatistics()
									.getOutgoingReplication().toString(), 0, spaceInstance.getPrimary()
									.getStatistics().getReplicationStatistics()
									.getOutgoingReplication().getRedoLogSize());
						}
					}
				}
				for (Admin admin : admins) {
					for (Space space : admin.getSpaces()) {
						Assert.assertEquals(0, space.getGigaSpace().count(null));
					}
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
			}
		}, (int)OPERATION_TIMEOUT);
	}

	public void assertSuccessfulOperationCount(final long expectedSuccessfulOperationCount, final Admin... admins) {
		TestUtils.repetitive(new Runnable() {
			public void run() {
				for (Admin admin : admins) {
					for (Space space : admin.getSpaces()) {
						for (final SpacePartition spaceInstance : space.getPartitions()) {
							if (!spaceInstance.getInstances()[0].getSpaceUrl().get("schema").equals("mirror"))
								continue;
							Assert.assertEquals(expectedSuccessfulOperationCount, spaceInstance.getPrimary().getStatistics()
									.getMirrorStatistics().getSuccessfulOperationCount());
						}
					}
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
			}
		}, (int)DEFAULT_TEST_TIMEOUT);
	}

	public void assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(final Admin... admins) {
		TestUtils.repetitive(new Runnable() {
			public void run() {
				for (Admin admin : admins) {
					for (Space space : admin.getSpaces()) {
						for (final SpacePartition spaceInstance : space.getPartitions()) {
							if (space.getInstances()[0].getSpaceUrl().get("schema").equals("mirror"))
								continue;
							Assert.assertEquals("redo log not empty - " + spaceInstance.getPrimary()
									.getStatistics().getReplicationStatistics()
									.getOutgoingReplication().toString(), 0, spaceInstance.getPrimary()
									.getStatistics().getReplicationStatistics()
									.getOutgoingReplication().getRedoLogSize());
						}
					}
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
			}
		}, (int)DEFAULT_TEST_TIMEOUT);
	}

	protected void clearGigaSpaces(GigaSpace... toClear) {
		for (GigaSpace gigaSpace : toClear) {
			gigaSpace.clear(null);
		}
	}

	public static void assertPrimaries(Admin admin, int expected) {
		int actual = 0;
		for (GridServiceContainer gsc : admin.getGridServiceContainers()) {
			for (ProcessingUnitInstance puInstance : gsc.getProcessingUnitInstances()) {
				if (puInstance.getSpaceInstance() == null)
					continue;
				if (SpaceMode.NONE.equals(puInstance.getSpaceInstance().getMode())) {
					puInstance.getSpaceInstance().waitForMode(SpaceMode.PRIMARY, (int)DEFAULT_TEST_TIMEOUT, TimeUnit.MILLISECONDS);
				}
				if (SpaceMode.BACKUP.equals(puInstance.getSpaceInstance().getMode())) {
					puInstance.getSpaceInstance().waitForMode(SpaceMode.PRIMARY, (int)DEFAULT_TEST_TIMEOUT, TimeUnit.MILLISECONDS);
				}
				if (SpaceMode.PRIMARY.equals(puInstance.getSpaceInstance().getMode())) {
					actual++;
				}
			}
		}
		assertEquals("unexpected primary PU instances", expected, actual);
	}

	/**
	 * clears all GigaSpace instances in spaces,
	 * asserting there are no replication pending for each Admin
	 * and asserting no objects are found in any of the spaces
	 *
	 * @param spaces
	 * @param admins
	 */
	protected void assertCleanSites(GigaSpace[] spaces, Admin[] admins) {
		clearGigaSpaces(spaces);
		assertGatewayReplicationHasNotingToReplicate(admins);
		for (GigaSpace space : spaces) {
			Assert.assertEquals(0, space.readMultiple(null).length);
		}
	}

	/**
	 * write to source and assert replication to targets
	 *
	 * @param source      the space you write to
	 * @param targets     the spaces you replicate to
	 * @param isProcessed whether the message is expected to be processed (if processor is deployed)
	 * @pre assertCleanSites
	 */
	protected void write(final GigaSpace source, GigaSpace[] targets, final boolean isProcessed) {
		source.write(new MessageGW(1, "Hello, world!"));
		for (final GigaSpace target : targets) {
			log("writing to " + source + " replicating to " + target);
			TestUtils.repetitive(new Runnable() {
				public void run() {
					MessageGW msg1 = source.read(new MessageGW(isProcessed));
					MessageGW msg2 = target.read(new MessageGW(isProcessed));
					Assert.assertNotNull(msg1);
					Assert.assertNotNull(msg2);
					Assert.assertEquals(msg1, msg2);
					Assert.assertEquals(msg1.getVersion(), msg2.getVersion());
				}
			}, (int)DEFAULT_TEST_TIMEOUT);
		}
	}

	/**
	 * writeMultiple to source and assert replication to targets
	 *
	 * @param source      the space you write to
	 * @param targets     the spaces you replicate to
	 * @param isProcessed whether the message is expected to be processed (if processor is deployed)
	 * @pre assertCleanSites
	 */
	protected void writeMultiple(final GigaSpace source, GigaSpace[] targets, final boolean isProcessed) {
		final MessageGW[] msgArray = new MessageGW[ENTRY_SIZE];
		for (int i = 0; i < ENTRY_SIZE; i++) {
			msgArray[i] = new MessageGW(i, "Hello, world!");
		}
		source.writeMultiple(msgArray);
		for (final GigaSpace target : targets) {
			log("writing multiple to " + source + " replicating to " + target);
			TestUtils.repetitive(new Runnable() {
				public void run() {
					Assert.assertEquals(ENTRY_SIZE,
							target.readMultiple(new MessageGW(isProcessed)).length);
					MessageGW[] sourceArray = source.readMultiple(new MessageGW(isProcessed));
					MessageGW[] targetArray = target.readMultiple(new MessageGW(isProcessed));
					Arrays.sort(sourceArray);
					Arrays.sort(targetArray);
					for (int i = 0; i < ENTRY_SIZE; i++) {
						Assert.assertEquals(sourceArray[i].getVersion(), targetArray[i].getVersion());
					}
					AssertUtils.assertEquivalenceArrays("checking that write and read arrays are the same", sourceArray, targetArray);
				}
			}, (int)DEFAULT_TEST_TIMEOUT);
		}
	}

	/**
	 * write to source and assert replication to targets
	 * than write again with the same ID and assert replication
	 *
	 * @param source      the space you write to
	 * @param targets     the spaces you replicate to
	 * @param isProcessed whether the message is expected to be processed (if processor is deployed)
	 * @pre assertCleanSites
	 */
	protected void update(final GigaSpace source, GigaSpace[] targets, final boolean isProcessed) {
		write(source, targets, isProcessed);
		source.write(new MessageGW(1, "Hello, world!"));
		for (final GigaSpace target : targets) {
			log("updating  on " + source + " replicating to " + target);
			TestUtils.repetitive(new Runnable() {
				public void run() {
					MessageGW msg1 = source.read(new MessageGW(isProcessed));
					MessageGW msg2 = target.read(new MessageGW(isProcessed));
					Assert.assertEquals(msg1, msg2);
					Assert.assertEquals(msg1.getVersion(), msg2.getVersion());
				}
			}, (int)DEFAULT_TEST_TIMEOUT);
		}
	}

	/**
	 * writeMultiple to source and assert replication to targets
	 * than writeMultiple again with the same IDs and assert replication
	 *
	 * @param source      the space you write to
	 * @param targets     the spaces you replicate to
	 * @param isProcessed whether the message is expected to be processed (if processor is deployed)
	 * @pre assertCleanSites
	 */
	protected void updateMultiple(final GigaSpace source, GigaSpace[] targets, final boolean isProcessed) {
		final MessageGW[] msgArray = new MessageGW[ENTRY_SIZE];
		for (int i = 0; i < ENTRY_SIZE; i++) {
			msgArray[i] = new MessageGW(i, "Hello, world!");
		}
		writeMultiple(source, targets, isProcessed);
		source.writeMultiple(msgArray);
		for (final GigaSpace target : targets) {
			log("updating multiple on " + source + " replicating to " + target);
			TestUtils.repetitive(new Runnable() {
				public void run() {
					Assert.assertEquals(ENTRY_SIZE,
							target.readMultiple(new MessageGW(isProcessed)).length);
					MessageGW[] sourceArray = source.readMultiple(new MessageGW(isProcessed));
					MessageGW[] targetArray = target.readMultiple(new MessageGW(isProcessed));
					Arrays.sort(sourceArray);
					Arrays.sort(targetArray);
					Assert.assertEquals(sourceArray.length, targetArray.length);
					for (int i = 0; i < ENTRY_SIZE; i++) {
						Assert.assertEquals(sourceArray[i].getVersion(), targetArray[i].getVersion());
					}
					AssertUtils.assertEquivalenceArrays("checking that write and read arrays are the same", sourceArray, targetArray);
				}
			}, (int)DEFAULT_TEST_TIMEOUT);
		}
	}

	/**
	 * write to source and assert replication to targets
	 * than take from source and assert replication
	 *
	 * @param source      the space you write to
	 * @param targets     the spaces you replicate to
	 * @param isProcessed whether the message is expected to be processed (if processor is deployed)
	 * @pre assertCleanSites
	 */
	protected void take(final GigaSpace source, GigaSpace[] targets, final boolean isProcessed) {
		write(source, targets, isProcessed);
		source.take(new MessageGW(isProcessed));
		for (final GigaSpace target : targets) {
			log("taking from " + source + " asserting take replicated to " + target);
			TestUtils.repetitive(new Runnable() {
				public void run() {
					Assert.assertNull(target.read(new MessageGW(isProcessed)));
				}
			}, (int)DEFAULT_TEST_TIMEOUT);
		}
	}

	/**
	 * writeMultiple to source and assert replication to targets
	 * than takeMultiple from source and assert replication
	 *
	 * @param source      the space you write to
	 * @param targets     the spaces you replicate to
	 * @param isProcessed whether the message is expected to be processed (if processor is deployed)
	 * @pre assertCleanSites
	 */
	protected void takeMultiple(final GigaSpace source, GigaSpace[] targets, final boolean isProcessed) {
		final MessageGW[] msgArray = new MessageGW[ENTRY_SIZE];
		for (int i = 0; i < ENTRY_SIZE; i++) {
			msgArray[i] = new MessageGW(i, "Hello, world!");
		}
		writeMultiple(source, targets, isProcessed);
		source.takeMultiple(new MessageGW(isProcessed));
		for (final GigaSpace target : targets) {
			log("taking multiple from " + source + " asserting take replicated to " + target);
			TestUtils.repetitive(new Runnable() {
				public void run() {
					MessageGW[] msgArray = target.readMultiple((new MessageGW(isProcessed)));
					Assert.assertEquals(0, msgArray.length);
				}
			}, (int)DEFAULT_TEST_TIMEOUT);
		}
	}

	/**
	 * write to source and assert no replication to targets
	 *
	 * @param source      the space you write to
	 * @param targets     the spaces you replicate to
	 * @param isProcessed whether the message is expected to be processed (if processor is deployed)
	 * @pre assertCleanSites
	 */
	protected void writeNotExpectingReplication(final GigaSpace source, GigaSpace[] targets, final boolean isProcessed) throws Exception {
		source.write(new MessageGW(1, "Hello, world!"));
		Thread.currentThread();
		Thread.sleep(SLEEP_TIMEOUT);
		for (final GigaSpace target : targets) {
			log("writing to " + source + " not replicating to " + target);
			TestUtils.repetitive(new Runnable() {
				public void run() {
					Assert.assertNull(target.read(new MessageGW(isProcessed)));
				}
			}, (int)DEFAULT_TEST_TIMEOUT);
		}
	}

	/**
	 * write to source and assert no replication to targets
	 * than write again with the same ID and assert no replication
	 *
	 * @param source      the space you write to
	 * @param targets     the spaces you replicate to
	 * @param isProcessed whether the message is expected to be processed (if processor is deployed)
	 * @pre assertCleanSites
	 */
	protected void updateNotExpectingReplication(final GigaSpace source, GigaSpace[] targets, final boolean isProcessed) throws Exception {
		source.write(new MessageGW(1, "Hello, world!"));
		for (final GigaSpace target : targets) {
			log("writing to " + source + " not replicating to " + target);
			TestUtils.repetitive(new Runnable() {
				public void run() {
					Assert.assertNull(target.read(new MessageGW(isProcessed)));
				}
			}, (int)DEFAULT_TEST_TIMEOUT);
		}
		source.write(new MessageGW(1, "Hello, world!"));
		Thread.currentThread();
		Thread.sleep(SLEEP_TIMEOUT);
		for (final GigaSpace target : targets) {
			log("updating on " + source + " not replicating to " + target);
			TestUtils.repetitive(new Runnable() {
				public void run() {
					Assert.assertNull(target.read(new MessageGW(isProcessed)));
				}
			}, (int)DEFAULT_TEST_TIMEOUT);
		}
	}

	/**
	 * writeMultiple to source and assert no replication to targets
	 *
	 * @param source      the space you write to
	 * @param targets     the spaces you replicate to
	 * @param isProcessed whether the message is expected to be processed (if processor is deployed)
	 * @pre assertCleanSites
	 */
	protected void writeMultipleNotExpectingReplication(final GigaSpace source, GigaSpace[] targets, final boolean isProcessed) throws Exception {
		final MessageGW[] msgArray = new MessageGW[ENTRY_SIZE];
		for (int i = 0; i < ENTRY_SIZE; i++) {
			msgArray[i] = new MessageGW(i, "Hello, world!");
		}
		source.writeMultiple(msgArray);
		Thread.currentThread();
		Thread.sleep(SLEEP_TIMEOUT);
		for (final GigaSpace target : targets) {
			log("writing multiple to " + source + " not replicating to " + target);
			TestUtils.repetitive(new Runnable() {
				public void run() {
					Assert.assertEquals(0, target.readMultiple(new MessageGW(isProcessed)).length);
				}
			}, (int)DEFAULT_TEST_TIMEOUT);
		}
	}

	/**
	 * writeMultiple to source and assert no replication to targets
	 * than writeMultiple again to source with same IDs and assert no replication
	 *
	 * @param source      the space you write to
	 * @param targets     the spaces you replicate to
	 * @param isProcessed whether the message is expected to be processed (if processor is deployed)
	 * @pre assertCleanSites
	 */
	protected void updateMultipleNotExpectingReplication(final GigaSpace source, GigaSpace[] targets, final boolean isProcessed) throws Exception {
		final MessageGW[] msgArray = new MessageGW[ENTRY_SIZE];
		for (int i = 0; i < ENTRY_SIZE; i++) {
			msgArray[i] = new MessageGW(i, "Hello, world!");
		}
		source.writeMultiple(msgArray);
		for (final GigaSpace target : targets) {
			log("writing multiple to " + source + " not replicating to " + target);
			TestUtils.repetitive(new Runnable() {
				public void run() {
					Assert.assertEquals(0, target.readMultiple(new MessageGW(isProcessed)).length);
				}
			}, (int)DEFAULT_TEST_TIMEOUT);
		}
		source.write(new MessageGW(1, "Hello, world!"));
		Thread.currentThread();
		Thread.sleep(20 * 1000);
		for (final GigaSpace target : targets) {
			log("updating multiple on " + source + " not replicating to " + target);
			Assert.assertEquals(0, target.readMultiple(new MessageGW(isProcessed)).length);
		}
	}
	
	
	/**
	 * @param admin the admin that manages the gateway
	 * @param localGatewayName the name of the local gateway
	 * @param targetGatewayName the name of the target to bootstrap from
	 */
	protected void bootstrap(Admin admin, String localGatewayName, String targetGatewayName){   
		Gateway gateway = admin.getGateways().waitFor(localGatewayName);
		GatewaySinkSource sinkSource = gateway.waitForSinkSource(targetGatewayName);        
		sinkSource.bootstrapFromGatewayAndWait((int)DEFAULT_TEST_TIMEOUT, TimeUnit.MILLISECONDS);
	}

	/**
	 * @param admin the admin that manages the gateway
	 * @param localGatewayName the name of the local gateway in the sink
	 * @param sourceGatewayName the name of the source the sink should enable replication from
	 */
	protected void enableIncomingReplication(Admin admin, String localGatewayName, String sourceGatewayName){
		admin.getGateways().getGateway(localGatewayName).waitForSink(sourceGatewayName).enableIncomingReplication();
	}


	/**
	 * @return the current time formatted string
	 */
	protected String printCurrentTime(){
		Calendar cal = Calendar.getInstance();
		DateFormat df = DateFormat.getTimeInstance(DateFormat.FULL);
		return df.format(cal.getTime()).split(" ")[0];
	}

}
