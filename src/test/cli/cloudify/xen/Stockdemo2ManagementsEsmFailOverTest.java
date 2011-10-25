package test.cli.cloudify.xen;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.esm.ElasticServiceManagers;
import org.openspaces.admin.esm.events.ElasticServiceManagerRemovedEventListener;
import org.openspaces.admin.machine.Machine;
import org.openspaces.cloud.xenserver.XenServerMachineProvisioningConfig;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;
import test.gsm.GsmTestUtils;
import test.utils.LogUtils;
import test.utils.ScriptUtils;


public class Stockdemo2ManagementsEsmFailOverTest extends AbstractApplicationFailOverXenTest {
	
	protected final String stockdemoAppDirPath = ScriptUtils.getBuildPath() + "/examples/stockdemo";
	protected int cassandraPort1;
	protected int cassandraPort2;
	protected String cassandraHostIp;

	@BeforeClass
	public void beforeClass()  {
		super.beforeTest();
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
		
		XenServerMachineProvisioningConfig xenConfig = getMachineProvisioningConfig();
		Machine esmMachine = admin.getElasticServiceManagers().waitForAtLeastOne().getMachine();
		
		LogUtils.log("Shuting down esm's mahcine gracefuly");
		GsmTestUtils.shutdownMachine(esmMachine, xenConfig, DEFAULT_TEST_TIMEOUT);
		
		LogUtils.log("asserting enviroment reconstracted");
		assertEnvReconstracted();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = false)
	public void testEsmMachineHardShutdownFailover() throws Exception {
		
		XenServerMachineProvisioningConfig xenConfig = getMachineProvisioningConfig();
		Machine esmMachine = admin.getElasticServiceManagers().waitForAtLeastOne().getMachine();
		
		LogUtils.log("Shuting down esm's mahcine");
		GsmTestUtils.hardShutdownMachine(esmMachine, xenConfig, DEFAULT_TEST_TIMEOUT);
		
		LogUtils.log("asserting enviroment reconstracted");
		assertEnvReconstracted();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = false)
	public void testEsmRestart() throws Exception {
		
		ElasticServiceManager esm = admin.getElasticServiceManagers().waitForAtLeastOne();
		LogUtils.log("killing esm");
		killEsmAndWait(esm);
		
		LogUtils.log("asserting enviroment reconstracted");
		assertEnvReconstracted();
	}

	private void assertEnvReconstracted() throws InterruptedException {
		
		boolean esmRestarted = admin.getElasticServiceManagers().waitFor(1, DEFAULT_TEST_TIMEOUT/3, TimeUnit.MICROSECONDS);
		boolean gsmRestarted = admin.getGridServiceManagers().waitFor(2, DEFAULT_TEST_TIMEOUT/3, TimeUnit.MICROSECONDS);
//		boolean lusRestarted = admin.getLookupServices().waitFor(2, DEFAULT_TEST_TIMEOUT/3, TimeUnit.MICROSECONDS);
		boolean gsaRestarted = admin.getGridServiceAgents().waitFor(5, DEFAULT_TEST_TIMEOUT/3, TimeUnit.MICROSECONDS);
		boolean restRestarted = admin.getProcessingUnits().waitFor("rest", DEFAULT_TEST_TIMEOUT/3, TimeUnit.MICROSECONDS).waitFor(2, DEFAULT_TEST_TIMEOUT/5, TimeUnit.MICROSECONDS);
		boolean webuiRestarted = admin.getProcessingUnits().waitFor("webui", DEFAULT_TEST_TIMEOUT/3, TimeUnit.MICROSECONDS).waitFor(2, DEFAULT_TEST_TIMEOUT/5, TimeUnit.MICROSECONDS);
		
		assertTrue("esm did not restart" , esmRestarted);
		assertTrue("gsm did not restart" , gsmRestarted);
//		assertTrue("lus did not restart" , lusRestarted);
		assertTrue("gsa did not restart" , gsaRestarted);
		assertTrue("rest did not restart" , restRestarted);
		assertTrue("webui did not restart" , webuiRestarted);
		
		isStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
	}
	
	private void killEsmAndWait(final ElasticServiceManager esm){
		if (esm.isDiscovered()) {
            final CountDownLatch latch = new CountDownLatch(1);
            
            ElasticServiceManagerRemovedEventListener removedEventListener = new ElasticServiceManagerRemovedEventListener() {

                public void elasticServiceManagerRemoved(
                        ElasticServiceManager elasticServiceManager) {
                    if (elasticServiceManager.equals(esm)) {
                        latch.countDown();
                    }
                }
            };
            ElasticServiceManagers managers = esm.getAdmin().getElasticServiceManagers();
            managers.getElasticServiceManagerRemoved().add(removedEventListener);
            try {
                esm.kill();
                latch.await();
            } catch (InterruptedException e) {
                Assert.fail("Interrupted while killing esm", e);
            } finally {
                managers.getElasticServiceManagerRemoved().remove(removedEventListener);
            }
        }
	}
}
