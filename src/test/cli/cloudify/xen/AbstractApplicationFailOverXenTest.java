package test.cli.cloudify.xen;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.esm.ElasticServiceManagers;
import org.openspaces.admin.esm.events.ElasticServiceManagerRemovedEventListener;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

import test.cli.cloudify.CommandTestUtils;

import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntryMatcher;
import com.gigaspaces.log.LogEntryMatchers;

import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.LogUtils;
import framework.utils.TestUtils;

/**
 * 
 * @author gal
 *	The extending classes start VMs by zones in the BeforeClass method and all their tests
 *	use those VMs (instead of tearing then down after each test) to save time
 */
public class AbstractApplicationFailOverXenTest extends AbstractStartManagementXenTest {
		
    protected Integer cassandraPort1;
    protected Integer cassandraPort2;
    protected String cassandraHostIp;
    
	protected String restUrl;
	protected final long DEFAULT_RECOVERY_TIME = DEFAULT_TEST_TIMEOUT/3;
	
	@Override
	@BeforeMethod
	public void beforeTest() {
		super.beforeTest();
		String host = admin.getElasticServiceManagers().getManagers()[0].getMachine().getHostAddress();
		restUrl = host + ":8100";		
	}
	
	protected void installApp(int port1 , String host1, int port2 ,String host2 ,String appDirPath) throws Exception{
		LogUtils.log("asserting needed ports for application are availible");
		boolean port1Availible = portIsAvailible(port1 ,host1);
		boolean port2Availible = portIsAvailible(port2 ,host2);
		assertTrue("port was not free befor installation - port number " + port1, port1Availible);
		assertTrue("port was not free befor installation - port number " + port2, port2Availible);
		    
		String commandOutput = null;
		commandOutput = CommandTestUtils.runCommandAndWait("connect --verbose " + restUrl + ";install-application --verbose " + appDirPath);
		String appName = new File(appDirPath).getName();
		assertTrue("install-application command didn't install the application" , commandOutput.contains("Application " + appName + " installed successfully"));
	}
	
