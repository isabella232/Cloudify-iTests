package test.cli.cloudify;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.restclient.RestException;
import org.hyperic.sigar.SigarException;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.pu.service.ServiceMonitors;
import org.testng.annotations.Test;

import test.usm.USMTestUtils;

import com.gigaspaces.log.AllLogEntryMatcher;
import com.gigaspaces.log.ContinuousLogEntryMatcher;
import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogProcessType;

import framework.utils.LogUtils;
import framework.utils.SigarUtils;

public class StopLifecycleTest extends AbstractLocalCloudTest {

	final private String RECIPE_DIR_PATH = CommandTestUtils
			.getPath("apps/USM/usm/stopTest");

	// set in checkMonitors
	private long actualPid;

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testStopLifecycle()
			throws IOException, InterruptedException,
			PackagingException, DSLException, RestException, SigarException {

		installService();

		final ProcessingUnit pu = findPU();

		final ProcessingUnitInstance pui = findPUI(pu);

		final long gscPid = pui.getGridServiceContainer().getVirtualMachine()
				.getDetails().getPid();

		checkMonitors(pui);

		LogUtils.log("Actual Process PID is: " + this.actualPid);

		final ContinuousLogEntryMatcher matcher = new ContinuousLogEntryMatcher(
				new AllLogEntryMatcher(), new AllLogEntryMatcher());

		uninstallService();

		// test shutdown events

		final boolean foundStopText = checkForStopPrintout(pui, gscPid, matcher);
		assertTrue("Did not find expected stop event text", foundStopText);

		
		assertTrue("Process is expected to be alive", SigarUtils.isProcessAlive(this.actualPid));

		SigarUtils.killProcess(this.actualPid);
		
	}

	private boolean checkForStopPrintout(final ProcessingUnitInstance pui,
			final long pid, final ContinuousLogEntryMatcher matcher) {
		final LogEntries shutdownEntries = pui.getGridServiceContainer()
				.getGridServiceAgent()
				.logEntries(LogProcessType.GSC, pid, matcher);
		for (final LogEntry logEntry : shutdownEntries) {
			final String text = logEntry.getText();
			if (text.contains("This is the Stop event")) {
				return true;
			}
		}

		return false;

	}

	private void checkMonitors(final ProcessingUnitInstance pui) {
		// verify monitors

		final Map<String, Object> allMonitors = new HashMap<String, Object>();

		final Collection<ServiceMonitors> allSserviceMonitors = pui.getStatistics()
				.getMonitors().values();

		for (final ServiceMonitors serviceMonitors : allSserviceMonitors) {
			allMonitors.putAll(serviceMonitors.getMonitors());
		}

		this.actualPid = (Long) allMonitors.get(CloudifyConstants.USM_MONITORS_ACTUAL_PROCESS_ID);
		assertTrue("Actual PID should not be zero", this.actualPid > 0);
	}

	private ProcessingUnitInstance findPUI(final ProcessingUnit pu)
			throws UnknownHostException {
		final boolean found = pu.waitFor(1, 30, TimeUnit.SECONDS);
		assertTrue("Could not find instance of deployed service", found);

		assertTrue("USM Service state is not RUNNING",
				USMTestUtils.waitForPuRunningState("default.simple", 20, TimeUnit.SECONDS, admin));

		final ProcessingUnitInstance pui = pu.getInstances()[0];

		return pui;
	}

	private ProcessingUnit findPU() {
		final ProcessingUnit pu = this.admin.getProcessingUnits().waitFor(
				ServiceUtils.getAbsolutePUName("default", "simple"), 30, TimeUnit.SECONDS);
		assertNotNull("Could not find processing unit for installed service",
				pu);
		return pu;
	}

	private void installService()
			throws PackagingException, IOException, InterruptedException, DSLException {
		runCommand("connect " + restUrl + ";install-service --verbose " + RECIPE_DIR_PATH);
	}

	private void uninstallService()
			throws PackagingException, IOException, InterruptedException, DSLException {
		runCommand("connect " + restUrl + ";uninstall-service --verbose simple");
	}

	private boolean isPortOpen() {
		final Socket socket = new Socket();
		try {
			socket.connect(new InetSocketAddress("127.0.0.1", 7777));
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

}
