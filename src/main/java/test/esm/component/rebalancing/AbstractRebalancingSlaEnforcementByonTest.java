package test.esm.component.rebalancing;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.grid.gsm.rebalancing.RebalancingSlaEnforcement;

import test.esm.AbstractFromXenToByonGSMTest;
import framework.utils.AdminUtils;

public class AbstractRebalancingSlaEnforcementByonTest extends AbstractFromXenToByonGSMTest {

    protected static final int NUMBER_OF_OBJECTS = 100;
    protected final static String ZONE = "testzone";
    protected RebalancingSlaEnforcement rebalancing;
           
    protected GridServiceContainer[] loadGSCs(GridServiceAgent agent, int number) {
    	return AdminUtils.loadGSCsWithSystemProperty(agent, false, number, "-Dcom.gs.zones="+ZONE);
    }
}
