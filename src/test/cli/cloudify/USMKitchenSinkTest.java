package test.cli.cloudify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.pu.service.ServiceDetails;
import org.openspaces.pu.service.ServiceMonitors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.cloudify.dsl.internal.CloudifyConstants;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;
import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;
import com.gigaspaces.log.AllLogEntryMatcher;
import com.gigaspaces.log.ContinuousLogEntryMatcher;
import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogProcessType;

public class USMKitchenSinkTest extends AbstractCommandTest {

	private static final String LOCAL_GROUP_NAME = "kitchensinktest";

	@Override
	@BeforeMethod
	public void beforeTest() {
	}

	final private String RECIPE_DIR_PATH = CommandTestUtils
			.getPath("apps/USM/usm/kitchensink");

	private static final String[] EXPECTED_STARTUP_EVENT_STRINGS = {
			"init fired Test Property number 1",
			"preInstall fired Test Property number 2",
			"postInstall fired Test Property number 1",
			"preStart fired Test Property number 2", "postStart fired",
			"Instantiated kitchensink-service" };
	private static final String[] EXPECTED_SHUTDOWN_EVENT_STRINGS = {
			"preStop fired", "String_with_Spaces", "postStop fired",
			"shutdown fired" };

	private static final String EXPECTED_DETAILS_FIELDS[] = { "Details",
			"Counter", "Type" };

	private static final String EXPECTED_MONITORS_FIELDS[] = {
			"Counter", CloudifyConstants.USM_MONITORS_CHILD_PROCESS_ID,
			CloudifyConstants.USM_MONITORS_ACTUAL_PROCESS_ID };

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testKitchenSink() throws IOException, InterruptedException,
			PackagingException {

		installService();

		ProcessingUnit pu = findPU();

		ProcessingUnitInstance pui = findPUI(pu);

		// check port is open
		final String host = pui.getGridServiceContainer().getMachine()
				.getHostAddress();

		assertTrue("Process port is not open! Process did start as expected",
				isPortOpen(host));

		long pid = pui.getGridServiceContainer().getVirtualMachine()
				.getDetails().getPid();

		ContinuousLogEntryMatcher matcher = new ContinuousLogEntryMatcher(
				new AllLogEntryMatcher(), new AllLogEntryMatcher());

		checkForStartupPrintouts(pui, pid, matcher);

		// verify details
		checkDetails(pui);

		checkMonitors(pui);

		checkCustomCommands();

		// undeploy
		pu.undeploy();

		// test shutdown events
		checkForShutdownPrintouts(pui, pid, matcher);

		// check port closed.
		assertTrue(
				"Process port is still open! Process did not shut down as expected",
				!isPortOpen(host));

	}

	private void checkForShutdownPrintouts(ProcessingUnitInstance pui,
			long pid, ContinuousLogEntryMatcher matcher) {
		LogEntries shutdownEntries = pui.getGridServiceContainer()
				.getGridServiceAgent()
				.logEntries(LogProcessType.GSC, pid, matcher);
		int shutdownEventIndex = 0;
		for (LogEntry logEntry : shutdownEntries) {
			String text = logEntry.getText();
			if (text.contains(EXPECTED_SHUTDOWN_EVENT_STRINGS[shutdownEventIndex])) {
				++shutdownEventIndex;
				if (shutdownEventIndex == EXPECTED_SHUTDOWN_EVENT_STRINGS.length) {
					break;
				}
			}
		}

		if (shutdownEventIndex != EXPECTED_SHUTDOWN_EVENT_STRINGS.length) {
			AssertFail("An event was not fired. Missing event details: "
					+ EXPECTED_SHUTDOWN_EVENT_STRINGS[shutdownEventIndex]);
		}
	}

	private void checkCustomCommands() throws IOException, InterruptedException {
		// test custom commands
		String invoke1Result = runCommand("connect " + this.restUrl
				+ "; invoke kitchensink-service cmd1");

		if ((!invoke1Result.contains("1: OK"))
				|| (!invoke1Result.contains("Result: null"))) {
			AssertFail("Custom command cmd1 returned unexpected result: "
					+ invoke1Result);
		}

		String invoke2Result = runCommand("connect " + this.restUrl
				+ "; invoke kitchensink-service cmd2");

		if ((!invoke2Result.contains("1: FAILED"))
				|| (!invoke2Result
						.contains("This is the cmd2 custom command - This is an error test"))) {
			AssertFail("Custom command cmd2 returned unexpected result: "
					+ invoke2Result);
		}

		String invoke3Result = runCommand("connect " + this.restUrl
				+ "; invoke kitchensink-service cmd3");
		if ((!invoke3Result.contains("1: OK"))
				|| (!invoke3Result
						.contains("Result: This is the cmd3 custom command. Service Dir is:"))) {
			AssertFail("Custom command cmd3 returned unexpected result: "
					+ invoke3Result);
		}
		String invoke4Result = runCommand("connect " + this.restUrl
				+ "; invoke kitchensink-service cmd4");
		if ((!invoke4Result.contains("1: OK"))
				|| (!invoke4Result.contains("context_command"))
//				|| (!invoke4Result.contains("instance is:"))
				) {
			AssertFail("Custom command cmd4 returned unexpected result: "
					+ invoke4Result);
		}
		
		String invoke5Result = runCommand("connect " + this.restUrl
				+ "; invoke kitchensink-service cmd5 ['x=2' 'y=3']");

		if ((!invoke5Result.contains("1: OK"))
				|| (!invoke5Result.contains("this is the custom parameters command. expecting 123: 123"))) {
			AssertFail("Custom command cmd5 returned unexpected result: "
					+ invoke1Result);
		}
	}

