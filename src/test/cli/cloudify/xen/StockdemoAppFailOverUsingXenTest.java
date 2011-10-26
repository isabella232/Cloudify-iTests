package test.cli.cloudify.xen;

import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class StockdemoAppFailOverUsingXenTest extends AbstractApplicationFailOverXenTest {

	protected final String stockdemoAppDirPath = ScriptUtils.getBuildPath() + "/examples/stockdemo";
	protected int cassandraPort1;
	protected int cassandraPort2;
	protected String cassandraHostIp;
	
	@BeforeClass
	public void beforeClass()  {
		super.beforeTest();
		assignCassandraPorts(cassandraPort1 , cassandraPort2 , stockdemoAppDirPath);
																			   
		startAgent(0 ,"stockAnalytics" ,"stockAnalyticsMirror" ,"StockDemo"  ,"cassandra");
	    startAgent(0 ,"stockAnalyticsProcessor" ,"stockAnalyticsSpace","stockAnalyticsFeeder");
	    startAgent(0 ,"stockAnalyticsProcessor" ,"stockAnalyticsSpace");
	    assertEquals("Expecting exactly 4 grid service agents to be added", 4, getNumberOfGSAsAdded());
	    assertEquals("Expecting 0 agents to be removed", 0, getNumberOfGSAsRemoved());
	    
	    cassandraHostIp = admin.getZones().getByName("cassandra").getGridServiceAgents().getAgents()[0].getMachine().getHostAddress();
	}

	@Override
	@BeforeMethod
	public void beforeTest() {
		
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
	
/////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = false)
	public void testStockdemoApplication() throws Exception {	    
		assertInstallApp(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp , stockdemoAppDirPath);
		isStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
	public void testStockdemoAppCassandraPuInstFailOver() throws Exception{
		assertInstallApp(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp , stockdemoAppDirPath);
		isStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
		
		ProcessingUnit cassandra = admin.getProcessingUnits().getProcessingUnit("cassandra");
		int cassandraPuInstancesAfterInstall = cassandra.getInstances().length;
		LogUtils.log("destroying the pu instance holding cassandra");
		cassandra.getInstances()[0].destroy();
		assertPuInstanceKilled("cassandra" , cassandraPort1 , cassandraHostIp , cassandraPuInstancesAfterInstall);
		assertPuInstanceRessurected("cassandra" , cassandraPort1 , cassandraHostIp , cassandraPuInstancesAfterInstall);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
	public void testStockdemoAppCassandraGSCFailOver() throws Exception{
		assertInstallApp(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp , stockdemoAppDirPath);
		isStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
		
		ProcessingUnit cassandra = admin.getProcessingUnits().getProcessingUnit("cassandra");
		int cassandraPuInstancesAfterInstall = cassandra.getInstances().length;
		LogUtils.log("restarting GSC containing cassandra");
		cassandra.getInstances()[0].getGridServiceContainer().kill();
		assertPuInstanceKilled("cassandra" , cassandraPort1 ,cassandraHostIp , cassandraPuInstancesAfterInstall);
		assertPuInstanceRessurected("cassandra" , cassandraPort1 ,cassandraHostIp , cassandraPuInstancesAfterInstall);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
	public void testStockdemoAppStockAnalyticsSpacePuInstFailOver() throws Exception{
		assertInstallApp(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp , stockdemoAppDirPath);
		isStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
		
		ProcessingUnit stockAnalyticsSpace = admin.getProcessingUnits().getProcessingUnit("stockAnalyticsSpace");
		int spacePuInstancesAfterInstall = stockAnalyticsSpace.getInstances().length;
		LogUtils.log("destroying the pu instance holding stockAnalyticsSpace");
		stockAnalyticsSpace.getInstances()[0].destroy();
		assertPuInstanceKilled("stockAnalyticsSpace" , spacePuInstancesAfterInstall);
		assertPuInstanceRessurected("stockAnalyticsSpace" , spacePuInstancesAfterInstall);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
	public void testStockdemoAppStockAnalyticsSpaceGSCFailOver() throws Exception{
		assertInstallApp(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp , stockdemoAppDirPath);
		isStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
		
		ProcessingUnit stockAnalyticsSpace = admin.getProcessingUnits().getProcessingUnit("stockAnalyticsSpace");
		int spacePuInstancesAfterInstall = stockAnalyticsSpace.getInstances().length;
		LogUtils.log("restarting GSC containing stockAnalyticsFeeder");
		stockAnalyticsSpace.getInstances()[0].getGridServiceContainer().kill();
		assertPuInstanceKilled("stockAnalyticsSpace" , spacePuInstancesAfterInstall);
		assertPuInstanceRessurected("stockAnalyticsSpace" , spacePuInstancesAfterInstall);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testEsmRestart() throws Exception {
		assertInstallApp(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp , stockdemoAppDirPath);
		isStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
		
		ElasticServiceManager esm = admin.getElasticServiceManagers().waitForAtLeastOne();
		LogUtils.log("killing esm");
		killEsmAndWait(esm);
		
		LogUtils.log("asserting esm is managing enviroment");
		assertEsmIsManagingEnvBySearchingLogs(esm);
	}
}
