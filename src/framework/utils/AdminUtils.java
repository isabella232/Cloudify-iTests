package framework.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.gsa.ElasticServiceManagerOptions;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsa.LookupServiceOptions;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.internal.InternalAdminFactory;
import org.openspaces.admin.lus.LookupService;
import org.openspaces.admin.machine.Machine;

import com.gigaspaces.log.LogEntryMatchers;
import com.gigaspaces.security.directory.UserDetails;


/**
 * Utility methods on top of the Admin API
 * 
 * @author Moran Avigdor
 */
public class AdminUtils {
	
	private static final int TIMEOUT = 15 * 60; //default - 15 minutes
	
	/**
	 * @return lookup group environment variable value or null if undefined.
	 */
	private static String getGroupsEnvironmentVariable() {
		String groupsProperty = System.getProperty("com.gs.jini_lus.groups");
        if (groupsProperty == null) {
            groupsProperty = System.getenv("LOOKUPGROUPS");
        }
        return groupsProperty;
	}
	
	public static String getTestGroups()  {
		String groups = getGroupsEnvironmentVariable();
		if (groups == null) {
			try {
				groups = "sgtest-"+InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				AssertUtils.AssertFail("Failed generating unique group name",e);
			}
		}
		return groups;
	}
	
	private static Admin createAdmin(AdminFactory adminFactory) {
		if (getGroupsEnvironmentVariable() == null) {
			adminFactory.addGroups(getTestGroups());
		}
		return adminFactory.createAdmin();
	}
	
	public static Admin createAdmin() {
		Admin admin = createAdmin(new AdminFactory());
		admin.setDefaultTimeout(TIMEOUT, TimeUnit.SECONDS); //default - 15 minutes
		return admin;
	}

	public static Admin createSingleThreadAdmin() {
		Admin admin = createAdmin(new InternalAdminFactory().singleThreadedEventListeners());
		admin.setDefaultTimeout(TIMEOUT, TimeUnit.SECONDS); //default - 15 minutes
		return admin;
	}
	
    public static Admin createAdminWithLocators(Admin admin) {
        AdminFactory factory = new AdminFactory();
        for (Machine machine : admin.getMachines().getMachines()) {
            factory.addLocator(machine.getHostAddress());
        }
        Admin _admin = createAdmin(factory);
        _admin.setDefaultTimeout(TIMEOUT, TimeUnit.SECONDS); //default - 15 minutes
        return _admin;
    }
    
    public static Admin createSecuredAdmin(String userName,String password) {
        AdminFactory factory = new AdminFactory().userDetails(userName, password).discoverUnmanagedSpaces();
        Admin _admin = createAdmin(factory);
        _admin.setDefaultTimeout(TIMEOUT, TimeUnit.SECONDS); //default - 15 minutes
        return _admin;
    }
    
    public static Admin createSecuredAdmin(UserDetails userDetails) {
        AdminFactory factory = new AdminFactory().userDetails(userDetails).discoverUnmanagedSpaces();
        Admin _admin = createAdmin(factory);
        _admin.setDefaultTimeout(TIMEOUT, TimeUnit.SECONDS); //default - 15 minutes
        return _admin;
    }

    public static LookupService loadLUS(GridServiceAgent gsa) {
    	return loadLUS(gsa, null);
    }
    
    /** loads 1 LUS via this GSA, tagged with the list of comma separated zones */
	public static LookupService loadLUS(GridServiceAgent gsa, String zones) {
		LookupServiceOptions options = new LookupServiceOptions();
		if (zones == null) {
			options.useScript();
		}
		if (zones!=null) {
			options.vmInputArgument("-Dcom.gs.zones="+zones);
		}
		return gsa.startGridServiceAndWait(options);
	}
	
	/** loads 1 GSC on this machine */
	public static GridServiceContainer loadGSC(Machine machine) {
		return loadGSC(machine.getGridServiceAgents().waitForAtLeastOne());
	}
	
