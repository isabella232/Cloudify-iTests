package test.cli.cloudify.xen;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.cloud.xenserver.XenServerMachineProvisioningConfig;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.cloudify.dsl.internal.CloudifyConstants;

import framework.tools.SGTestHelper;
import framework.utils.LogUtils;
import framework.utils.xen.GsmTestUtils;


public class Stockdemo2ManagementsEsmFailOverTest extends AbstractApplicationFailOverXenTest {
	
	private final String stockdemoAppDirPath = SGTestHelper.getSGTestRootDir().replace("\\", "/") + "/apps/USM/usm/applications/stockdemo";
	private XenServerMachineProvisioningConfig xenConfigOfEsmMachine;
	private Machine esmMachine;
	
	@BeforeMethod
	public void beforeTest() {
	    
	    setTwoManagementMachines();
		super.beforeTest();
		
		assignCassandraPorts(stockdemoAppDirPath);
		
		xenConfigOfEsmMachine = getMachineProvisioningConfig();
		ElasticServiceManager esm = admin.getElasticServiceManagers().waitForAtLeastOne();
		esmMachine = esm.getMachine();
		
		GridServiceContainer restGsc = restGscWorkaround();
		
		startAdditionalManagement();

		// Here we rely on the ESM to restart to rest service
		restGsc.kill();
		
        assertManagementStarted();
		
		startAgent(0 ,"stockAnalytics" ,"stockAnalyticsMirror" ,"StockDemo" ,"cassandra");
	    startAgent(0 ,"stockAnalyticsProcessor" ,"stockAnalyticsSpace" , "stockAnalyticsFeeder");
	    startAgent(0 ,"stockAnalyticsProcessor" ,"stockAnalyticsSpace");
	    
	    boolean agents = admin.getGridServiceAgents().waitFor(5, DEFAULT_RECOVERY_TIME, TimeUnit.MICROSECONDS);
	    assertTrue(agents);
	    
	    repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
	    cassandraHostIp = admin.getZones().getByName("cassandra").getGridServiceAgents().getAgents()[0].getMachine().getHostAddress();
	    
	    try {
			installApp(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp , stockdemoAppDirPath);
			assertStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
		} catch (Exception e) {
		    AssertFail("Failed installing stockdemo application", e);
		}
	}

    // this is a hack. we need to have a handle to the GSC currently holding the rest instance so we could restart it after the second management machine starts
    // Problem: the rest depends on the management space. in this specific case the space is defined as 'highlyAvailable' meaning it would not be instantiated until 
    // after the second xen machine is started and a backup space is deployed on it. 
    // Workaround: We restart the rest GSC after the second management space is deployed on the second machine.
    // TODO: change start management implementation so that rest deployment depends on management space
    private GridServiceContainer restGscWorkaround() {

		GridServiceContainer restGsc = null;
		admin.getGridServiceContainers().waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		for (GridServiceContainer container : admin.getGridServiceContainers()) {
			if (container.getZones().containsKey("rest")) {
				restGsc = container;
			}
		}
		
		if (restGsc == null) {
			AssertFail("Failed finding rest GSC");
		}
        return restGsc;
    }
	
	
	@Override
	@AfterMethod
	public void afterTest() {
		super.afterTest();
	}
	
	/** 
	 * TODO: shutting down the machine running the esm closes all others
	 * 
	 * @throws Exception
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testEsmMachineShutdownFailover() throws Exception {
		LogUtils.log("Shuting down esm's mahcine gracefuly");
		GsmTestUtils.shutdownMachine(esmMachine, xenConfigOfEsmMachine, DEFAULT_TEST_TIMEOUT);
		
		assertDesiredLogic();
	}
	
	/**
	 * GS-8637
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testEsmMachineHardShutdownFailover() throws Exception {
		LogUtils.log("Shuting down esm's mahcine");
		GsmTestUtils.hardShutdownMachine(esmMachine, xenConfigOfEsmMachine, DEFAULT_TEST_TIMEOUT);
		
		assertDesiredLogic();
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////	
	
	 private void assertEnvSanity() {
		boolean esmRestarted = admin.getElasticServiceManagers().waitFor(1, DEFAULT_RECOVERY_TIME, TimeUnit.MILLISECONDS);
		boolean gsmUp = admin.getGridServiceManagers().waitFor(1, DEFAULT_RECOVERY_TIME, TimeUnit.MILLISECONDS);
		boolean lusUp = admin.getLookupServices().waitFor(1, DEFAULT_RECOVERY_TIME, TimeUnit.MILLISECONDS);
		boolean gsaUp = admin.getGridServiceAgents().waitFor(4, DEFAULT_RECOVERY_TIME, TimeUnit.MILLISECONDS);
		boolean restUp = admin.getProcessingUnits().waitFor("rest", DEFAULT_RECOVERY_TIME, TimeUnit.MILLISECONDS)
												   .waitFor(1, DEFAULT_TEST_TIMEOUT/5, TimeUnit.MILLISECONDS);
		boolean webuiUp = admin.getProcessingUnits().waitFor("webui", DEFAULT_RECOVERY_TIME, TimeUnit.MILLISECONDS)
													.waitFor(1, DEFAULT_TEST_TIMEOUT/5, TimeUnit.MILLISECONDS);
		boolean managementSpaceUp = admin.getProcessingUnits().waitFor(CloudifyConstants.MANAGEMENT_SPACE_NAME, DEFAULT_RECOVERY_TIME, TimeUnit.MILLISECONDS)
													.waitFor(1, DEFAULT_TEST_TIMEOUT/5, TimeUnit.MILLISECONDS);
		
		assertTrue("esm did not restart" , esmRestarted);
		assertTrue("gsm did not restart" , gsmUp);
		assertTrue("lus did not restart" , lusUp);
		assertTrue("gsa did not restart" , gsaUp);
		assertTrue("rest did not restart" , restUp);
		assertTrue("webui did not restart" , webuiUp);
		assertTrue("management space could not be found", managementSpaceUp);
	}
	 
	private void assertDesiredLogic() throws InterruptedException {
		LogUtils.log("asserting enviroment reconstracted");
		assertEnvSanity();
		
		LogUtils.log("asserting stockdemo application is still installed");
		assertStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
		
		ElasticServiceManager esm = admin.getElasticServiceManagers().waitForAtLeastOne();
		LogUtils.log("asserting esm is managing the application");
		assertEsmIsManagingEnvBySearchingLogs(esm);
	}
}
