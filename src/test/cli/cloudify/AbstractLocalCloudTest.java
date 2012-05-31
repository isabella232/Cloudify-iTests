package test.cli.cloudify;

import static framework.utils.LogUtils.log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.jms.IllegalStateException;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.restclient.ErrorStatusException;
import org.cloudifysource.restclient.StringUtils;
import org.cloudifysource.shell.commands.CLIException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.hyperic.sigar.ProcExe;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarPermissionDeniedException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import test.AbstractTest;

import com.gigaspaces.internal.sigar.SigarHolder;

import framework.utils.DumpUtils;
import framework.utils.LogUtils;
import framework.utils.PortConnectionUtils;
import framework.utils.ScriptUtils;
import framework.utils.SetupUtils;
import framework.utils.TeardownUtils;

public class AbstractLocalCloudTest extends AbstractTest {

	protected final int WAIT_FOR_TIMEOUT = 20;
	private final int HTTP_STATUS_OK = 200;
	private final int restPort = 8100;
	protected static String restUrl = null;
	protected static final String DEFAULT_APPLICATION_NAME = "default";
	private static Set<String> clientStartupPIDs = null;
	private static Set<String> localCloudPIDs = null;
	private static Set<String> alivePIDs = null;

	protected boolean isDevEnv = false;

	protected boolean checkIsDevEnv() {
		if (this.isDevEnv) {
			return true;
		}

		final String val = System.getenv("DEV_ENV");
		if (val != null) {
			if (val.equalsIgnoreCase("true")) {
				return true;
			}
		}

		final String propVal = System.getProperty("localcloud.DEV_ENV");
		if (propVal != null) {
			if (propVal.equalsIgnoreCase("true")) {
				return true;
			}
		}

		return false;

	}

