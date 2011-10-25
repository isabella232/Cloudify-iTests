package test.servicegrid.failover;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSMs;
import static test.utils.LogUtils.log;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.gsm.events.GridServiceManagerRemovedEventListener;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.Assert;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.DeploymentUtils;
import test.utils.ProcessingUnitUtils;

/**
 * Tests that the dynamic property numberOfInstances is replicated after multiple failovers.
 * @author itaif
 *
 */
public class NumberOfInstancesAfterFailoverTest extends AbstractTest {

	@Test(timeOut=DEFAULT_TEST_TIMEOUT)
	public void failoverTest() {
	    
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		Machine machine = gsa.getMachine();
		
		log("loading 2 GSM, 2 GSCs on " + machine.getHostName());
		GridServiceManager[] gsms = loadGSMs(machine, 2);
		admin.getGridServiceManagers().waitFor(2);
		assertEquals(2,admin.getGridServiceManagers().getSize());
		loadGSCs(machine, 2);
		admin.getGridServiceContainers().waitFor(2);
		assertEquals(2,admin.getGridServiceContainers().getSize());
		
	
		log("deploy PU when 2 GSMs and 2GSCs already running");
		File archive = DeploymentUtils.getArchive("servlet.war");
		ProcessingUnit pu = deploy2InstancesOn2Containers(archive);
		pu.waitForManaged();
		
		// wait until PU has a backup GSM
		GridServiceManager gsm = pu.getManagingGridServiceManager();
		GridServiceManager backupGsm = gsms[0].equals(gsm) ? gsms[1] : gsms[0]; 
		ProcessingUnitUtils.waitForBackupGsm(pu, backupGsm);
		
		//kill managing GSM
		log("killing managing GSM and waiting for backup to take its place");
		killGsm(gsm);
		ProcessingUnitUtils.waitForManaged(pu, backupGsm);
		
		//delete numberOfInstances cached value
		pu = null;
	
		
		Admin admin2 = newAdmin();
		try {
			ProcessingUnit pu2 = admin2.getProcessingUnits().waitFor("servlet");
			pu2.waitForManaged();
			assertEquals(3, pu2.getNumberOfInstances());	
			GridServiceManager gsm2 = pu2.getManagingGridServiceManager();
			log("Starting a new backup GSM");
			GridServiceManager backupGsm2 = loadGSMs(machine,1)[0];
			ProcessingUnitUtils.waitForBackupGsm(pu2, backupGsm2);
			log("killing managing GSM and waiting for backup to take its place");
			killGsm(gsm2);	
			ProcessingUnitUtils.waitForManaged(pu2, backupGsm2);
			
			//delete numberOfInstances cached value
			pu2 = null;
		}
		finally {
			admin2.close();
			admin2=null;
		}
		
		Admin admin3 = newAdmin();
		try {
			ProcessingUnit pu3 = admin3.getProcessingUnits().waitFor("servlet");
			pu3.waitForManaged();
			assertEquals(3, pu3.getNumberOfInstances());
		}
		finally {
			admin3.close();
		}
	}

	private ProcessingUnit deploy2InstancesOn2Containers(File archive) {
		// requesting 3 instances, although we have only 2 GSCs
		final ProcessingUnit pu = admin.getGridServiceManagers().deploy(
				new ProcessingUnitDeployment(archive)
				.numberOfInstances(2)
				.maxInstancesPerVM(1));
		
		pu.waitFor(2);
		pu.incrementInstance();
		while (pu.getNumberOfInstances() != 3) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				AssertFail(e.getMessage());
			}
		}
		
		return pu;
	}

    private void killGsm(final GridServiceManager gsm) {
        final CountDownLatch latch = new CountDownLatch(1);
        GridServiceManagerRemovedEventListener eventListener = new GridServiceManagerRemovedEventListener() {
            public void gridServiceManagerRemoved(
                    GridServiceManager gridServiceManager) {
                if (gridServiceManager.equals(gsm)) {
                    latch.countDown();
                }
                
            }};
        admin.getGridServiceManagers().getGridServiceManagerRemoved().add(eventListener);
        try {
            gsm.kill();
            latch.await();
        } catch (InterruptedException e) {
            Assert.fail("Interrupted while killing gsm", e);
        } finally {
            admin.getGridServiceManagers().getGridServiceManagerRemoved().remove(eventListener);
        }
    }
}
