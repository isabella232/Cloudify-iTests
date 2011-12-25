package test.cli.cloudify.xen;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.cloud.xenserver.XenServerMachineProvisioningConfig;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
		
		startAdditionalManagement();
		
		startAgent(0 ,"stockAnalytics" ,"stockAnalyticsMirror" ,"StockDemo" ,"cassandra");
	    startAgent(0 ,"stockAnalyticsProcessor" ,"stockAnalyticsSpace" , "stockAnalyticsFeeder");
	    startAgent(0 ,"stockAnalyticsProcessor" ,"stockAnalyticsSpace");
	    
	    boolean agents = admin.getGridServiceAgents().waitFor(5, DEFAULT_RECOVERY_TIME, TimeUnit.MICROSECONDS);
	    assertTrue(agents);
	    
//	    assertEquals("Expecting exactly 5 grid service agents to be added", 5, getNumberOfGSAsAdded());
	    repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
	    cassandraHostIp = admin.getZones().getByName("cassandra").getGridServiceAgents().getAgents()[0].getMachine().getHostAddress();
	    
	    try {
			installApp(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp , stockdemoAppDirPath);
			assertStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = false)
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
	
	 private void assertEnvSanity() throws InterruptedException {
		boolean esmRestarted = admin.getElasticServiceManagers().waitFor(1, DEFAULT_RECOVERY_TIME, TimeUnit.MICROSECONDS);
		boolean gsmUp = admin.getGridServiceManagers().waitFor(1, DEFAULT_RECOVERY_TIME, TimeUnit.MICROSECONDS);
		boolean lusUp = admin.getLookupServices().waitFor(1, DEFAULT_RECOVERY_TIME, TimeUnit.MICROSECONDS);
		boolean gsaUp = admin.getGridServiceAgents().waitFor(4, DEFAULT_RECOVERY_TIME, TimeUnit.MICROSECONDS);
		boolean restUp = admin.getProcessingUnits().waitFor("rest", DEFAULT_RECOVERY_TIME, TimeUnit.MICROSECONDS)
												   .waitFor(1, DEFAULT_TEST_TIMEOUT/5, TimeUnit.MICROSECONDS);
		boolean webuiUp = admin.getProcessingUnits().waitFor("webui", DEFAULT_RECOVERY_TIME, TimeUnit.MICROSECONDS)
													.waitFor(1, DEFAULT_TEST_TIMEOUT/5, TimeUnit.MICROSECONDS);
		
		assertTrue("esm did not restart" , esmRestarted);
		assertTrue("gsm did not restart" , gsmUp);
		assertTrue("lus did not restart" , lusUp);
		assertTrue("gsa did not restart" , gsaUp);
		assertTrue("rest did not restart" , restUp);
		assertTrue("webui did not restart" , webuiUp);
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