	private void checkMonitors(ProcessingUnitInstance pui) {
		// verify monitors
		Collection<ServiceMonitors> allSserviceMonitors = pui.getStatistics()
				.getMonitors().values();
		Map<String, Object> allMonitors = new HashMap<String, Object>();
		for (ServiceMonitors serviceMonitors : allSserviceMonitors) {
			allMonitors.putAll(serviceMonitors.getMonitors());
		}

		for (String monitorKey : EXPECTED_MONITORS_FIELDS) {
			assertTrue("Missing Monitor Key: " + monitorKey,
					allMonitors.containsKey(monitorKey));
		}
	}

	private void checkDetails(ProcessingUnitInstance pui) {
		Collection<ServiceDetails> allServiceDetails = pui
				.getServiceDetailsByServiceId().values();
		Map<String, Object> allDetails = new HashMap<String, Object>();
		for (ServiceDetails serviceDetails : allServiceDetails) {
			allDetails.putAll(serviceDetails.getAttributes());
		}
		for (String detailKey : EXPECTED_DETAILS_FIELDS) {
			assertTrue("Missing details entry: " + detailKey,
					allDetails.containsKey(detailKey));
		}
	}

	private ContinuousLogEntryMatcher checkForStartupPrintouts(
			ProcessingUnitInstance pui, long pid,
			ContinuousLogEntryMatcher matcher) {

		int startupEventIndex = 0;
		LogEntries entries = pui.getGridServiceContainer()
				.getGridServiceAgent()
				.logEntries(LogProcessType.GSC, pid, matcher);
		for (LogEntry logEntry : entries) {
			String text = logEntry.getText();
			if (text.contains(EXPECTED_STARTUP_EVENT_STRINGS[startupEventIndex])) {
				++startupEventIndex;
				if (startupEventIndex == EXPECTED_STARTUP_EVENT_STRINGS.length) {
					break;
				}
			}
		}

		if (startupEventIndex != EXPECTED_STARTUP_EVENT_STRINGS.length) {
			AssertFail("An event was not fired. Missing event details: "
					+ EXPECTED_STARTUP_EVENT_STRINGS[startupEventIndex]);
		}
		return matcher;
	}

	private ProcessingUnitInstance findPUI(ProcessingUnit pu) {
		boolean found = pu.waitFor(1, 30, TimeUnit.SECONDS);
		assertTrue("Could not find instance of deployed service", found);

		ProcessingUnitInstance pui = pu.getInstances()[0];
		return pui;
	}

	private ProcessingUnit findPU() {
		ProcessingUnit pu = this.admin.getProcessingUnits().waitFor(
				"kitchensink-service", 30, TimeUnit.SECONDS);
		assertNotNull("Could not find processing unit for installed service",
				pu);
		return pu;
	}

	private void installService() throws FileNotFoundException,
			PackagingException, IOException, InterruptedException {
		File serviceDir = new File(RECIPE_DIR_PATH);
		ServiceReader.getServiceFromDirectory(serviceDir).getService();

		runCommand("bootstrap-localcloud;install-service --verbose " + RECIPE_DIR_PATH);
		AdminFactory factory = new AdminFactory();
		factory.addLocator(InetAddress.getLocalHost().getHostAddress() + ":4168");
		this.admin = factory.create();
		assertTrue("Could not find LUS of local cloud", admin.getLookupServices().waitFor(1, 10, TimeUnit.SECONDS));
		
		this.restUrl = "http://" + InetAddress.getLocalHost().getHostAddress() + ":8100";
	}

	private boolean isPortOpen(String host) {
		Socket socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(host, 7777));
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				// ignore
			}
		}

	}

	@Override
	@AfterMethod
	public void afterTest() {
		try {
			runCommand("teardown-localcloud");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}
}
