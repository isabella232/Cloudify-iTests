package test.gateway;

import static test.utils.LogUtils.log;

import java.util.HashMap;
import java.util.Map;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.testng.annotations.AfterMethod;

import test.utils.SetupUtils;
import test.utils.TeardownUtils;

import com.gatewayPUs.common.MessageGW;
import com.gigaspaces.query.IdQuery;


public abstract class UpdatePerformanceSingleSourceAbstractTest extends AbstractGatewayTest {
	
	protected final int OBJECTS = 1000;
	protected final static int THREADS = 5;
	protected final int UPDATES_PER_THREAD = 50000;
	protected final int WARM_UP_CONSTANT = 200000;

	private Admin sourceAdmin;
    private Admin targetAdmin1;
    private Admin targetAdmin2;
    private GigaSpace sourceGigaSpace;
    private GridServiceManager targetGsm1;
	private GridServiceManager targetGsm2;
	private GridServiceManager sourceGsm;
	private int initializationEndedMsgId = WARM_UP_CONSTANT + 1;
	private int timeLogMsgId = WARM_UP_CONSTANT;
	
    private String group1 = null;
    private String group2 = null;
    private String group3 = null;
    
    private String host1 = null;
    private String host2 = null;
    private String host3 = null;
	
	public UpdatePerformanceSingleSourceAbstractTest(){
        if (isDevMode()) {
            group1 = "israel-" + getUserName();
            group2 = "london-" + getUserName();
            group3 = "ny-" + getUserName();
           
     } else {
            group1 = GROUP1;
            group2 = GROUP2;
            group3 = GROUP3;
            host1 = HOST1;
            host2 = HOST2;
            host3 = HOST3;
        }
    }
    @AfterMethod
    public void tearDown() {	
        TeardownUtils.teardownAll(sourceAdmin, targetAdmin1, targetAdmin2);
        sourceAdmin.close();
        targetAdmin1.close();
        targetAdmin2.close();
    }
    
	public void runUpdatePerformanceTest(int targets ) throws Exception, InterruptedException {
		initialize(targets);
		log("indicating to the feeder that initialization is done");
		sourceGigaSpace.write(new MessageGW(initializationEndedMsgId));		
		log("waiting for the timeLogMsg written by the feeder indicating the end of it's work");
		MessageGW timeLogMsg = sourceGigaSpace.readById(new IdQuery<MessageGW>(MessageGW.class , timeLogMsgId) , DEFAULT_TEST_TIMEOUT);
		assertGatewayReplicationHasNotingToReplicateSiteIsNotEmpty(sourceAdmin);
		log(timeLogMsg.getInfo());
	}
	
	private void initialize(int targets) throws Exception {
        log("initializing with " + targets + " targets..");
        sourceAdmin = new AdminFactory().addGroups(group1).createAdmin();
        targetAdmin1 = new AdminFactory().addGroups(group2).createAdmin();
        targetAdmin2 = new AdminFactory().addGroups(group3).createAdmin();
        log("cleaning enviroment");
        SetupUtils.assertCleanSetup(sourceAdmin);
        SetupUtils.assertCleanSetup(targetAdmin1);
        SetupUtils.assertCleanSetup(targetAdmin2);
               
        log("setting up enviroment");
        // host discovery
        host1 = (String)sourceAdmin.getMachines().getHostsByAddress().keySet().toArray()[0];
        host2 = (String)targetAdmin1.getMachines().getHostsByAddress().keySet().toArray()[0];
        host3 = (String)targetAdmin2.getMachines().getHostsByAddress().keySet().toArray()[0];
        
        GridServiceAgent sourceGsa = sourceAdmin.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent targetGsa1 = targetAdmin1.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent targetGsa2 = targetAdmin2.getGridServiceAgents().waitForAtLeastOne();
        
        sourceGsa.startGridServiceAndWait(new GridServiceManagerOptions());
        sourceGsa.startGridServiceAndWait(new GridServiceContainerOptions());
    
        targetGsa1.startGridServiceAndWait(new GridServiceManagerOptions());
        targetGsa1.startGridServiceAndWait(new GridServiceContainerOptions());
        
        targetGsa2.startGridServiceAndWait(new GridServiceManagerOptions());
        targetGsa2.startGridServiceAndWait(new GridServiceContainerOptions());
        
        sourceGsm  = sourceAdmin.getGridServiceManagers().waitForAtLeastOne();
        targetGsm1 = targetAdmin1.getGridServiceManagers().waitForAtLeastOne();
        targetGsm2 = targetAdmin2.getGridServiceManagers().waitForAtLeastOne();
		
        log("deploying source site");
        deploySourceSiteWithEmbeddedFeeder(targets);
        log("deploying target sites");
        deployTargetSites(targets);
        log("deploying source gateway");
        deploySourceDelegator(targets);
        log("deploying target gateways");
        deployTargetSinks(targets);
               
        Space sourceSpace = sourceAdmin.getSpaces().waitFor("sourceSpace");
        sourceGigaSpace = sourceSpace.getGigaSpace();
        
        log("validating gateway components");
        // Verify delegators & sinks are connected.
        assertGatewayReplicationConnected(sourceSpace, targets);
               
        log("finished initialziation");
    }
	
