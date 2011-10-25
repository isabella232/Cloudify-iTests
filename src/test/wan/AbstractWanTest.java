package test.wan;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.space.UrlSpaceConfigurer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import test.AbstractTest;
import test.data.Data;
import test.utils.LogUtils;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;

import java.util.Arrays;
import com.j_spaces.core.IJSpace;

public class AbstractWanTest extends AbstractTest {

	protected String site1MirrorName = "site1Mirror";
	protected String site2MirrorName = "site2Mirror";
	protected GridServiceAgent gsa1;
	protected String ip;
	protected String ip2;
	protected Admin admin2;
	protected GridServiceAgent gsa2;
	protected String group1;
	protected String group2;

	protected Space sp1;
	protected Space sp2;
	protected GigaSpace gigaSpace1;
	protected GigaSpace gigaSpace2;
	
	
	private ProcessingUnitDeployment mirror1PuDeployment;
	private ProcessingUnitDeployment mirror2PuDeployment;
	protected String gatewayPUFilePath = "./apps/wan/wan-gateway";
	
	protected Integer site1NumberOfInstances = 2;
	protected Integer site2NumberOfInstances = 2;
	protected Integer site1NumberOfBackups = 0;
	protected Integer site2NumberOfBackups = 0;

	@Override
	@AfterMethod
	public void afterTest() {
		super.afterTest();

		try {
			TeardownUtils.teardownAll(admin2);
		} catch (final Throwable t) {
			LogUtils.log("failed to teardown", t);
		}
		admin2.close();
		admin2 = null;
	}

	/********
	 * The following three groups are available for tests in SGTEST: sgtest
	 * sgtest_WAN_pc-lab23 sgtest_WAN_pc-lab24
	 */
	@Override
	@BeforeMethod
	public void beforeTest() {

		if (System.getenv("SGTEST_DEV") != null) {

			System.setProperty("com.gs.jini_lus.groups", "SITE_1");
			group2 = "SITE_2";

			// testOnRemoteMachines();

			super.beforeTest();
			group1 = admin.getGroups()[0];

			// group2 = "sgtest_WAN_pclab-42";
			// group2 = "sgtest_WAN_pc-lab24";

		} else {
			// System.setProperty("com.gs.jini_lus.groups",
			// "sgtest_WAN_pc-lab23");
			super.beforeTest();
			group1 = admin.getGroups()[0];
			group2 = "sgtest_WAN_pc-lab24";
			// group2 = "sgtest_WAN_pclab-42";
		}

		newAdmin2();
		initWan();
	}

	protected void newAdmin2() {
		admin2 = new AdminFactory().addGroups(group2).createAdmin();
		SetupUtils.assertCleanSetup(admin2);
	}

	private class MirrorAddedListener implements ProcessingUnitInstanceAddedEventListener {

		private final String puName;
		private int counter;

		public MirrorAddedListener(final String puName) {
			this.puName = puName;
		}

		private synchronized void inc() {
			++counter;
			if (counter == 2) {
				this.notify();
			}
		}

		public synchronized boolean waitUntilReady() {
			if (counter == 2) {
				return true;
			}
			try {
				this.wait(60000);
			} catch (final InterruptedException e) {
				// ignore
			}
			return (counter == 2);

		}

		public void processingUnitInstanceAdded(final ProcessingUnitInstance processingUnitInstance) {
			final String currentPuName = processingUnitInstance.getName();
			if (currentPuName.equals(puName)) {
				inc();
			}
		}
	}

	protected static class DataComparator implements Comparator<Data> {
		public int compare(final Data o1, final Data o2) {
			return o1.toString().compareTo(o2.toString());
		}
	}

