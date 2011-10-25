package test.cli.cloudify.xen;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.internal.gsa.InternalGridServiceAgent;
import org.openspaces.admin.lus.LookupService;
import org.openspaces.cloud.xenserver.XenServerMachineProvisioningConfig;
import org.testng.annotations.BeforeMethod;

import test.gsm.AbstractXenGSMTest;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.LogUtils;
import test.utils.SSHUtils;

public class AbstractStartManagementXenTest extends AbstractXenGSMTest {
	
    private static final String SHUTDOWN_MANAGEMENT_COMMAND = "/opt/gigaspaces/tools/cli/cloudify.sh shutdown-management --verbose";
	private static final String SHUTDOWN_AGENT_COMMAND = "/opt/gigaspaces/tools/cli/cloudify.sh shutdown-agent --verbose";
    
	@Override
	protected void overrideXenServerProperties(XenServerMachineProvisioningConfig machineProvisioningConfig) {
        super.overrideXenServerProperties(machineProvisioningConfig);
        machineProvisioningConfig.setFileAgentRemoteLocation("/opt/cloudify-start-management.sh");
    }
	
	@Override
	@BeforeMethod
	public void beforeTest() {
	    super.setAcceptGSCsOnStartup(true);
	    super.beforeTest(); 
	    try {
			assertManagementStarted();
		} catch (URISyntaxException e) {		
			e.printStackTrace();
		}
	    assertEquals("Expecting exactly 1 agent to be added", 1, getNumberOfGSAsAdded());
	    assertEquals("Expecting 0 agents to be removed", 0, getNumberOfGSAsRemoved());
	}

	protected void assertManagementStarted() throws URISyntaxException {
		// test management rest and webui services
		for (GridServiceManager gsm : admin.getGridServiceManagers()) {
	    	String host = gsm.getMachine().getHostAddress();
	    
		    final URI restAdminURI = new URI("http", null, host, 8100, null, null, null);
		    final URI webUIURI = new URI("http", null, host, 8099, null, null, null);
		    
		    repetitiveAssertTrue("Failed waiting for REST/WebIU services", new RepetitiveConditionProvider() {
	            public boolean getCondition() {
	                return isURIAvailable(restAdminURI) &&
	                       isURIAvailable(webUIURI);
	            }
	        }, OPERATION_TIMEOUT);
	    }
	}
	
	protected void startAgent(int extraMemoryCapacityInMB ,String... zones) {
		
		final XenServerMachineProvisioningConfig agentConfig = getMachineProvisioningConfig();
	    agentConfig.setFileAgentRemoteLocation("/opt/cloudify-start-agent.sh");
	    agentConfig.setGridServiceAgentZones(zones);
	    int memoryCapacityInMB = agentConfig.getMemoryCapacityPerMachineInMB() + extraMemoryCapacityInMB;
	    super.startNewVM(0,memoryCapacityInMB, agentConfig,OPERATION_TIMEOUT, TimeUnit.MILLISECONDS); 
	}
	
	protected void startAdditionalManagement() {
		
		final XenServerMachineProvisioningConfig agentConfig = getMachineProvisioningConfig();
	    agentConfig.setFileAgentRemoteLocation("/opt/cloudify-start-additional-management.sh");	    
	    super.startNewVM(0,0, agentConfig,OPERATION_TIMEOUT, TimeUnit.MILLISECONDS); 
	}
	
	@Override
	public void teardownAllVMs() {
	
		shutdownAgents();
		shutdownManagements();
		
	    super.teardownAllVMs();
	}
	
	private void shutdownManagements() {
	    final List<GridServiceAgent> agentsShutdown = new ArrayList<GridServiceAgent>(); 
		if (admin != null) {
		    for (GridServiceAgent agent : admin.getGridServiceAgents()) {
		    	if (isManagementMachine(agent)) {
		    		agentsShutdown.add(agent);
		    		runCommand(agent, SHUTDOWN_MANAGEMENT_COMMAND);
		    	}
		    }
		}
		
	    waitForAgentsShutdown(agentsShutdown);
		
	}

	private void shutdownAgents() {
		final List<GridServiceAgent> agentsShutdown = new ArrayList<GridServiceAgent>(); 
		if (admin != null) {
			for (GridServiceAgent agent : admin.getGridServiceAgents()) {
		    	if (!isManagementMachine(agent)) {
		    		agentsShutdown.add(agent);
		    		runCommand(agent, SHUTDOWN_AGENT_COMMAND);
		    	}
		    }
		}
		
	    waitForAgentsShutdown(agentsShutdown);
	}
	
	private void waitForAgentsShutdown(final List<GridServiceAgent> agentsShutdown) {
		repetitiveAssertTrue("Failed waiting for all agents to be removed", new RepetitiveConditionProvider() {
            public boolean getCondition() {
                int agentsAlive = 0;
                for (final GridServiceAgent agent : agentsShutdown) {
                	if (isManagementMachine(agent)) {
	                	try {
	    					((InternalGridServiceAgent)agent).getGSA().ping();
	    					agentsAlive++;
	    					LogUtils.log("Agent on " + agent.getMachine().getHostAddress() + " still alive");
	    				} catch (final RemoteException e) {
	    					//Probably NoSuchObjectException meaning the GSA is going down
	    				}	
                	}
                }
            	return agentsAlive == 0;
            }
        }, OPERATION_TIMEOUT);
	}


	public boolean isManagementMachine(GridServiceAgent agent) {
		boolean isManagementMachine = false;
		for (LookupService lus : admin.getLookupServices()) {
			if (agent.equals(lus.getGridServiceAgent())) {
				isManagementMachine = true;
				break;
			}
		}
		return isManagementMachine;
	}

	public boolean isURIAvailable(URI uri) {
	    HttpClient client = new DefaultHttpClient();
	    HttpGet httpGet = new HttpGet(uri);
	    try {
	        client.execute(httpGet, new BasicResponseHandler());
	        return true;
	    } catch (Exception e) {
	        return false;
	    } finally {
	        client.getConnectionManager().shutdown();
	    }
	}
	
	void runCommand(GridServiceAgent agent, String command) {
	    String username = getMachineProvisioningConfig().getSshUsername();
	    String password = getMachineProvisioningConfig().getSshPassword();
	    String host = agent.getMachine().getHostAddress();		
		SSHUtils.runCommand(host, 1000 * 60, SHUTDOWN_MANAGEMENT_COMMAND, username, password);
	}
}


