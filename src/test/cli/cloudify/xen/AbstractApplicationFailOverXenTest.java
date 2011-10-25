package test.cli.cloudify.xen;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.testng.annotations.BeforeMethod;

import test.cli.cloudify.CommandTestUtils;
import test.utils.AssertUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.LogUtils;
import test.utils.TestUtils;

import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;

/**
 * 
 * @author gal
 *	The extending classes start VMs by zones in the BeforeClass method and all their tests
 *	use those VMs (instead of tearing then down after each test) to save time
 */
public class AbstractApplicationFailOverXenTest extends AbstractStartManagementXenTest {
		
	protected String restUrl;
	
	public AbstractApplicationFailOverXenTest(){
		
	}
	
	@Override
	@BeforeMethod
	public void beforeTest() {
		super.beforeTest();
		String host = admin.getElasticServiceManagers().getManagers()[0].getMachine().getHostAddress();
		restUrl = host + ":8100";		
	}
	
	protected void assertInstallApp(int port1 , String host1, int port2 ,String host2 ,String appDirPath) throws Exception{
		LogUtils.log("asserting needed ports for application are availible");
		boolean port1Availible = portIsAvailible(port1 ,host1);
		boolean port2Availible = portIsAvailible(port2 ,host2);
		assertTrue("port was not free befor installation - port number " + port1, port1Availible);
		assertTrue("port was not free befor installation - port number " + port2, port2Availible);
		    
		String commandOutput = CommandTestUtils.runCommandAndWait("connect " + restUrl + ";install-application " + appDirPath);
		String appName = new File(appDirPath).getName();
		assertTrue("install-application command didn't install the application" , commandOutput.contains("Application " + appName + " installed successfully"));
	}
	
	protected void assertPuInstanceKilled(final String puName, final int port ,final String host, final int puInstancesAfterInstall){
		LogUtils.log("asserting that " + puName + " was killed");
		TestUtils.repetitive(new Runnable(){
			@Override
			public void run() {
				ProcessingUnit pu = admin.getProcessingUnits().getProcessingUnit(puName);
				Assert.assertEquals(puName + " pu wasn't killed" ,puInstancesAfterInstall - 1, pu.getInstances().length);
				Assert.assertTrue(puName + " port was not free after kill - port number " + port, portIsAvailible(port ,host));
			}
		}, 1000 * 60);
	}
//	
//	protected void killProcessingUnitInstance(final ProcessingUnitInstance pui){
//		pui.destroy();
//		final CountDownLatch latch = new CountDownLatch(2);
//        ProcessingUnitInstanceLifecycleEventListener added = new ProcessingUnitInstanceLifecycleEventListener() {
//            public void processingUnitInstanceAdded(ProcessingUnitInstance processingUnitInstance) {
//            	if (!processingUnitInstance.getUid().equals(pui.getUid()) && processingUnitInstance.getClusterInfo().getRunningNumber() == pui.getClusterInfo().getRunningNumber()) 
//                    latch.countDown();
//                
//            }
//            public void processingUnitInstanceRemoved(ProcessingUnitInstance processingUnitInstance) {    
//            	if (processingUnitInstance.getUid().equals(pui.getUid())) 
//            		latch.countDown();
//            }
//        };
//        pui.getProcessingUnit().addLifecycleListener(added);
//	}
//	
//	protected void killGSC(GridServiceContainer gsc){
//		gsc.kill();
//		final CountDownLatch latch = new CountDownLatch(2);
//        GridServiceContainerLifecycleEventListener added = new GridServiceContainerLifecycleEventListener() {
//            
//			@Override
//			public void gridServiceContainerAdded(GridServiceContainer gridServiceContainer) {
//				
//			}
//			@Override
//			public void gridServiceContainerRemoved(GridServiceContainer gridServiceContainer) {
//						
//			}
//        };
//	}
//	
	protected void assertPuInstanceRessurected(final String puName , final int port, final String bindAddr ,int puInstancesAfterInstall) {
		ProcessingUnit pu = admin.getProcessingUnits().waitFor(puName);
		LogUtils.log("waiting for " + puName + " pu instance ressurection");
		Assert.assertTrue(puName + " pu was not recovered for " + DEFAULT_TEST_TIMEOUT + " seconds after fail over" ,
				pu.waitFor(puInstancesAfterInstall, DEFAULT_TEST_TIMEOUT, TimeUnit.SECONDS));
		LogUtils.log(puName + " pu was ressurected");
		
		if(port > 0){
			LogUtils.log("waiting for requried port to be taken");
			TestUtils.repetitive(new Runnable(){
				@Override
				public void run() {
					Assert.assertTrue(puName + " port was not occupied after ressurection - port number " + port, !portIsAvailible(port ,bindAddr));
				}
			}, 1000 * 60);
		}
	}
	
	protected void assertPuInstanceRessurected(String puName , int puInstancesAfterInstall) {
		assertPuInstanceRessurected(puName , -1 ,null , puInstancesAfterInstall);
	}

	protected void assertPuInstanceKilled(String puName , int puInstancesAfterInstall) {
		assertPuInstanceRessurected(puName , -1 ,null , puInstancesAfterInstall);
	}
	
	protected void assertAppUninstalled(final String appName) {
		AssertUtils.repetitiveAssertTrue(appName + " application was not uninstalled", new RepetitiveConditionProvider() {				
			@Override
			public boolean getCondition() {			
				ProcessingUnit cassandra = admin.getProcessingUnits().waitFor("cassandra", 1, TimeUnit.SECONDS);
				boolean condition = cassandra==null && admin.getApplications().getApplication(appName) == null;
				return condition;
			}
		}, DEFAULT_TEST_TIMEOUT);
	}
	