	protected void assertPuInstanceKilled(final String puName, final int port ,final String host, final int puInstancesAfterInstall){
		LogUtils.log("asserting that " + puName + " was killed");
		TestUtils.repetitive(new Runnable(){
			@Override
			public void run() {
				ProcessingUnit pu = admin.getProcessingUnits().getProcessingUnit(puName);
				assertEquals(puName + " pu wasn't killed" ,puInstancesAfterInstall - 1, pu.getInstances().length);
				assertTrue(puName + " port was not free after kill - port number " + port, portIsAvailible(port ,host));
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
		assertTrue(puName + " pu was not recovered for " + DEFAULT_TEST_TIMEOUT + " seconds after fail over" ,
				pu.waitFor(puInstancesAfterInstall, DEFAULT_TEST_TIMEOUT, TimeUnit.SECONDS));
		LogUtils.log(puName + " pu was ressurected");
		
		if(port > 0){
			LogUtils.log("waiting for requried port to be taken");
			TestUtils.repetitive(new Runnable(){
				@Override
				public void run() {
					assertTrue(puName + " port was not occupied after ressurection - port number " + port, !portIsAvailible(port ,bindAddr));
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
				String absolutePUName = ServiceUtils.getAbsolutePUName(appName, "cassandra");
				
				ProcessingUnit cassandra = admin.getProcessingUnits().waitFor(absolutePUName, 1, TimeUnit.SECONDS);
				if (cassandra != null){
					LogUtils.log("Cassandra instance is still found by admin. StockDemo was not properly uninstalled.");
				}
				Application application = admin.getApplications().getApplication(appName);
				if (application != null) {
					LogUtils.log("Application stockdemo is not null.");
				}
				boolean condition = cassandra==null && application == null;
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
	
	protected void assertStockdemoAppInstalled(int port1 ,String host1 ,int port2 ,String host2) throws InterruptedException {
		ProcessingUnit stockAnalyticsSpace = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("stockdemo", "stockAnalyticsSpace"));
		ProcessingUnit cassandra = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("stockdemo","cassandra"));
		ProcessingUnit stockAnalytics = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("stockdemo","stockAnalytics"));
		ProcessingUnit stockAnalyticsFeeder = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("stockdemo","stockAnalyticsFeeder"));
		ProcessingUnit stockAnalyticsMirror = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("stockdemo","stockAnalyticsMirror"));
		ProcessingUnit stockAnalyticsProcessor = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("stockdemo","stockAnalyticsProcessor"));
		ProcessingUnit stockDemo = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("stockdemo","StockDemo"));
		
		assertNotNull("cassandra pu didn't deploy", cassandra);
		assertNotNull("stockAnalytics pu didn't deploy", stockAnalytics);
		assertNotNull("stockAnalyticsFeeder pu didn't deploy", stockAnalyticsFeeder);
		assertNotNull("stockAnalyticsMirror pu didn't deploy", stockAnalyticsMirror);
		assertNotNull("stockAnalyticsSpace pu didn't deploy", stockAnalyticsSpace);
		assertNotNull("stockAnalyticsProcessor pu didn't deploy", stockAnalyticsProcessor);
		assertNotNull("StockDemo pu didn't deploy", stockDemo);
		
		cassandra.waitFor(cassandra.getTotalNumberOfInstances());
		stockAnalytics.waitFor(stockAnalytics.getTotalNumberOfInstances());
		stockAnalyticsFeeder.waitFor(stockAnalyticsFeeder.getTotalNumberOfInstances());
		stockAnalyticsMirror.waitFor(stockAnalyticsMirror.getTotalNumberOfInstances());
		stockAnalyticsSpace.waitFor(stockAnalyticsSpace.getTotalNumberOfInstances());
		stockAnalyticsProcessor.waitFor(stockAnalyticsProcessor.getTotalNumberOfInstances());
		stockDemo.waitFor(stockDemo.getTotalNumberOfInstances());
		
		Thread.sleep(1000 * 20); // sleep to give redundent instances time to start
		
		assertEquals("cassandra pu didn't deploy all of it's instances. Expecting " 
							+ cassandra.getTotalNumberOfInstances() + ", got " + cassandra.getInstances().length, 
							cassandra.getInstances().length, cassandra.getTotalNumberOfInstances());
		assertEquals("stockAnalytics pu didn't deploy all of it's instances. Expecting "
							+ stockAnalytics.getTotalNumberOfInstances() + ", got " + stockAnalytics.getInstances().length
							, stockAnalytics.getInstances().length, stockAnalytics.getTotalNumberOfInstances());
		assertEquals("stockAnalyticsFeeder pu didn't deploy all of it's instances. Expecting " 
							+ stockAnalyticsFeeder.getTotalNumberOfInstances() + ", got " + stockAnalyticsFeeder.getInstances().length
							, stockAnalyticsFeeder.getTotalNumberOfInstances(), stockAnalyticsFeeder.getInstances().length);
		assertEquals("stockAnalyticsMirror pu didn't deploy all of it's instances. Expecting " 
							+ stockAnalyticsMirror.getTotalNumberOfInstances() + ", got " + stockAnalyticsMirror.getInstances().length
							, stockAnalyticsMirror.getTotalNumberOfInstances(),  stockAnalyticsMirror.getInstances().length);
		assertEquals("stockAnalyticsSpace pu didn't deploy all of it's instances. Expecting " 
				+ stockAnalyticsSpace.getTotalNumberOfInstances() + ", got " + stockAnalyticsSpace.getInstances().length
				, stockAnalyticsSpace.getTotalNumberOfInstances(),  stockAnalyticsSpace.getInstances().length);
		assertEquals("stockAnalyticsProcessor pu didn't deploy all of it's instances. Expecting " 
				+ stockAnalyticsProcessor.getTotalNumberOfInstances() + ", got " + stockAnalyticsProcessor.getInstances().length
				, stockAnalyticsProcessor.getTotalNumberOfInstances(),  stockAnalyticsProcessor.getInstances().length);
		assertEquals("stockDemo pu didn't deploy all of it's instances. Expecting " 
				+ stockDemo.getTotalNumberOfInstances() + ", got " + stockDemo.getInstances().length
				, stockDemo.getTotalNumberOfInstances(),  stockDemo.getInstances().length);
		
		if(port1 > 0  && port2 > 0){
			LogUtils.log("asserting needed ports for application are taken");
			boolean port1Availible = portIsAvailible(port1 ,host1);
			boolean port2Availible = portIsAvailible(port2 ,host2);
			assertTrue("port was not occupied after installation - port number " + port1, !port1Availible);
			assertTrue("port was not occupied after installation - port number " + port2, !port2Availible);
		}		
		LogUtils.log("Stockdemo application installed");
	}
	
	protected void assignCassandraPorts(String stockdemoAppDirPath) {
		Service cassandraService = null;
		try {
			cassandraService = ServiceReader.getServiceFromDirectory(new File(stockdemoAppDirPath + "/cassandra"), CloudifyConstants.DEFAULT_APPLICATION_NAME).getService();
		} catch (Exception e) {
		    AssertFail("Failed assiging cassandra ports", e);
		} 
		ArrayList<Integer> cassandraPorts = (ArrayList<Integer>) cassandraService.getPlugins().get(0).getConfig().get("Port");
		cassandraPort1 = cassandraPorts.get(0);
		cassandraPort2 = cassandraPorts.get(1);
	}
	
	protected void assertEsmIsManagingEnvBySearchingLogs(ElasticServiceManager esm) {
		
		boolean esmRestarted = admin.getElasticServiceManagers().waitFor(1, DEFAULT_RECOVERY_TIME, TimeUnit.MILLISECONDS);
		assertTrue("esm did not restart" , esmRestarted);
		
		String format1 = "Elastic properties for pu %s are being enforced";
		
		// TODO: ask gal why did he search for these strings.. I could not find them anywhere
//		String format2 = "Machines eager SLA for %s has been reached";
//		String format3 = "Eager containers SLA for %s has been reached";
		
		for(ProcessingUnit pu : admin.getProcessingUnits().getProcessingUnits()){
			
			String requiredText1 = String.format(format1, pu.getName());
//			String requiredText2 = String.format(format2, pu.getName());
//			String requiredText3 = String.format(format3, pu.getName());
			
			repetativeScanElasticManagerLogsFor(esm, requiredText1, DEFAULT_RECOVERY_TIME);
//			repetativeScanElasticManagerLogsFor(esm, requiredText2, DEFAULT_RECOVERY_TIME);
//			repetativeScanElasticManagerLogsFor(esm, requiredText3, DEFAULT_RECOVERY_TIME);
		}
	}
	
	/**
     * scans the logs of the ESM repetatively for a specific text message
     * assert fails if text is not found in logs after timeout passes
     * @param esm - the esm to scan
     * @param text - the text to look for
     */
    
    protected static void repetativeScanElasticManagerLogsFor(final ElasticServiceManager esm , final String text, long timeoutInMillis) {
        RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
            public boolean getCondition() {
                LogEntryMatcher matcher = LogEntryMatchers.containsString(text);
                LogEntries logEntriesGsm = esm.logEntries(matcher);
                return logEntriesGsm.getEntries().size() > 1;
            }
        };
        AssertUtils.repetitiveAssertTrue("Failed finding: " + text + " in the gsm logs", condition, timeoutInMillis);       
    }
    
   
	
    protected void killEsmAndWait(final ElasticServiceManager esm){
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