	private void deploySourceSiteWithEmbeddedFeeder(int targets) {
	String embeddedFeederClassToUse = null;
	switch(targets){
		case  1: {embeddedFeederClassToUse = "embeddedFeeder1Target"; break;}
		case 10: {embeddedFeederClassToUse = "embeddedFeeder10Targets"; break;}
		case 20: {embeddedFeederClassToUse = "embeddedFeeder20Targets"; break;}
		case 30: {embeddedFeederClassToUse = "embeddedFeeder30Targets"; break;}
	}
	Map<String, String> props = new HashMap<String, String>();
	props.put("localGatewayName", "SOURCE");
	for(int i=0 ; i < targets ; i++)
		if(i < targets/2)
			if (i==0)
				props.put("gatewayTarget" + i, "TARGET-WITH-LUS-1");
			else
				props.put("gatewayTarget" + i, "TARGET-" + i);
		else
			if(i==targets/2)
				props.put("gatewayTarget" + i, "TARGET-WITH-LUS-2");
			else
				props.put("gatewayTarget" + i, "TARGET-" + i);
	
	props.put("spaceUrl", "/./sourceSpace");
	props.put("updatesPerThread", "" + UPDATES_PER_THREAD);
	props.put("warmUpConstant", "" + WARM_UP_CONSTANT);
	props.put("numberOfThreads", "" + THREADS);		
	props.put("timeLogMsgId", "" + timeLogMsgId);
	props.put("initializationEndedMsgId", "" + initializationEndedMsgId);
	props.put("DEFAULT_TEST_TIMEOUT", "" + DEFAULT_TEST_TIMEOUT);		
			
	deploySite(sourceGsm, sitePrepareAndDeployment(embeddedFeederClassToUse,props));
	sourceAdmin.getSpaces().waitFor("sourceSpace");
	}
	