	/**
	 * 
	 */
	private void initWan() {
		this.gsa1 = admin.getGridServiceAgents().waitForAtLeastOne(10, TimeUnit.SECONDS);
		this.gsa2 = admin2.getGridServiceAgents().waitForAtLeastOne(10, TimeUnit.SECONDS);
		AbstractTest.assertNotNull("Could not find agent on group: " + admin.getGroups()[0], gsa1);
		AbstractTest.assertNotNull("Could not find agent on group: " + admin2.getGroups()[0], gsa2);

		this.ip = gsa1.getMachine().getHostAddress();
		this.ip2 = gsa2.getMachine().getHostAddress();

		gsa1.startGridService(new GridServiceManagerOptions());
		gsa2.startGridService(new GridServiceManagerOptions());

		gsa1.startGridService(new GridServiceContainerOptions());
		gsa1.startGridService(new GridServiceContainerOptions());
		gsa2.startGridService(new GridServiceContainerOptions());
		gsa2.startGridService(new GridServiceContainerOptions());

		final GridServiceManager gsm1 = admin.getGridServiceManagers().waitForAtLeastOne(20, TimeUnit.SECONDS);
		final GridServiceManager gsm2 = admin2.getGridServiceManagers().waitForAtLeastOne(20, TimeUnit.SECONDS);
		AbstractTest.assertNotNull(gsm1);
		AbstractTest.assertNotNull(gsm2);

		gsm1.deploy(createSite1PUDeployment());
		gsm2.deploy(createSite2PUDeployment());

		AbstractTest.assertTrue(admin.getGridServiceContainers().waitFor(2, 30, TimeUnit.SECONDS));
		AbstractTest.assertTrue(admin2.getGridServiceContainers().waitFor(2, 30, TimeUnit.SECONDS));

		final MirrorAddedListener listener1 = new MirrorAddedListener(this.site1MirrorName);
		final MirrorAddedListener listener2 = new MirrorAddedListener(this.site2MirrorName);

		this.admin.getProcessingUnits().getProcessingUnitInstanceAdded().add(listener1, false);
		this.admin2.getProcessingUnits().getProcessingUnitInstanceAdded().add(listener2, false);

		mirror1PuDeployment = createPUDeploymentForMirror1(ip, ip2);
		mirror2PuDeployment = createPUDeploymentForMirror2(ip, ip2);
		gsm1.deploy(mirror1PuDeployment);
		gsm2.deploy(mirror2PuDeployment);

		this.sp1 = waitForSpace(admin, "site1Space");
		this.sp2 = waitForSpace(admin2, "site2Space");

		AbstractTest.assertTrue("Wan Gateway 1 did not start", listener1.waitUntilReady());
		AbstractTest.assertTrue("Wan Gateway 2 did not start", listener2.waitUntilReady());

		this.gigaSpace1 = createGigaSpace1();
		this.gigaSpace2 = createGigaSpace2();
	}

	protected ProcessingUnit getMirrorPU1() {
		return this.admin.getProcessingUnits().getProcessingUnit(this.site1MirrorName);
	}

	protected ProcessingUnit getMirrorPU2() {
		return this.admin2.getProcessingUnits().getProcessingUnit(this.site2MirrorName);
	}

	protected Space waitForSpace(final Admin admin, final String spaceName) {
		final Space space = admin.getSpaces().waitFor(spaceName, 40, TimeUnit.SECONDS);
		AbstractTest.assertNotNull(space);
		AbstractTest.assertTrue(space.waitFor(space.getNumberOfInstances() * (space.getNumberOfBackups() + 1), 40,
				TimeUnit.SECONDS));
		return space;
	}

	protected void waitForSpace(final String url) {
		final IJSpace space = new UrlSpaceConfigurer(url).lookupTimeout(50000).space();
		AbstractTest.assertNotNull("Could not find Space on " + url, space);
	}

	protected GigaSpace createGigaSpace2() {
		return this.sp2.getGigaSpace().getClustered();
	}

	protected GigaSpace createGigaSpace1() {
		return this.sp1.getGigaSpace().getClustered();
	}

	protected ProcessingUnit waitForPU(final Admin clusterAdmin, final String puName) {
		final ProcessingUnit pu = clusterAdmin.getProcessingUnits().waitFor(puName, 30, TimeUnit.SECONDS);
		AbstractTest.assertNotNull(pu);
		AbstractTest.assertTrue(pu.waitFor(1, 90, TimeUnit.SECONDS));
		return pu;
	}

	protected ProcessingUnitDeployment createSite1PUDeployment() {
		return new ProcessingUnitDeployment(
					new File("./apps/wan/wan-space-cluster"))
					.name("site1Space")
					.setContextProperty("localClusterInitializerMirrorUrl",
							"jini://*/site1Mirror_container/site1Mirror")
					.setContextProperty(
							"localClusterInitializerUrl",
							"/./site1Space?mirror=true&groups="
									+ group1)
					.maxInstancesPerMachine(2)
					.setContextProperty("numOfPartitions", site1NumberOfInstances.toString())
					.setContextProperty("numOfBackupsPerPartition", site1NumberOfBackups.toString());
	}

