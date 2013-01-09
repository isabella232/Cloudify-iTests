package test.esm.component.rebalancing;

import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.internal.InternalAdminFactory;
import org.openspaces.grid.gsm.rebalancing.RebalancingSlaEnforcement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import test.esm.AbstractFromXenToByonGSMTest;
import framework.utils.AdminUtils;

public class AbstractRebalancingSlaEnforcementByonTest extends AbstractFromXenToByonGSMTest {

    protected static final int NUMBER_OF_OBJECTS = 100;
    protected final static String ZONE = "testzone";
    
    protected RebalancingSlaEnforcement rebalancing;
        
    @BeforeMethod
    public void beforeTest() {
		super.beforeTestInit();
		rebalancing = new RebalancingSlaEnforcement();
        rebalancing.enableTracing();
		
	}
	
	@BeforeClass
	protected void bootstrap() throws Exception {
		super.bootstrapBeforeClass();
	}
	
	
	@AfterClass(alwaysRun = true)
	protected void teardownAfterClass() throws Exception {
		super.teardownAfterClass();
	}
    
	// single threaded admin instead of default admin
    @Override
    protected AdminFactory createAdminFactory() {
    	return ((InternalAdminFactory) super.createAdminFactory()).singleThreadedEventListeners();
    }
    

    @Override
    @AfterMethod
    public void afterTest() {
        try {
            rebalancing.destroy();
        }
        finally {
            super.afterTest();
        }
    }
    
    protected GridServiceContainer[] loadGSCs(GridServiceAgent agent, int number) {
    	return AdminUtils.loadGSCsWithSystemProperty(agent, false, number, "-Dcom.gs.zones="+ZONE);
    }
}
