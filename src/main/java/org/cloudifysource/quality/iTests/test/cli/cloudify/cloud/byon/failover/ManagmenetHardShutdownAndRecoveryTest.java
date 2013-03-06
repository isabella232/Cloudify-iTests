package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.failover;

import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;

/**
 * Created with IntelliJ IDEA.
 * User: sagib
 * Date: 3/5/13
 * Time: 5:32 PM
 */
public class ManagmenetHardShutdownAndRecoveryTest extends AbstractByonManagementPersistencyTest {

    @Override
    public void shutdownManagement() throws Exception{
        GridServiceManager[] griServiceManagers = admin.getGridServiceManagers().getManagers();
        Machine[] gsmMachines = new Machine[griServiceManagers.length];
        for (int i = 0 ; i < griServiceManagers.length ; i++) {
            gsmMachines[i] = griServiceManagers[i].getMachine();
        }
        for (Machine machine : gsmMachines) {
            AbstractKillManagementTest.restartMachineAndWait(machine.getHostAddress());
        }
    }


}