	protected ProcessingUnitDeployment createSite2PUDeployment() {
		return new ProcessingUnitDeployment(
				new File("./apps/wan/wan-space-cluster"))
				.name("site2Space")
				.setContextProperty("localClusterInitializerMirrorUrl", "jini://*/site2Mirror_container/site2Mirror")
				.setContextProperty(
						"localClusterInitializerUrl",
						"/./site2Space?mirror=true&groups="
								+ group2)
				.maxInstancesPerMachine(3)
				.setContextProperty("numOfPartitions", site2NumberOfInstances.toString())
				.setContextProperty("numOfBackupsPerPartition", site2NumberOfBackups.toString());
	}
	
	protected ProcessingUnitDeployment createPUDeploymentForMirror2(final String ip1, final String ip2) {
		return new ProcessingUnitDeployment(
				new File(gatewayPUFilePath))
				.name(site2MirrorName)
				.setContextProperty("mySiteId", "2")
				.setContextProperty("localClusterSpaceUrl", "jini://*/*/site2Space?groups=" + group2)
				.setContextProperty("mirrorURL", "/./site2Mirror?groups=" + group2)
				.setContextProperty("siteSpaceName", "site2Space")
				.setContextProperty("ip1", ip1)
				.setContextProperty("ip2", ip2);
	}

	protected ProcessingUnitDeployment createPUDeploymentForMirror1(final String ip1, final String ip2) {

		return new ProcessingUnitDeployment(new File(gatewayPUFilePath))
				.name(site1MirrorName)
				.setContextProperty("mySiteId", "1")
				.setContextProperty("localClusterSpaceUrl", "jini://*/*/site1Space?groups=" + group1)
				.setContextProperty("mirrorURL", "/./site1Mirror?groups=" + group1)
				.setContextProperty("siteSpaceName", "site1Space")
				.setContextProperty("ip1", ip1)
				.setContextProperty("ip2", ip2);
	}

	private int markerTypeCounter = 0;

	protected void waitForEndMarkers(final int howmany, final long howlongMillis, final int markerType) {
		waitForEndMarkers(gigaSpace2, howmany, howlongMillis, markerType);
	}
	
	protected void waitForEndMarkers(GigaSpace gigaSpace, final int howmany, final long howlongMillis, final int markerType) {
		final long end = System.currentTimeMillis() + howlongMillis;

		final Marker template = new Marker();
		template.setType(markerType);

		while (System.currentTimeMillis() < end) {
			final int result = gigaSpace.count(template);
			if (result == howmany) {
				return;
			}
			try {
				Thread.sleep(1000);
			} catch (final InterruptedException ie) {
				// ignore
			}
		}
		AbstractTest.assertTrue("End markers were not found in remote space", false);

	}

	protected void writeEndMarkersAndWait(final int howmany, final long timeout) {
		writeEndMarkersAndWait(gigaSpace1, gigaSpace2, howmany, timeout);
	}
	protected void writeEndMarkersAndWait(final GigaSpace gigaSpaceWrite, final GigaSpace gigaSpaceRead, 
			final int howmany, final long timeout) {
		final int markerType = ++markerTypeCounter;
		writeEndMarkers(gigaSpaceWrite, howmany, markerType);
		waitForEndMarkers(gigaSpaceRead, howmany, timeout, markerType);

	}

	protected void writeEndMarkers(final int howmany, final int markerType) {
		writeEndMarkers(gigaSpace1, howmany, markerType);
	}
	protected void writeEndMarkers(GigaSpace gigaSpace, final int howmany, final int markerType) {

		for (int i = 0; i < howmany; ++i) {
			final Marker marker = new Marker();
			marker.setId(markerType * 1000 + i);
			marker.setType(markerType);

			gigaSpace.write(marker);
		}

	}

