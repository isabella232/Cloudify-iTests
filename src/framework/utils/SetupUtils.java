package framework.utils;

import static framework.utils.LogUtils.log;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudifysource.usm.shutdown.DefaultProcessKiller;
import org.openspaces.admin.Admin;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.testng.Assert;
import org.testng.AssertJUnit;

import test.cli.cloudify.CommandTestUtils;

/**
 * Utility methods for test setup.
 *
 * @author Moran Avigdor
 */
public class SetupUtils {

    private static final long DEFAULT_CLEAN_SETUP_TIMEOUT_MINUTES = 1;

    private static final int SSH_TIMEOUT = 60000;

    private static Map<String, Set<String>> gsaStartupPIDs = null;

    public static final String USERNAME = "tgrid";
    public static final String PASSWORD = "tgrid";
    public static final String LINUX_HOST_PREFIX = "192.168.9";
    private static final String LIST_GSA_PROCESSES_CMD =
            "ps -ww u -U tgrid | grep java | grep GSA | grep -v PID | awk '{ print $2 }'";
    private static final String LIST_LUS_PROCESSES_CMD =
            "ps -ww u -U tgrid | grep java | grep LH | grep -v PID | awk '{ print $2 }'";
    private static final String LIST_JAVA_PROCESSES_CMD = "ps -U tgrid | grep java | grep -v PID | awk '{ print $1 }'";

