package test.gsm.component.rebalancing;

import org.openspaces.admin.Admin;
import org.openspaces.grid.gsm.rebalancing.RebalancingSlaEnforcement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import test.gsm.AbstractGsmTest;
import test.utils.AdminUtils;

/*
 * Tests rebalancing component functionality.
 *
 * Before running this test locally open a command prompt and run:
 * set LOOKUPGROUPS=itaif-laptop
 * set JSHOMEDIR=D:\eclipse_workspace\SGTest\tools\gigaspaces
 * start cmd /c "%JSHOMEDIR%\bin\gs-agent.bat gsa.global.esm 0 gsa.gsc 0 gsa.global.gsm 0 gsa.global.lus 1"
 *
 * on linux:
 * export LOOKUPGROUPS=itaif-laptop
 * export JSHOMEDIR=~/gigaspaces
 * nohup ${JSHOMEDIR}/bin/gs-agent.sh gsa.global.esm 0 gsa.gsc 0 gsa.global.gsm 0 gsa.global.lus 1 &
 */

public abstract class AbstractRebalancingSlaEnforcementTest extends AbstractGsmTest {
    
    protected static final int NUMBER_OF_OBJECTS = 100;
    protected final static String ZONE = "testzone";
    
    protected RebalancingSlaEnforcement rebalancing;
		
    @Override
    protected Admin newAdmin() {
        return AdminUtils.createSingleThreadAdmin();
    }
	
    @Override
    @BeforeMethod
    public void beforeTest() {
    	super.beforeTest();
    	rebalancing = new RebalancingSlaEnforcement();
		rebalancing.enableTracing();
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

}