	protected void writeRandomDataEntries(final int numOfEntries, final int startFromId) {
		writeRandomDataEntries(this.gigaSpace1, numOfEntries, startFromId);
	}
	protected void writeRandomDataEntries(final GigaSpace gigaSpace, final int numOfEntries, final int startFromId) {
		int lastId = startFromId;
		final Random rnd = new Random();
		for (int i = 0; i < numOfEntries; ++i) {
			final int type = rnd.nextInt(3);
			switch (type) {
			case 0:
				final int updateIndex = rnd.nextInt(lastId);
				final Data tmp = gigaSpace.readById(Data.class, "" + updateIndex);
				if (tmp != null) {
					tmp.setData(tmp.getData() + " MODIFIED");
					gigaSpace.write(tmp);
				}
				break;
			case 1:
				final int takeIndex = rnd.nextInt(lastId);
				gigaSpace.takeById(Data.class, "" + takeIndex);
				break;
			case 2:
				final Data data = new Data();
				data.setData("Basic Test - " + (++lastId));
				data.setId("" + lastId);
				data.setType(lastId);
				gigaSpace.write(data);
				break;

			default:
				break;

			}
		}
	}

	
	protected void writeRandomDataEntriesAndWait(final int howManyEntries, final int firstWriteId,
			final int howManyEndMarkers, final long timeout) {
		
	}
	protected void writeRandomDataEntriesAndWait(final GigaSpace gigaSpaceWrite, 
			final GigaSpace gigaSpaceRead,
			final int howManyEntries, final int firstWriteId,
			final int howManyEndMarkers, final long timeout) {
		writeRandomDataEntries(gigaSpaceWrite, howManyEntries, firstWriteId);
		writeEndMarkersAndWait(gigaSpaceWrite, gigaSpaceRead, howManyEndMarkers, timeout);
	}
	protected void compareDataResults() {
		compareDataResults(gigaSpace1.readMultiple(new Data(), Integer.MAX_VALUE),
				gigaSpace2.readMultiple(new Data(), Integer.MAX_VALUE));	
	}
	
	protected void compareDataResults(final Data[] datas1, final Data[] datas2) {
		final DataComparator comparator = new DataComparator();
		Arrays.sort(datas1, comparator);
		Arrays.sort(datas2, comparator);

		Map<String, Data> map1 = new HashMap<String, Data>();
		for (Data data : datas1) {
			map1.put(data.getId(), data);
		}

		Map<String, Data> map2 = new HashMap<String, Data>();
		for (Data data : datas2) {
			map2.put(data.getId(), data);
		}

		assertEquals("Number of items in maps is different", map1.size(), map2.size());

		for (Data data1 : map1.values()) {
			Data data2 = map2.get(data1.getId());
			assertNotNull("Could not find remote object with ID: " + data1.getId(), data2);

			if (data1.getType() != data2.getType() ||
					!data1.getData().equals(data2.getData())) {
				
				final String newLine = System.getProperty("line.separator");
				
				final String errorMessage = "Discovered different objects: " + newLine
						+ "local copy: " + data1.toString() + newLine
						+ "remote copy: " + data2.toString();
				AbstractTest.AssertFail(errorMessage);

			}

		}

	}

	protected void writeDataEntriesToSpace(final int howManyEntries, final int firstId, final String dataPrefix) {
		writeDataEntriesToSpace(gigaSpace1, howManyEntries, firstId, dataPrefix);
	}
	protected void writeDataEntriesToSpace(final GigaSpace gigaSpace, final int howManyEntries, final int firstId, final String dataPrefix) {
		writeDataEntriesToSpace(gigaSpace, howManyEntries, 100, firstId, dataPrefix);
	}
	
	protected void writeDataEntriesToSpace(final int howManyEntries, final int bulkSize, final int firstId, final String dataPrefix) {
		writeDataEntriesToSpace(gigaSpace1, howManyEntries, bulkSize, firstId, dataPrefix);
	}
	protected void writeDataEntriesToSpace(final GigaSpace gigaSpace, final int howManyEntries, final int bulkSize, final int firstId, final String dataPrefix) {
		int totalEntries = 0;
		
		while(totalEntries < howManyEntries) {
			int nextBulkSize = Math.min(bulkSize, howManyEntries - totalEntries);
			Data[] arr = new Data[nextBulkSize];
			for (int i = 0; i < arr.length; i++) {
				final Data data = new Data();
				final int id = (firstId + totalEntries + i);
				data.setData(dataPrefix + id);
				data.setId("" + id);
				data.setType(id);
				arr[i] = data;
			}
			gigaSpace.writeMultiple(arr);
			totalEntries += nextBulkSize;
			
			
		}


	}

}
