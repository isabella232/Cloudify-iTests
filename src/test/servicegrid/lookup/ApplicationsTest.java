package test.servicegrid.lookup;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.LogUtils.log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.application.events.ApplicationLifecycleEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.events.ProcessingUnitAddedEventListener;
import org.openspaces.admin.pu.events.ProcessingUnitRemovedEventListener;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.util.ConcurrentHashSet;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AdminUtils;
import test.utils.AssertUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;

/**
 * Test case for GS-8920
 * APIs for applications
 * @author itaif
 */
public class ApplicationsTest extends AbstractTest{

    private static final int MAKE_SURE_APP_NOT_REMOVED_TIMEOUT_SECONDS = 5;
	private static final String APP1_NAME = "app1";
	private static final String APP2_NAME = "app2";
	CountDownLatch app1AddedCounter;
    CountDownLatch app1RemovedCounter;
    CountDownLatch app2AddedCounter;
    CountDownLatch app2RemovedCounter;

    @BeforeMethod
    @Override
    public void beforeTest() {
    	super.beforeTest();
    	app1AddedCounter = new CountDownLatch(1);
        app1RemovedCounter = new CountDownLatch(1);
        app2AddedCounter = new CountDownLatch(1);
        app2RemovedCounter = new CountDownLatch(1);
        admin.getApplications().addLifecycleListener(new ApplicationLifecycleEventListener() {
			
			public void applicationAdded(final Application application) {
				if (application.getName().equals(APP1_NAME)) {
					ApplicationsTest.assertTrue(!isApp1Added());
					ApplicationsTest.assertTrue(!isApp1Removed());
					app1AddedCounter.countDown();
				}
				else if (application.getName().equals(APP2_NAME)) {
					ApplicationsTest.assertTrue(!isApp2Added());
					ApplicationsTest.assertTrue(!isApp2Removed());
					app2AddedCounter.countDown();
				}
				else {
					Assert.fail("No such application " + application.getName());
				}
			}
			
			public void applicationRemoved(final Application application) {
				if (application.getName().equals(APP1_NAME)) {
					ApplicationsTest.assertTrue(isApp1Added());
					ApplicationsTest.assertTrue(!isApp1Removed());
					app1RemovedCounter.countDown();
				}
				else if (application.getName().equals(APP2_NAME)) {
					ApplicationsTest.assertTrue(isApp2Added());
					ApplicationsTest.assertTrue(!isApp2Removed());
					app2RemovedCounter.countDown();
				}
				else {
					Assert.fail("No such application " + application.getName());
				}
			}
		});
    }
    
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() throws InterruptedException {
		
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        Machine machine = gsa.getMachine();
        
        log("loading 1 GSM, 2 GSCs on " + machine.getHostName());
        GridServiceManager gsm = AdminUtils.loadGSM(machine.getGridServiceAgent());
        loadGSCs(machine, 2);
        admin.getGridServiceContainers().waitFor(2);
        
        log("deploy PU when 2 GSMs and 2GSCs already running");
        final ProcessingUnit space1 = gsm.deploy(new SpaceDeployment("app1space1").partitioned(1, 0).setContextProperty("com.gs.application", APP1_NAME));
        final ProcessingUnit space2 = gsm.deploy(new SpaceDeployment("app1space2").partitioned(1, 0).setContextProperty("com.gs.application", APP1_NAME));
        final ProcessingUnit space3 = gsm.deploy(new SpaceDeployment("app2space3").partitioned(1, 0).setContextProperty("com.gs.application", APP2_NAME));
        ProcessingUnit space4 = gsm.deploy(new SpaceDeployment("space4").partitioned(1, 0));
        space1.waitForManaged();
        space2.waitForManaged();
        space3.waitForManaged();
        space4.waitForManaged();
        
        Application app1 = admin.getApplications().waitFor(APP1_NAME);
        Application app2 = admin.getApplications().waitFor(APP2_NAME);
        assertEquals(APP1_NAME,app1.getName());
        assertEquals(APP2_NAME,app2.getName());
        assertTrue(isApp1Added());
        assertTrue(isApp2Added());
        assertTrue(!isApp1Removed());
        assertTrue(!isApp2Removed());
        
        Set<String> applicationNames = admin.getApplications().getNames().keySet();
        assertEquals(2,applicationNames.size());
        assertTrue(applicationNames.contains(APP1_NAME));
        assertTrue(applicationNames.contains(APP2_NAME));
        
        Set<ProcessingUnit> app1ProcessingUnits = new HashSet<ProcessingUnit>(
        		Arrays.asList(admin.getApplications().getApplication(APP1_NAME).getProcessingUnits().getProcessingUnits()));
        assertEquals(2,app1ProcessingUnits.size());
        assertTrue(app1ProcessingUnits.contains(space1));
        assertTrue(app1ProcessingUnits.contains(space2));
        Set<ProcessingUnit> app2ProcessingUnits = new HashSet<ProcessingUnit>(
        		Arrays.asList(admin.getApplications().getApplication(APP2_NAME).getProcessingUnits().getProcessingUnits()));
        assertEquals(1,app2ProcessingUnits.size());
        assertTrue(app2ProcessingUnits.contains(space3));
        
        assertTrue(null==admin.getApplications().getApplication("app3"));
        
        assertEquals(APP1_NAME,space1.getApplication().getName());
        assertEquals(APP1_NAME,space2.getApplication().getName());
        assertEquals(APP2_NAME,space3.getApplication().getName());
        assertTrue(null==space4.getApplication());
        
        final Set<String> app1ProcessingUnitsAdded = new ConcurrentHashSet<String>();
        final Set<String> app2ProcessingUnitsAdded = new ConcurrentHashSet<String>();
        final Set<String> app1ProcessingUnitsRemoved = new ConcurrentHashSet<String>();
        final Set<String> app2ProcessingUnitsRemoved = new ConcurrentHashSet<String>();

        admin.getApplications().getApplication(APP1_NAME).getProcessingUnits().getProcessingUnitAdded().add(new ProcessingUnitAddedEventListener() {

			public void processingUnitAdded(ProcessingUnit processingUnit) {
				app1ProcessingUnitsAdded.add(processingUnit.getName());
			}
		});

        admin.getApplications().getApplication(APP1_NAME).getProcessingUnits().getProcessingUnitRemoved().add(new ProcessingUnitRemovedEventListener() {

			public void processingUnitRemoved(ProcessingUnit processingUnit) {
				app1ProcessingUnitsRemoved.add(processingUnit.getName());
			}
		});

        admin.getApplications().getApplication(APP2_NAME).getProcessingUnits().getProcessingUnitAdded().add(new ProcessingUnitAddedEventListener() {

			public void processingUnitAdded(ProcessingUnit processingUnit) {
				app2ProcessingUnitsAdded.add(processingUnit.getName());
			}
		});

        admin.getApplications().getApplication(APP2_NAME).getProcessingUnits().getProcessingUnitRemoved().add(new ProcessingUnitRemovedEventListener() {

			public void processingUnitRemoved(ProcessingUnit processingUnit) {
				app2ProcessingUnitsRemoved.add(processingUnit.getName());			
			}
		});
        
        assertTrue(app1ProcessingUnitsAdded.contains(space1.getName()));
        assertTrue(app1ProcessingUnitsAdded.contains(space2.getName()));
        assertTrue(!app1ProcessingUnitsAdded.contains(space3.getName()));
        assertTrue(!app1ProcessingUnitsAdded.contains(space4.getName()));
        assertTrue(!app1ProcessingUnitsRemoved.contains(space1.getName()));
        assertTrue(!app1ProcessingUnitsRemoved.contains(space2.getName()));
        assertTrue(!app1ProcessingUnitsRemoved.contains(space3.getName()));
        assertTrue(!app1ProcessingUnitsRemoved.contains(space4.getName()));
        
        assertTrue(!app2ProcessingUnitsAdded.contains(space1.getName()));
        assertTrue(!app2ProcessingUnitsAdded.contains(space2.getName()));
        assertTrue(app2ProcessingUnitsAdded.contains(space3.getName()));
        assertTrue(!app2ProcessingUnitsAdded.contains(space4.getName()));
        assertTrue(!app2ProcessingUnitsRemoved.contains(space1.getName()));
        assertTrue(!app2ProcessingUnitsRemoved.contains(space2.getName()));
        assertTrue(!app2ProcessingUnitsRemoved.contains(space3.getName()));
        assertTrue(!app2ProcessingUnitsRemoved.contains(space4.getName()));
        
        space1.undeploy();
        assertNotNull(admin.getApplications().getApplication(space1.getApplication().getName()));
        
        assertTrue(isApp1Added());
        assertTrue(!waitForApp1Removed(MAKE_SURE_APP_NOT_REMOVED_TIMEOUT_SECONDS,TimeUnit.SECONDS));
        assertTrue(isApp2Added());
        assertTrue(!waitForApp2Removed(MAKE_SURE_APP_NOT_REMOVED_TIMEOUT_SECONDS,TimeUnit.SECONDS));
        AssertUtils.repetitiveAssertTrue("waiting for space1 to be removed from app1", new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return app1ProcessingUnitsRemoved.contains(space1.getName());
			}
        	
        }, OPERATION_TIMEOUT);
        assertTrue(!app1ProcessingUnitsRemoved.contains(space2.getName()));
        assertTrue(!app2ProcessingUnitsRemoved.contains(space1.getName()));
        
        space2.undeploy();
        assertTrue(null==admin.getApplications().getApplication(space2.getApplication().getName()));
        assertTrue(isApp1Added());
        assertTrue(waitForApp1Removed(MAKE_SURE_APP_NOT_REMOVED_TIMEOUT_SECONDS,TimeUnit.SECONDS));
        assertTrue(isApp2Added());
        assertTrue(!waitForApp2Removed(MAKE_SURE_APP_NOT_REMOVED_TIMEOUT_SECONDS,TimeUnit.SECONDS));
        assertTrue(app1ProcessingUnitsRemoved.contains(space1.getName()));
        AssertUtils.repetitiveAssertTrue("waiting for space1 to be removed from app1", new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return app1ProcessingUnitsRemoved.contains(space2.getName());
			}
        	
        }, OPERATION_TIMEOUT);
        assertTrue(!app2ProcessingUnitsRemoved.contains(space1.getName()));
        
        space3.undeploy();
        assertTrue(null==admin.getApplications().getApplication(space3.getApplication().getName()));
        assertTrue(isApp1Added());
        assertTrue(waitForApp1Removed(MAKE_SURE_APP_NOT_REMOVED_TIMEOUT_SECONDS,TimeUnit.SECONDS));
        assertTrue(isApp2Added());
        assertTrue(waitForApp2Removed(MAKE_SURE_APP_NOT_REMOVED_TIMEOUT_SECONDS,TimeUnit.SECONDS));
        assertTrue(app1ProcessingUnitsRemoved.contains(space1.getName()));
        assertTrue(app1ProcessingUnitsRemoved.contains(space2.getName()));
        AssertUtils.repetitiveAssertTrue("waiting for space1 to be removed from app1", new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return app2ProcessingUnitsRemoved.contains(space3.getName());
			}
        	
        }, OPERATION_TIMEOUT);
    }

	private boolean isApp1Added() {
		return app1AddedCounter.getCount() == 0;
	}
	
	private boolean isApp1Removed() {
		return app1RemovedCounter.getCount() == 0;
	}
	
	private boolean waitForApp1Removed(long timeout, TimeUnit unit) throws InterruptedException {
		return app1RemovedCounter.await(timeout, unit);
	}
	
	private boolean isApp2Added() {
		return app2AddedCounter.getCount() == 0;
	}
	
	private boolean isApp2Removed() {
		return app2RemovedCounter.getCount() == 0;
	}
	
	private boolean waitForApp2Removed(long timeout, TimeUnit unit) throws InterruptedException {
		return app2RemovedCounter.await(timeout, unit);
	}

}