	@BeforeSuite
	public void beforeSuite()
			throws Exception {
		if (checkIsDevEnv()) {
			LogUtils.log("Local cloud test running in dev mode, will use existing localcloud");
			clientStartupPIDs = new HashSet<String>();
		} else {
			cleanUpCloudifyLocalDir();
			scanForLeakedProcesses(false);

			LogUtils.log("Tearing-down existing localclouds");
			try {
				runCommand("teardown-localcloud -force");
			} catch (final AssertionError e) {
				LogUtils.log("teardown failed because no cloud was found. proceeding with suite");
			}

			clientStartupPIDs = SetupUtils.getLocalProcesses();

			try {
				LogUtils.log("Performing bootstrap");
				final boolean portOpenBeforeBootstrap = PortConnectionUtils.isPortOpen("localhost",
						restPort);
				assertTrue("port " + restPort
						+ " is open on localhost before rest deployment. will not try to deploy rest",
						!portOpenBeforeBootstrap);
				runCommand("bootstrap-localcloud --verbose -timeout 15");
			} catch (final Exception e) {
				LogUtils.log("Booststrap Failed." + e);
				e.printStackTrace();
			}
		}

		try {
			this.admin = getAdminWithLocators();
		} catch (final UnknownHostException e1) {
			LogUtils.log("Could not create admin " + e1);
			e1.printStackTrace();
		}
		assertTrue("Could not find LUS of local cloud",
				admin.getLookupServices().waitFor(1,
						WAIT_FOR_TIMEOUT,
						TimeUnit.SECONDS));
		try {
			restUrl = "http://" + InetAddress.getLocalHost().getHostAddress() + ":" + restPort;
		} catch (final UnknownHostException e) {
			e.printStackTrace();
		}
		try {
			alivePIDs = SetupUtils.getLocalProcesses();
			localCloudPIDs = SetupUtils.getClientProcessesIDsDelta(clientStartupPIDs,
					alivePIDs);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private void cleanUpCloudifyLocalDir()
			throws IOException {
		String userHomeProp = null;
		if (ScriptUtils.isLinuxMachine()) {
			userHomeProp = System.getProperty("user.home");
		} else {
			// TODO eli - fix this hack. not very nice and generic
			userHomeProp = System.getProperty("user.dir") + "/../../"; // windows machine
		}
		final File userHomeDir = new File(userHomeProp, ".cloudify");
		LogUtils.log("Cleaning up cloudify folder under 'user.home' folder at: " + userHomeDir);

		if (!userHomeDir.exists()) {
			LogUtils.log(userHomeDir + " does not exist");
		} else {
			if (!userHomeDir.isDirectory()) {
				LogUtils.log(userHomeDir + " is not a directory!");
			} else {
				FileUtils.cleanDirectory(userHomeDir);
			}
		}
	}

	@BeforeClass
	public void beforeClass()
			throws Exception {
		LogUtils.log("Test Class Configuration Started: " + this.getClass());
		if (this.admin == null) {
			try {
				this.admin = getAdminWithLocators();
			} catch (final UnknownHostException e1) {
				LogUtils.log("Could not create admin " + e1);
				throw e1;
			}
		}

		if (!admin.getMachines().waitFor(1,
				30,
				TimeUnit.SECONDS)) {
			// Admin API did not find anything!

			throw new IllegalStateException(
					"Could not find any machines in Admin API! There is probably a discovery issue");
		}

		Machine machine = admin.getMachines().getMachines()[0];
		System.out.println("Machine [" + machine.getHostName() + "], " + "TotalPhysicalMem ["
				+ machine.getOperatingSystem().getDetails().getTotalPhysicalMemorySizeInGB() + "GB], "
				+ "FreePhysicalMem [" + machine.getOperatingSystem().getStatistics().getFreePhysicalMemorySizeInGB()
				+ "GB]]");
	}

	@Override
	@BeforeMethod
	public void beforeTest() {
		LogUtils.log("Test Configuration Started: " + this.getClass());
		System.out.println("Machine ["
				+ admin.getMachines().getMachines()[0].getHostName()
				+ "], "
				+ "TotalPhysicalMem ["
				+ admin.getMachines().getMachines()[0].getOperatingSystem().getDetails()
						.getTotalPhysicalMemorySizeInGB()
				+ "GB], "
				+ "FreePhysicalMem ["
				+ admin.getMachines().getMachines()[0].getOperatingSystem().getStatistics()
						.getFreePhysicalMemorySizeInGB() + "GB]]");
	}

	private static class ProcessDetails {

		private long pid;
		private String baseName;
		private String fullName;
		private String[] args;
		private long parentPid;

		@Override
		public String toString() {
			return "ProcessDetails [pid=" + pid + ", baseName=" + baseName + ", fullName=" + fullName + ", args="
					+ Arrays.toString(args) + ", parentPid=" + parentPid + "]";
		}

	}

	private static final Set<String> suspectProcessNames = new HashSet<String>(Arrays.asList("mongod", "mongos", "nc"));
	private static final Set<String> suspectJavaProcessNames = new HashSet<String>(Arrays.asList(
			"org.codehaus.groovy.tools.GroovyStarter", // groovy script executable
			"simplejavaprocess.jar",
			"org.apache.catalina.startup.Bootstrap",
			"org.apache.cassandra.thrift.CassandraDaemon"));

	private void scanForLeakedProcesses(final boolean failOnLeak)
			throws SigarException {
		final Map<Long, ProcessDetails> processTable = createProcessTable();

		boolean failed = false;
		final Set<Entry<Long, ProcessDetails>> entries = processTable.entrySet();
		for (final Entry<Long, ProcessDetails> entry : entries) {
			final long pid = entry.getKey();
			final ProcessDetails procDetails = entry.getValue();

			if (procDetails.baseName.contains("java")) {
				final String[] args = procDetails.args;
				for (final String arg : args) {
					if (suspectJavaProcessNames.contains(arg)) {
						LogUtils.log("Found a leaking java process (" + arg + "): " + procDetails);
						failed = true;
						SetupUtils.killProcessesByIDs(new HashSet<String>(Arrays.asList("" + pid)));
					}
				}
			} else if (suspectProcessNames.contains(procDetails.baseName)) {
				LogUtils.log("Found a leaking process: " + procDetails);
				failed = true;
				SetupUtils.killProcessesByIDs(new HashSet<String>(Arrays.asList("" + pid)));
			}

		}

		if (failed && failOnLeak) {
			LogUtils.log("Leaked process scan found and killed at least one process. Restarting the local cloud");
			try {
				beforeSuite();
			} catch (Exception e) {
				LogUtils.log("Failed to restart local cloud", e);
			}
			AssertFail("At least one leaked process was found and removed!");

		}

	}

	private Map<Long, ProcessDetails> createProcessTable()
			throws SigarException
	{
		final Sigar sigar = SigarHolder.getSigar();
		final long[] allpids = sigar.getProcList();

		final Map<Long, ProcessDetails> processDetailsByPid = new HashMap<Long, ProcessDetails>();

		for (final long pid : allpids) {
			try {
				final ProcessDetails details = new ProcessDetails();
				final ProcState state = sigar.getProcState(pid);
				details.baseName = state.getName();
				details.parentPid = state.getPpid();

				final ProcExe exe = sigar.getProcExe(pid);
				details.fullName = exe.getName();
				details.args = sigar.getProcArgs(pid);

				details.pid = pid;
				processDetailsByPid.put(pid, details);

			} catch (SigarPermissionDeniedException e) {
				// ignore
			} catch (SigarException e) {
				// this often happens for security reasons, as procs from other users will fail on this.
				LogUtils.log("Failed to read process details for pid: " + pid + ". Error was: " + e.getMessage());
			}
		}

		return processDetailsByPid;
	}

	@Override
	@AfterMethod(alwaysRun = true)
	public void afterTest()
			throws Exception {

		if (admin != null) {
			TeardownUtils.snapshot(admin);
			uninstallAllRunningServices(admin);
		}

		try {
			scanForLeakedProcesses(true);
		} catch (SigarException e) {
			LogUtils.log("WARNING! Failed to scan for leaked processes using sigar!", e);
		}

		if (alivePIDs != null) {

			final Set<String> currentPids = SetupUtils.getLocalProcesses();
			final Set<String> delta = SetupUtils.getClientProcessesIDsDelta(alivePIDs,
					currentPids);

			if (delta.size() > 0) {
				String pids = "";
				for (final String pid : delta) {
					pids += pid + ", ";
				}
				try {
					LogUtils.log("WARNING There is a leak PIDS [ " + pids + "] are alive");
					Sigar sigar = SigarHolder.getSigar();
					for (String pid : delta) {
						try {
							LogUtils.log("PID: " + pid + ": " + sigar.getProcExe(pid).getName());
						} catch (SigarException e) {
							LogUtils.log("Failed to get process info for pid: " + pid);
						}

					}

					if (!checkIsDevEnv()) {
						SetupUtils.killProcessesByIDs(delta);
						LogUtils.log("INFO killing all orphan processes");
						SetupUtils.killProcessesByIDs(localCloudPIDs);
						LogUtils.log("INFO killing local cloud processes and boostraping again");
					}

				} finally {
					if (!checkIsDevEnv()) {
						beforeSuite();
					}
				}
			}

		}
		LogUtils.log("Test Finished : " + this.getClass());
	}

	@AfterSuite(alwaysRun = true)
	public void afterSuite() {
		if (checkIsDevEnv()) {
			LogUtils.log("Running in dev mode - cloud will not be torn down");
		} else {
			try {
				LogUtils.log("Tearing-down localcloud");
				runCommand("teardown-localcloud  -force");
			} catch (final Exception e) {
				e.printStackTrace();
			}

			try {
				TeardownUtils.teardownAll(admin);
			} catch (final Throwable t) {
				log("failed to teardown", t);
			}
			if (admin != null)
				admin.close();
			admin = null;
		}
	}

	private Admin getAdminWithLocators()
			throws UnknownHostException {
		// Class LocalhostGridAgentBootsrapper defines the locator discovery addresses.
		final String nicAddress = "127.0.0.1"; // Constants.getHostAddress();

		// int defaultLusPort = Constants.getDiscoveryPort();
		final AdminFactory factory = new AdminFactory();
		LogUtils.log("adding locator to admin : " + nicAddress + ":" + CloudifyConstants.DEFAULT_LOCALCLOUD_LUS_PORT);
		factory.addLocator(nicAddress + ":" + CloudifyConstants.DEFAULT_LOCALCLOUD_LUS_PORT);
		return factory.createAdmin();
	}

	// This method implementation is used in order to access the admin api
	// without having to worry about locators issue.
	protected Map<String, Object> getAdminData(final String relativeUrl)
			throws CLIException, ErrorStatusException {
		final String url = getFullUrl("/admin/" + relativeUrl);
		LogUtils.log("performing http get to url: " + url);
		final HttpGet httpMethod = new HttpGet(url);
		return readHttpAdminMethod(httpMethod);
	}

	private String getFullUrl(final String relativeUrl) {
		return restUrl + relativeUrl;
	}

	private Map<String, Object> readHttpAdminMethod(final HttpRequestBase httpMethod)
			throws CLIException, ErrorStatusException {
		InputStream instream = null;
		try {
			final DefaultHttpClient httpClient = new DefaultHttpClient();
			final HttpResponse response = httpClient.execute(httpMethod);
			if (response.getStatusLine().getStatusCode() != HTTP_STATUS_OK) {
				LogUtils.log(httpMethod.getURI() + " response code " + response.getStatusLine().getStatusCode());
				throw new CLIException(response.getStatusLine().toString());
			}
			final HttpEntity entity = response.getEntity();
			if (entity == null) {
				final ErrorStatusException e = new ErrorStatusException("comm_error");
				LogUtils.log(httpMethod.getURI() + " response entity is null",
						e);
				throw e;
			}
			instream = entity.getContent();
			final String responseBody = StringUtils.getStringFromStream(instream);
			LogUtils.log(httpMethod.getURI() + " http get response: " + responseBody);
			final Map<String, Object> responseMap = jsonToMap(responseBody);
			return responseMap;
		} catch (final ClientProtocolException e) {
			LogUtils.log(httpMethod.getURI() + " Rest api error",
					e);
			throw new ErrorStatusException("comm_error", e, e.getMessage());
		} catch (final IOException e) {
			LogUtils.log(httpMethod.getURI() + " Rest api error",
					e);
			throw new ErrorStatusException("comm_error", e, e.getMessage());
		} finally {
			if (instream != null) {
				try {
					instream.close();
				} catch (final IOException e) {
				}
			}
			httpMethod.abort();
		}
	}

	// returns the number of processing unit instances of the specified service
	protected int getProcessingUnitInstanceCount(final String absolutePUName)
			throws CLIException, ErrorStatusException {
		final String puNameAdminUrl = "processingUnits/Names/" + absolutePUName;
		final Map<String, Object> mongoProcessingUnitAdminData = getAdminData(puNameAdminUrl);
		return (Integer) mongoProcessingUnitAdminData.get("Instances-Size");
	}

	private static Map<String, Object> jsonToMap(final String response)
			throws IOException {
		final JavaType javaType = TypeFactory.type(Map.class);
		final ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.readValue(response,
				javaType);
	}

	protected String runCommand(final String command)
			throws IOException, InterruptedException {
		return CommandTestUtils.runCommandAndWait(command);
	}

	protected void uninstallApplication(final String applicationName) {
		try {
			DumpUtils.dumpLogs(admin);
			runCommand("connect " + restUrl + ";uninstall-application " + applicationName);
		} catch (final Exception e) {
			LogUtils.log("Failed to uninstall " + applicationName,
					e);
			e.printStackTrace();
		}
	}

	protected void uninstallService(final String serviceName) {
		try {
			DumpUtils.dumpLogs(admin);
			runCommand("connect " + restUrl + ";uninstall-service " + serviceName);
		} catch (final Exception e) {
			LogUtils.log("Failed to uninstall " + serviceName,
					e);
			e.printStackTrace();
		}
	}

	public void uninstallAllRunningServices(final Admin admin) {
		DumpUtils.dumpLogs(admin);
		for (final ProcessingUnit pu : admin.getProcessingUnits().getProcessingUnits()) {
			if (!pu.getName().equals("webui") && !pu.getName().equals("rest")
					&& !pu.getName().equals("cloudifyManagementSpace")) {
				if (!pu.undeployAndWait(30,
						TimeUnit.SECONDS)) {
					LogUtils.log("Failed to uninstall " + pu.getName());
				} else {
					LogUtils.log("Uninstalled service: " + pu.getName());
				}
			}
		}
	}

	public void updateLocalCloudPids(final long oldPid, final long newPid) {
		localCloudPIDs.remove(oldPid);
		localCloudPIDs.add(String.valueOf(newPid));
		alivePIDs.remove(oldPid);
		alivePIDs.add(String.valueOf(newPid));
	}
}