	/** loads 1 GSC via this GSA */
	public static GridServiceContainer loadGSC(GridServiceAgent gsa) {
		return loadGSC(gsa, null);
	}
		
	
	/** loads 1 GSC via this GSA, tagged with the list of comma separated zones */
	public static GridServiceContainer loadGSC(GridServiceAgent gsa, String zones) {
		GridServiceContainerOptions options = new GridServiceContainerOptions();
		if (zones == null) {
			options.useScript();
		}
		if (zones!=null) {
			options.vmInputArgument("-Dcom.gs.zones="+zones);
		}
		return gsa.startGridServiceAndWait(options);
	}
	
	/** loads 1 secured GSC via this GSA, */
	public static GridServiceContainer loadGSC(GridServiceAgent gsa, boolean secured) {
		GridServiceContainerOptions options = new GridServiceContainerOptions();
		if (secured) {
			options.vmInputArgument("-Dcom.gs.security.enabled="+secured);
		}
		return gsa.startGridServiceAndWait(options);
	}
	
	/** loads n GSCs simultaneously */
	public static GridServiceContainer[] loadGSCs(Machine machine, int nGSCs) {
		return loadGSCs(machine.getGridServiceAgents().waitForAtLeastOne(), nGSCs);
	}
	
	/** loads n GSCs simultaneously */
	public static GridServiceContainer[] loadGSCs(Machine machine, int nGSCs, String zones) {
		return loadGSCs(machine.getGridServiceAgents().waitForAtLeastOne(), nGSCs, zones);
	}
	