	protected boolean portIsAvailible(int port , String host){
		Socket sock = null;
		try {
			sock = new Socket(host , port);
		    return false;
		} catch (Exception e) {
			return true;
		}  finally {
			if (sock != null)
				try {
					sock.close();
				} catch (IOException e) {
				}
		}				
	}
	
	protected void isStockdemoAppInstalled(int port1 ,String host1 ,int port2 ,String host2) throws InterruptedException {
		Space stockAnalyticsSpace = admin.getSpaces().waitFor("stockAnalyticsSpace");
		ProcessingUnit cassandra = admin.getProcessingUnits().waitFor("cassandra");
		ProcessingUnit stockAnalytics = admin.getProcessingUnits().waitFor("stockAnalytics");
		ProcessingUnit stockAnalyticsFeeder = admin.getProcessingUnits().waitFor("stockAnalyticsFeeder");
		ProcessingUnit stockAnalyticsMirror = admin.getProcessingUnits().waitFor("stockAnalyticsMirror");
		ProcessingUnit stockAnalyticsProcessor = admin.getProcessingUnits().waitFor("stockAnalyticsProcessor");
		ProcessingUnit StockDemo = admin.getProcessingUnits().waitFor("StockDemo");
		
		Assert.assertNotNull("cassandra pu didn't deploy", cassandra);
		Assert.assertNotNull("stockAnalytics pu didn't deploy", stockAnalytics);
		Assert.assertNotNull("stockAnalyticsFeeder pu didn't deploy", stockAnalyticsFeeder);
		Assert.assertNotNull("stockAnalyticsMirror pu didn't deploy", stockAnalyticsMirror);
		Assert.assertNotNull("stockAnalyticsSpace pu didn't deploy", stockAnalyticsSpace);
		Assert.assertNotNull("stockAnalyticsProcessor pu didn't deploy", stockAnalyticsProcessor);
		Assert.assertNotNull("StockDemo pu didn't deploy", StockDemo);
		
		cassandra.waitFor(cassandra.getTotalNumberOfInstances());
		stockAnalytics.waitFor(stockAnalytics.getTotalNumberOfInstances());
		stockAnalyticsFeeder.waitFor(stockAnalyticsFeeder.getTotalNumberOfInstances());
		stockAnalyticsMirror.waitFor(stockAnalyticsMirror.getTotalNumberOfInstances());
		stockAnalyticsSpace.waitFor(stockAnalyticsSpace.getTotalNumberOfInstances());
		stockAnalyticsProcessor.waitFor(stockAnalyticsProcessor.getTotalNumberOfInstances());
		StockDemo.waitFor(StockDemo.getTotalNumberOfInstances());
		
		Thread.sleep(1000 * 20); // sleep to give redundent instances time to start
		
		boolean cassandraDeployed = cassandra.getInstances().length == cassandra.getTotalNumberOfInstances();
		boolean stockAnalyticsDeployed = stockAnalytics.getInstances().length == stockAnalytics.getTotalNumberOfInstances();
		boolean stockAnalyticsFeederDeployed = stockAnalyticsFeeder.getInstances().length == stockAnalyticsFeeder.getTotalNumberOfInstances();
		boolean stockAnalyticsMirrorDeployed = stockAnalyticsMirror.getInstances().length == stockAnalyticsMirror.getTotalNumberOfInstances();
		boolean stockAnalyticsSpaceDeployed = stockAnalyticsSpace.getInstances().length == stockAnalyticsSpace.getTotalNumberOfInstances();
		boolean stockAnalyticsProcessorDeployed = stockAnalyticsProcessor.getInstances().length == stockAnalyticsProcessor.getTotalNumberOfInstances();
		boolean StockDemoDeployed = StockDemo.getInstances().length == StockDemo.getTotalNumberOfInstances();
		
		Assert.assertTrue("cassandra pu didn't deploy all of it's instances", cassandraDeployed);
		Assert.assertTrue("stockAnalytics pu didn't deploy all of it's instances", stockAnalyticsDeployed);
		Assert.assertTrue("stockAnalyticsFeeder pu didn't deploy all of it's instances", stockAnalyticsFeederDeployed);
		Assert.assertTrue("stockAnalyticsMirror pu didn't deploy all of it's instances", stockAnalyticsMirrorDeployed);
		Assert.assertTrue("stockAnalyticsSpace pu didn't deploy all of it's instances", stockAnalyticsSpaceDeployed);
		Assert.assertTrue("stockAnalyticsProcessor pu didn't deploy all of it's instances", stockAnalyticsProcessorDeployed);
		Assert.assertTrue("StockDemo pu didn't deploy all of it's instances", StockDemoDeployed);
		
		if(port1 > 0  && port2 > 0){
			LogUtils.log("asserting needed ports for application are taken");
			boolean port1Availible = portIsAvailible(port1 ,host1);
			boolean port2Availible = portIsAvailible(port2 ,host2);
			assertTrue("port was not occupied after installation - port number " + port1, !port1Availible);
			assertTrue("port was not occupied after installation - port number " + port2, !port2Availible);
		}		
		LogUtils.log("Stockdemo application installed");
	}
	
	protected void assignCassandraPorts(int cassandraPort1 , int cassandraPort2 , String stockdemoAppDirPath){
		Service cassandraService = null;
		try {
			cassandraService = ServiceReader.getServiceFromDirectory(new File(stockdemoAppDirPath + "/cassandra")).getService();
		} catch (Exception e) {
			e.printStackTrace();
		} 
		ArrayList<Integer> cassandraPorts = (ArrayList<Integer>) cassandraService.getPlugins().get(0).getConfig().get("Port");
		cassandraPort1 = cassandraPorts.get(0);
		cassandraPort2 = cassandraPorts.get(1);
	}
}
