package test.cli.cloudify.xen;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.cloud.xenserver.XenServerMachineProvisioningConfig;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;
import framework.utils.xen.GsmTestUtils;


public class Stockdemo2ManagementsEsmFailOverTest extends AbstractApplicationFailOverXenTest {
	
	private final String stockdemoAppDirPath = ScriptUtils.getBuildPath() + "/examples/stockdemo";
	private int cassandraPort1;
	private int cassandraPort2;
	private String cassandraHostIp;
	private XenServerMachineProvisioningConfig xenConfigOfEsmMachine;
	private Machine esmMachine;
	
	@BeforeClass
	public void beforeClass()  {
		super.beforeTest();
		xenConfigOfEsmMachine = getMachineProvisioningConfig();
		ElasticServiceManager esm = admin.getElasticServiceManagers().waitForAtLeastOne();
		esmMachine = esm.getMachine();
		
		startAdditionalManagement();
		
		assignCassandraPorts(cassandraPort1, cassandraPort2, stockdemoAppDirPath);
		startAgent(0 ,"stockAnalytics" ,"stockAnalyticsMirror" ,"StockDemo" ,"cassandra");
	    startAgent(0 ,"stockAnalyticsProcessor" ,"stockAnalyticsSpace" , "stockAnalyticsFeeder");
	    startAgent(0 ,"stockAnalyticsProcessor" ,"stockAnalyticsSpace");
	    assertEquals("Expecting exactly 5 grid service agents to be added", 5, getNumberOfGSAsAdded());
	    assertEquals("Expecting 0 agents to be removed", 0, getNumberOfGSAsRemoved());
	    cassandraHostIp = admin.getZones().getByName("cassandra").getGridServiceAgents().getAgents()[0].getMachine().getHostAddress();
	}
	
	@Override
	@BeforeMethod
	public void beforeTest() {
		try {
			assertInstallApp(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp , stockdemoAppDirPath);
			isStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	@AfterMethod
	public void afterTest() {
		try {
			CommandTestUtils.runCommandAndWait("connect " + restUrl + " ;uninstall-application stockdemo");
		} catch (Exception e) {	}
		
		assertAppUninstalled("stockdemo");
	}
	
	@AfterClass
	public void AfterClass(){		
		super.afterTest();
	}
	/** TODO: shutting down the machine running the esm closes all others
	 * 
	 * @throws Exception
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = false)
	public void testEsmMachineShutdownFailover() throws Exception {
		LogUtils.log("Shuting down esm's mahcine gracefuly");
		GsmTestUtils.shutdownMachine(esmMachine, xenConfigOfEsmMachine, DEFAULT_TEST_TIMEOUT);
		
		LogUtils.log("asserting enviroment reconstracted");
		isStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
		assertEnvReconstracted();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = false)
	public void testEsmMachineHardShutdownFailover() throws Exception {
		LogUtils.log("Shuting down esm's mahcine");
		GsmTestUtils.hardShutdownMachine(esmMachine, xenConfigOfEsmMachine, DEFAULT_TEST_TIMEOUT);
		
		LogUtils.log("asserting enviroment reconstracted");
		isStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
		assertEnvReconstracted();
			
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////	
	
	 private void assertEnvReconstracted() throws InterruptedException {
			
			boolean esmRestarted = admin.getElasticServiceManagers().waitFor(1, DEFAULT_RECOVERY_TIME, TimeUnit.MICROSECONDS);
			boolean gsmRestarted = admin.getGridServiceManagers().waitFor(2, DEFAULT_RECOVERY_TIME, TimeUnit.MICROSECONDS);
			boolean lusRestarted = admin.getLookupServices().waitFor(2, DEFAULT_RECOVERY_TIME, TimeUnit.MICROSECONDS);
			boolean gsaRestarted = admin.getGridServiceAgents().waitFor(5, DEFAULT_RECOVERY_TIME, TimeUnit.MICROSECONDS);
			boolean restRestarted = admin.getProcessingUnits().waitFor("rest", DEFAULT_RECOVERY_TIME, TimeUnit.MICROSECONDS).waitFor(2, DEFAULT_TEST_TIMEOUT/5, TimeUnit.MICROSECONDS);
			boolean webuiRestarted = admin.getProcessingUnits().waitFor("webui", DEFAULT_RECOVERY_TIME, TimeUnit.MICROSECONDS).waitFor(2, DEFAULT_TEST_TIMEOUT/5, TimeUnit.MICROSECONDS);
			
			assertTrue("esm did not restart" , esmRestarted);
			assertTrue("gsm did not restart" , gsmRestarted);
			assertTrue("lus did not restart" , lusRestarted);
			assertTrue("gsa did not restart" , gsaRestarted);
			assertTrue("rest did not restart" , restRestarted);
			assertTrue("webui did not restart" , webuiRestarted);
		}
}