    public static void assertCleanSetup(Admin admin) {
        assertCleanSetup(admin, DEFAULT_CLEAN_SETUP_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * asserts that there is a clean setup before the test is run
     */
    public static void assertCleanSetup(Admin admin, long timeout, TimeUnit timeunit) {

        long end = System.currentTimeMillis() + timeunit.toMillis(timeout);

        AssertJUnit.assertNotNull(admin.getGridServiceAgents().waitForAtLeastOne(timeout, timeunit));

        while (true) {

            try {
                // sleep for two seconds to allow discovery to find whatever is left
                // around
                Thread.sleep(2000);
            } catch (InterruptedException e1) {
                // ignore
            }

            SetupUtils.cleanSetup(admin);

            boolean isDone = true;

            int numberOfElasticServiceManagers = admin.getElasticServiceManagers().getSize();
            if (numberOfElasticServiceManagers > 0) {
                LogUtils.log("Waiting for " + numberOfElasticServiceManagers + " ElasticServiceManagers to shutdown");
                isDone = false;
            }

            int numberOfGridServiceManagers = admin.getGridServiceManagers().getSize();
            if (numberOfGridServiceManagers > 0) {
                LogUtils.log("Waiting for " + numberOfGridServiceManagers + " GridServiceManagers to shutdown");
                isDone = false;
            }

            int numberOfGridServiceContainers = admin.getGridServiceContainers().getSize();
            if (numberOfGridServiceContainers > 0) {
                LogUtils.log("Waiting for " + numberOfGridServiceContainers + " GridServiceContainers to shutdown");
                isDone = false;
            }

            int numberOfProcessingUnits = admin.getProcessingUnits().getSize();
            if (numberOfProcessingUnits > 0) {
                LogUtils.log("Waiting for " + numberOfProcessingUnits + " ProcessingUnits to shutdown");
                isDone = false;
            }

            if (isDone) {
                break;
            }

            if (System.currentTimeMillis() > end) {
                try {
                    TeardownUtils.snapshot(admin);
                } catch (Throwable t) {
                    LogUtils.log("Failed on snapshot", t);
                }

                AssertJUnit.fail("Failed to cleanup existing processes beforeTest. Timeout = " + timeout + " " + timeunit.toString());
            }
        }
        assertEquals("Unexpected ESM", 0, admin.getElasticServiceManagers().getSize());
        assertEquals("Unexpected GSM", 0, admin.getGridServiceManagers().getSize());
        assertEquals("Unexpected GSC", 0, admin.getGridServiceContainers().getSize());
        assertEquals("Unexpected PUs", 0, admin.getProcessingUnits().getSize());
    }

    private static void cleanSetup(Admin admin) {
        /*
           * assert that we have a clean environment - try to kill any left overs
           */
        for (ElasticServiceManager esm : admin.getElasticServiceManagers()) {
            try {
                log("killing ESM [ID:" + esm.getAgentId() + "] [PID: " + esm.getVirtualMachine().getDetails().getPid() + " ]");
                esm.getMachine().getGridServiceAgents().waitForAtLeastOne(30, TimeUnit.SECONDS);
                esm.kill();
            } catch (Exception e) {
                LogUtils.log("Failed to kill ESM", e);
            }
        }

        for (GridServiceManager gsm : admin.getGridServiceManagers()) {
            try {
                log("killing GSM [ID:" + gsm.getAgentId() + "] [PID: " + gsm.getVirtualMachine().getDetails().getPid() + " ]");
                gsm.getMachine().getGridServiceAgents().waitForAtLeastOne(30, TimeUnit.SECONDS);
                gsm.kill();
            } catch (Exception e) {
                LogUtils.log("Failed to kill GSM", e);
            }
        }

        for (GridServiceContainer gsc : admin.getGridServiceContainers()) {
            try {
                log("killing GSC [ID:" + gsc.getAgentId() + "] [PID: " + gsc.getVirtualMachine().getDetails().getPid() + " ]");
                gsc.getMachine().getGridServiceAgents().waitForAtLeastOne(30, TimeUnit.SECONDS);
                gsc.kill();
            } catch (Exception e) {
                LogUtils.log("Failed to kill GSC on machine " + gsc.getMachine().getHostAddress(), e);
            }
        }

//        if (gsaStartupPIDs != null) {
//            setupCleanEnvironment(admin);
//        }
    }

    private static void setupCleanEnvironment(Admin admin) {

        Map<String, Set<String>> currentGSAProcessesId = getProcessesIDsByCommand(admin, LIST_GSA_PROCESSES_CMD);

        log("current GSA Processes Id amount = " + currentGSAProcessesId.size());
        log("GSA start up PID amount = " + gsaStartupPIDs.size());
        if (currentGSAProcessesId.size() < gsaStartupPIDs.size()) {
            recoverBrokenEnvironmnet(admin, currentGSAProcessesId);
        }

        Map<String, Set<String>> endProcessesIDs = getCurrentProcessesIDs(admin);

        Map<String, Set<String>> gsaAndLusProcessesId = new HashMap<String, Set<String>>(gsaStartupPIDs);
        Map<String, Set<String>> lusProcessesId = getProcessesIDsByCommand(admin, LIST_LUS_PROCESSES_CMD);
        for (Entry<String, Set<String>> entry : lusProcessesId.entrySet()) {
            if (gsaAndLusProcessesId.containsKey(entry.getKey())) {
                gsaAndLusProcessesId.get(entry.getKey()).addAll(entry.getValue());
            } else {
                gsaAndLusProcessesId.put(entry.getKey(), entry.getValue());
            }
        }

        Map<String, Set<String>> deltaProcessesIDs = getProcessesIDsDelta(gsaAndLusProcessesId, endProcessesIDs);
        killProcessesByIDs(deltaProcessesIDs);
    }

    private static void recoverBrokenEnvironmnet(Admin admin, Map<String, Set<String>> currentGSAPIDs) {

        for (String hostAddress : gsaStartupPIDs.keySet()) {

            if (currentGSAPIDs.containsKey(hostAddress)) {
                continue;
            }

            log("Expected finding a GSA on " + hostAddress + ". starting agent.");

            // clear old PIDs
            gsaStartupPIDs.get(hostAddress).clear();

            String command = "killall -9 java;"; // kill leftovers
            command += "LOOKUPGROUPS=" + admin.getGroups()[0] + ";";
            command += "export LOOKUPGROUPS;";
            command += ScriptUtils.getBuildBinPath() + "/gs-agent.sh gsa.global.lus 2 gsa.global.gsm 0 gsa.gsc 0";

            log("running '" + command + "' on " + hostAddress);
            SSHUtils.runCommand(hostAddress, SSH_TIMEOUT, command, USERNAME, PASSWORD);

        }

        log("Waiting for 4 GSAs");
        assertTrue(admin.getGridServiceAgents().waitFor(4, 120, TimeUnit.SECONDS));

        for (String hostAddress : gsaStartupPIDs.keySet()) {
            if (gsaStartupPIDs.get(hostAddress).size() == 0) {

                log("Rebuilding PID list for GSAs on" + hostAddress);

                // get new PIDs

                log("running '" + LIST_GSA_PROCESSES_CMD + "' on " + hostAddress);

                String output =
                        SSHUtils.runCommand(hostAddress, SSH_TIMEOUT, LIST_GSA_PROCESSES_CMD, USERNAME, PASSWORD);
                String[] lines = output.split("\n");

                log("Finished executing command. updating list");

                for (String pid : lines) {
                    log("adding: " + pid);
                    gsaStartupPIDs.get(hostAddress).add(pid);
                }
            }
        }
    }

    public static void fetchStartProcessesIDs(Admin admin) {
        gsaStartupPIDs = getProcessesIDsByCommand(admin, LIST_GSA_PROCESSES_CMD);
    }

    private static Map<String, Set<String>> getCurrentProcessesIDs(Admin admin) {
        return getProcessesIDsByCommand(admin, LIST_JAVA_PROCESSES_CMD);
    }

    private static Map<String, Set<String>> getProcessesIDsByCommand(Admin admin, String command) {

        Map<String, Set<String>> result = new HashMap<String, Set<String>>();

        if (gsaStartupPIDs == null) {
            Assert.assertTrue(admin.getGridServiceAgents().waitFor(4, 120, TimeUnit.SECONDS));
        } else {
            admin.getGridServiceAgents().waitFor(4, 10, TimeUnit.SECONDS);
        }

        for (GridServiceAgent gsa : admin.getGridServiceAgents()) {
            String host = gsa.getMachine().getHostAddress();
            if (host.startsWith(LINUX_HOST_PREFIX)) {
                result.put(host, new HashSet<String>());

                String output = SSHUtils.runCommand(host, SSH_TIMEOUT, command, USERNAME, PASSWORD);
                String[] lines = output.split("\n");

                for (String pid : lines) {
                    result.get(host).add(pid);
                }
            }
        }
        return result;
    }

    public static Set<String> getLocalProcesses() throws IOException, InterruptedException {
        Set<String> result = new HashSet<String>();

        if (!isWindows()) {
            String output = null;
            try {
                output = SSHUtils.runCommand(InetAddress.getLocalHost().getHostName(), SSH_TIMEOUT, LIST_JAVA_PROCESSES_CMD, USERNAME, PASSWORD);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            String[] lines = output.split("\n");

            for (String pid : lines) {
                result.add(pid);
            }
        } else {
            String s = CommandTestUtils.runLocalCommand("jps", true, false);
            Pattern p = Pattern.compile("\\d+ Jps");
            Matcher m = p.matcher(s);
            if(m.find()) {
                s = s.replace(m.group(), "");
            }
            
            p = Pattern.compile("\\d+");
            m = p.matcher(s);
            while (m.find()) {
            	String pid = m.group();
            	if (pid != null && pid.length() > 0) {
            		result.add(m.group());
            	}
            }
        }
        return result;
    }

    private static Map<String, Set<String>> getProcessesIDsDelta(
            Map<String, Set<String>> startProcessesIDs,
            Map<String, Set<String>> endProcessesIDs) {

        Map<String, Set<String>> result = new HashMap<String, Set<String>>();

        for (String host : endProcessesIDs.keySet()) {
            result.put(host, new HashSet<String>());
            for (String pid : endProcessesIDs.get(host)) {
                if (!startProcessesIDs.get(host).contains(pid)) {
                    result.get(host).add(pid);
                }
            }
        }

        return result;
    }

    public static Set<String> getClientProcessesIDsDelta(Set<String> startProcessesIDs, Set<String> endProcessesIDs) {
        Set<String> result = new HashSet<String>();
        for (String pid : endProcessesIDs) {
            if (!startProcessesIDs.contains(pid)) {
                result.add(pid);
            }
        }
        return result;
    }

    private static void killProcessesByIDs(Map<String, Set<String>> processesIDs) {
        for (String host : processesIDs.keySet()) {
            if (processesIDs.get(host).size() == 0) {
                continue;
            }

            log("Remaining java processes found on " + host + ". Killing remaining processes");

            String command = "kill -9 ";
            for (String pid : processesIDs.get(host)) {
                command += pid + " ";
            }
            log("running '" + command + "' on " + host);
            SSHUtils.runCommand(host, SSH_TIMEOUT, command, USERNAME, PASSWORD);
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                // ignore
            }

        }
    }

    public static void killProcessesByIDs(Set<String> processesIDs) {

    	DefaultProcessKiller dpk = new DefaultProcessKiller();
    	dpk.setKillRetries(10);
    	for (String pid : processesIDs) {
    		long processID = Long.valueOf(pid);
    		try{
    			dpk.killProcess(processID);
    		}catch (Exception e){
    			e.printStackTrace();
    		}
    	}
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }
}
