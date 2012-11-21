package test.cli.cloudify;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.restclient.RestException;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.pu.service.ServiceMonitors;
import org.testng.annotations.Test;

import test.usm.USMTestUtils;
import framework.utils.LogUtils;

public class ProcessLocatorsTest extends AbstractLocalCloudTest {

	public ProcessLocatorsTest() {
		
	}

	final private String BG_RECIPE_DIR_PATH = CommandTestUtils
			.getPath("apps/USM/usm/locators/simple-background");
	final private String EMPTY_RECIPE_DIR_PATH = CommandTestUtils
			.getPath("apps/USM/usm/locators/empty");

	final private String MULTI_RECIPE_DIR_PATH = CommandTestUtils
			.getPath("apps/USM/usm/locators/multi-simple");

	private boolean isPortOpen(final int port) {
		final Socket socket = new Socket();
		try {
			socket.connect(new InetSocketAddress("127.0.0.1", port));
			return true;
		} catch (final IOException e) {
			return false;
		} finally {
			try {
				socket.close();
			} catch (final IOException e) {
				// ignore
			}
		}

	}

	private ProcessingUnitInstance findPUI(final ProcessingUnit pu, final String serviceFullName)
			throws UnknownHostException {
		final boolean found = pu.waitFor(1, 30, TimeUnit.SECONDS);
		assertTrue("USM Service state is not RUNNING",
				USMTestUtils.waitForPuRunningState(serviceFullName, 20, TimeUnit.SECONDS, admin));
		assertTrue("Could not find instance of deployed service", found);

		final ProcessingUnitInstance pui = pu.getInstances()[0];
		return pui;
	}

	private ProcessingUnit findPUForService(final String puName) {
		final ProcessingUnit pu = this.admin.getProcessingUnits().waitFor(puName, 30, TimeUnit.SECONDS);
		assertNotNull("Could not find processing unit for installed service",
				pu);
		return pu;
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testBackgroundService()
			throws IOException, InterruptedException,
			PackagingException, DSLException, RestException {

		assertTrue("port 7777 already in use", !isPortOpen(7777));
		runCommand("connect " + restUrl + ";install-service --verbose " + BG_RECIPE_DIR_PATH);
		assertTrue("port 7777 not in use, despite service up", isPortOpen(7777));
		final String puName = ServiceUtils.getAbsolutePUName("default", "simple-background");
		assertEquals("unexpected number of pids", 1, getPIDs(puName).size());

		runCommand("connect " + restUrl + ";uninstall-service --verbose simple-background");
		assertTrue("port 7777 still in use", !isPortOpen(7777));

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testEmptyService()
			throws IOException, InterruptedException,
			PackagingException, DSLException, RestException {

		runCommand("connect " + restUrl + ";install-service --verbose " + EMPTY_RECIPE_DIR_PATH);

		final String puName = ServiceUtils.getAbsolutePUName("default", "empty");
		assertEquals("unexpected number of pids", 0, getPIDs(puName).size());

		runCommand("connect " + restUrl + ";uninstall-service --verbose empty");
		

	}

	@SuppressWarnings("unchecked")
	private List<Long> getPIDs(final String puName)
			throws UnknownHostException {
		final ProcessingUnit pu = findPUForService(puName);
		final ProcessingUnitInstance pui = findPUI(pu, puName);
		final Map<String, ServiceMonitors> monitors = pui.getStatistics().getMonitors();
		final ServiceMonitors usmMonitors = monitors.get("USM");
		final Object pidsObject = usmMonitors.getMonitors().get(CloudifyConstants.USM_MONITORS_ACTUAL_PROCESS_ID);

		if (pidsObject == null) {
			return new ArrayList<Long>(0);
		}
		else if (pidsObject instanceof Long) {
			return Arrays.asList((Long) pidsObject);
		} else if (pidsObject instanceof List<?>) {
			return (List<Long>) pidsObject;
		} else {
			LogUtils.log("Unexpected pids monitor value. Pids is: " + pidsObject);
			AssertFail("Unexpected monitors class: " + pidsObject.getClass().getName());
			return null;
		}

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testMultiSimpleService()
			throws IOException, InterruptedException,
			PackagingException, DSLException, RestException {

		assertTrue("port 7777 already in use", !isPortOpen(7777));
		assertTrue("port 7778 already in use", !isPortOpen(7778));
		runCommand("connect " + restUrl + ";install-service --verbose " + MULTI_RECIPE_DIR_PATH);
		assertTrue("port 7777 not in use, despite service up", isPortOpen(7777));
		assertTrue("port 7778 not in use, despite service up", isPortOpen(7778));

		final String puName = ServiceUtils.getAbsolutePUName("default", "multi-simple");
		assertEquals("unexpected number of pids", 2, getPIDs(puName).size());

		runCommand("connect " + restUrl + ";uninstall-service --verbose multi-simple");
		assertTrue("port 7777 still in use", !isPortOpen(7777));
		assertTrue("port 7778 still in use", !isPortOpen(7778));

	}

}