	private void deploySourceDelegator(int targets) {
	String gatewayDelegatorPuToUse = null;
	switch(targets){
		case  1: {gatewayDelegatorPuToUse = "gatewayDelegator1Target"; break;}
		case 10: {gatewayDelegatorPuToUse = "gatewayDelegator10Targets"; break;}
		case 20: {gatewayDelegatorPuToUse = "gatewayDelegator20Targets"; break;}
		case 30: {gatewayDelegatorPuToUse = "gatewayDelegator30Targets"; break;}
	}
	Map<String, String> props = new HashMap<String, String>();
	props.put("localGatewayName", "SOURCE");	
	props.put("localClusterUrl", "jini://*/*/sourceSpace?groups=" + group1);
	props.put("localGatewayHost", host1);
	props.put("localGatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);
	for(int i=0 ; i < targets ; i++){
		if(i < targets/2){
			if(i==0)
				props.put("targetGatewayName" + i , "TARGET-WITH-LUS-1");
			else
				props.put("targetGatewayName" + i , "TARGET-" + i);
			props.put("targetGatewayHost" + i, host2);
	    	props.put("targetGatewayDiscoveryPort" + i, LONDON_GATEWAY_DISCOVERY_PORT);
		}
		else{
			if(i==targets/2)
				props.put("targetGatewayName" + i , "TARGET-WITH-LUS-2");
			else
				props.put("targetGatewayName" + i , "TARGET-" + i);
			props.put("targetGatewayHost" + i, host3);
	    	props.put("targetGatewayDiscoveryPort" + i, NY_GATEWAY_DISCOVERY_PORT);
		}	           
	}
	deployGateway(sourceGsm, siteDeployment("./apps/gateway/" + gatewayDelegatorPuToUse, "SOURCE-GW", props));
	sourceAdmin.getProcessingUnits().waitFor("SOURCE-GW");
	}
	
	private void deployTargetSites(int targets) {
	Map<String, String> props = new HashMap<String, String>();
	               
	props.put("spaceUrl", "/./targetSpace1");
	deploySite(targetGsm1, siteDeployment("./apps/gateway/clusterWithoutTargets", "targetSpace1", props).partitioned(1, 0));
	targetAdmin1.getSpaces().waitFor("targetSpace1");
	
	props.put("spaceUrl", "/./targetSpace2");
	deploySite(targetGsm2, siteDeployment("./apps/gateway/clusterWithoutTargets", "targetSpace2", props).partitioned(1, 0));
	targetAdmin2.getSpaces().waitFor("targetSpace2");
	}
	
	private void deployTargetSinks(int targets) {
	Map<String, String> props = new HashMap<String, String>();
	for(int i=0 ; i < targets ; i++){		
		props.put("targetGatewayName", "SOURCE");
		if(i < targets/2){
			props.put("localGatewayHost" , host2);
			props.put("localGatewayDiscoveryPort" , LONDON_GATEWAY_DISCOVERY_PORT);
			props.put("localClusterUrl", "jini://*/*/targetSpace1" + "?groups=" + group2);
			if (i==0){
				// deploy gw with lus on host2
				props.put("localGatewayName", "TARGET-WITH-LUS-1" );
	            deployGateway(targetGsm1, siteDeployment("./apps/gateway/gatewaySinkWithLus", "TARGET-WITH-LUS-1", props));
	            targetAdmin1.getProcessingUnits().waitFor("TARGET-WITH-LUS-1");
			}
			else{
				props.put("localGatewayName", "TARGET-" + i);
				deployGateway(targetGsm1, siteDeployment("./apps/gateway/gatewaySinkWithoutLus", "TARGET-GW" + i, props));
				targetAdmin1.getProcessingUnits().waitFor("TARGET-GW" + i);
			}
		}
		else{
			props.put("localGatewayHost" , host3);
			props.put("localGatewayDiscoveryPort" , NY_GATEWAY_DISCOVERY_PORT);
			props.put("localClusterUrl", "jini://*/*/targetSpace2" + "?groups=" + group3);
			if(i == targets/2){
	    		// deploy gw with lus on host3
				props.put("localGatewayName", "TARGET-WITH-LUS-2" );
	            deployGateway(targetGsm2, siteDeployment("./apps/gateway/gatewaySinkWithLus", "TARGET-WITH-LUS-2", props));
	            targetAdmin2.getProcessingUnits().waitFor("TARGET-WITH-LUS-2");
			}
			else{ 
				props.put("localGatewayName", "TARGET-" + i);
				deployGateway(targetGsm2, siteDeployment("./apps/gateway/gatewaySinkWithoutLus", "TARGET-GW" + i, props));
				targetAdmin2.getProcessingUnits().waitFor("TARGET-GW" + i);
			}
	    }
		props.clear();
	}
	}
	
	}