	/** loads 1 GSC via this GSA, tagged with the list of comma separated zones */
	public static GridServiceContainer loadGSC(Machine machine, String zones) {
		GridServiceAgent gsa = machine.getGridServiceAgents().waitForAtLeastOne();
		return gsa.startGridServiceAndWait(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.zones="+zones));
	}

    /** loads 1 GSC via this GSA, tagged with the list of comma separated system properties */
	public static GridServiceContainer loadGSCWithSystemProperty(Machine machine, String ... systemProperties) {
		GridServiceAgent gsa = machine.getGridServiceAgents().waitForAtLeastOne();
        return loadGSCWithSystemProperty(gsa, systemProperties);
	}
	
	/** loads 1 GSC via this GSA, tagged with the list of comma separated system properties */
	public static GridServiceContainer loadGSCWithSystemProperty(GridServiceAgent gsa, String ... systemProperties) {
        GridServiceContainerOptions options = new GridServiceContainerOptions();
        for(String prop : systemProperties){
            options = options.vmInputArgument(prop);    
        }
        options.overrideVmInputArguments();
		return gsa.startGridServiceAndWait(options);
	}

	
	/** loads 1 GSM on this machine */
	public static GridServiceManager loadGSM(Machine machine) {
		return loadGSM(machine.getGridServiceAgents().waitForAtLeastOne());
	}
	
	/** loads 1 GSM via this GSA */
	public static GridServiceManager loadGSM(GridServiceAgent gsa) {
		return gsa.startGridServiceAndWait(new GridServiceManagerOptions().useScript());
    }

	/** loads 1 GSM via this GSA */
	public static GridServiceManager loadGSMWithDebugger(GridServiceAgent gsa, int debugPort, boolean suspend) {
		return gsa.startGridServiceAndWait(new GridServiceManagerOptions().environmentVariable("GSM_JAVA_OPTIONS", "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,address="+debugPort+",suspend="+(suspend?"y":"n")).useScript());
    }
	
	/** loads 1 secured GSM via this GSA, */
    public static GridServiceManager loadGSM(GridServiceAgent gsa, boolean secured) {
        GridServiceManagerOptions options = new GridServiceManagerOptions();
        if (secured) {
            options.vmInputArgument("-Dcom.gs.security.enabled="+secured);
        }
        return gsa.startGridServiceAndWait(options);
    }
	
    public static GridServiceManager loadGSMWithSystemProperty(Machine machine, String ... systemProperties) {
        GridServiceAgent gsa = machine.getGridServiceAgents().waitForAtLeastOne();
        GridServiceManagerOptions options = new GridServiceManagerOptions();
        for(String prop : systemProperties){
            options = options.vmInputArgument(prop);    
        }
        options.overrideVmInputArguments();
        return gsa.startGridServiceAndWait(options);
    }
    
	/** loads n GSCs simultaneously and returns them */
	public static GridServiceContainer[] loadGSCs(final GridServiceAgent gsa, int nGSCs) {
		return loadGSCs(gsa, nGSCs, null);
	}
	
	/** loads n GSCs simultaneously and returns them */
	public static GridServiceContainer[] loadGSCs(final GridServiceAgent gsa, int nGSCs, final String zones) {
		ExecutorService threadPool = Executors.newFixedThreadPool(nGSCs);
		Future<GridServiceContainer>[] futures = new Future[nGSCs];
		for (int i=0; i<nGSCs; ++i) {
			futures[i] = threadPool.submit(new Callable<GridServiceContainer>() {
				public GridServiceContainer call() throws Exception {
					return loadGSC(gsa, zones);
				}
			});
		}
		
		GridServiceContainer[] gscs = new GridServiceContainer[nGSCs]; 
		for (int i=0; i<nGSCs; ++i) {
			try {
				gscs[i] = futures[i].get();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return gscs;
	}
	
	/** loads n GSMs simultaneously and returns them */
	public static GridServiceManager[] loadGSMs(final Machine m, int nGSMs) {
		ExecutorService threadPool = Executors.newFixedThreadPool(nGSMs);
		Future<GridServiceManager>[] futures = new Future[nGSMs];
		for (int i=0; i<nGSMs; ++i) {
			futures[i] = threadPool.submit(new Callable<GridServiceManager>() {
				public GridServiceManager call() throws Exception {
					return loadGSM(m);
				}
			});
		}
		
		GridServiceManager[] gsms = new GridServiceManager[nGSMs]; 
		for (int i=0; i<nGSMs; ++i) {
			try {
				gsms[i] = futures[i].get();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return gsms;
	}
	
	/** load nGSMs and nGSCs simultaneously, with no return value  */
	public static void loadGSMsGSCs(final Machine machine, int nGSMs, int nGSCs) {
		ExecutorService threadPool = Executors.newFixedThreadPool(nGSMs + nGSCs);
		
		for (int i=0; i<nGSMs; ++i) {
			threadPool.submit(new Runnable() {
				public void run() {
					loadGSM(machine);
				}
			});
		}
		
		for (int i=0; i<nGSCs; ++i) {
			threadPool.submit(new Runnable() {
				public void run() {
					loadGSC(machine);
				}
			});
		}
		
		// Initiates an orderly shutdown in which previously submitted tasks are
		// executed, but no new tasks will be accepted.
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	/** Waits for the GSM to print out:  "Registered GSC .... count [2]" */
	public static boolean waitForRegisteredGSCs(GridServiceManager gsm, int nGSCs) {
		long interval = 500;
		long timeout = 60000;
		while (gsm.logEntries(LogEntryMatchers.regex(".*(Registered GSC).*(count \\["+nGSCs+"\\]).*")).getEntries().size() < nGSCs) {
			try {
				Thread.sleep(interval);
				timeout -= interval;
				if (timeout <= 0) return false;
			} catch (InterruptedException e) {
			}
		}
		return true;
	}
	
	public static ElasticServiceManager loadESM(GridServiceAgent gsa) {
		return gsa.startGridServiceAndWait(new ElasticServiceManagerOptions());
	}
	
	public static ElasticServiceManager loadESMWithDebugger(GridServiceAgent gsa, int debugPort, boolean suspend) {
		return gsa.startGridServiceAndWait(
				new ElasticServiceManagerOptions()
				.vmInputArgument("-Xdebug")
				.vmInputArgument("-Xnoagent")
				.vmInputArgument("-Djava.compiler=NONE")
				.vmInputArgument("-Xrunjdwp:transport=dt_socket,server=y,address="+debugPort+",suspend="+(suspend?"y":"n")));
    }
}
