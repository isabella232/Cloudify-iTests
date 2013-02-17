package org.cloudifysource.quality.iTests.framework.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hyperic.sigar.ProcExe;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarPermissionDeniedException;

import com.gigaspaces.internal.sigar.SigarHolder;

public class ProcessUtils {
	
	public static boolean killProcessesByBaseName(final Set<String> baseNames) throws SigarException {
		
		final Map<Long, ProcessDetails> processTable = createProcessTable();
		boolean failed = false;
		final Set<Entry<Long, ProcessDetails>> entries = processTable.entrySet();
		for (final Entry<Long, ProcessDetails> entry : entries) {
			final long pid = entry.getKey();
			final ProcessDetails procDetails = entry.getValue();
			if (baseNames.contains(procDetails.baseName)) {
				LogUtils.log("Found a leaking process: " + procDetails);
				failed = true;
				SetupUtils.killProcessesByIDs(new HashSet<String>(Arrays.asList("" + pid)));
			}
		}
		return failed;
	}
	
	public static boolean killJavaProcessesByArgs(final Set<String> processesArgs) throws SigarException {
		
		final Map<Long, ProcessDetails> processTable = createProcessTable();
		boolean failed = false;
		final Set<Entry<Long, ProcessDetails>> entries = processTable.entrySet();
		for (final Entry<Long, ProcessDetails> entry : entries) {
			final long pid = entry.getKey();
			final ProcessDetails procDetails = entry.getValue();
			if (procDetails.baseName.contains("java")) {
				final String[] args = procDetails.args;
				for (final String arg : args) {
					if (processesArgs.contains(arg)) {
						LogUtils.log("Found a leaking java process (" + arg + "): " + procDetails);
						failed = true;
						SetupUtils.killProcessesByIDs(new HashSet<String>(Arrays.asList("" + pid)));
					}
				}
			}
		}
		return failed;

	}
	
	public static Map<Long, ProcessDetails> createProcessTable()
			throws SigarException {
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

			} catch (final SigarPermissionDeniedException e) {
				// ignore
			} catch (final SigarException e) {
				// this often happens for security reasons, as procs from other
				// users will fail on this.
				LogUtils.log("Failed to read process details for pid: " + pid
						+ ". Error was: " + e.getMessage());
			}
		}

		return processDetailsByPid;
	}
	
	
	private static class ProcessDetails {

		private long pid;
		private String baseName;
		private String fullName;
		private String[] args;
		private long parentPid;

		@Override
		public String toString() {
			return "ProcessDetails [pid=" + pid + ", baseName=" + baseName
					+ ", fullName=" + fullName + ", args="
					+ Arrays.toString(args) + ", parentPid=" + parentPid + "]";
		}

	}
	

}
