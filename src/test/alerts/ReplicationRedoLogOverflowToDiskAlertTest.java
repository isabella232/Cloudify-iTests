package test.alerts;

import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.openspaces.admin.alert.Alert;
import org.openspaces.admin.alert.AlertManager;
import org.openspaces.admin.alert.AlertSeverity;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.alert.alerts.ReplicationRedoLogOverflowToDiskAlert;
import org.openspaces.admin.alert.config.ReplicationRedoLogOverflowToDiskAlertConfiguration;
import org.openspaces.admin.alert.config.ReplicationRedoLogOverflowToDiskAlertConfigurer;
import org.openspaces.admin.alert.events.AlertTriggeredEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.SpaceMemoryShortageException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AdminUtils;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;


/**
 * test for RedoLog overflow. this test writes object to the space, and then terminates a space backup instance.
 * at witch point objects are replicated to the RedoLog, we set a memory capacity limit, and ensure an alert is
 * triggered once the number of objects in greater than that limit. the redoLog starts writing objects to the disk.
 * we then load a gsc so that a connection to the primary is restored and the RedoLog reduces to zero.
 * the test ensures 2 alerts have triggered, one for the disk overflow, and one for when no overflow occurs.
 *
 * @author elip
 */
public class ReplicationRedoLogOverflowToDiskAlertTest extends AbstractTest {

    Machine machineA, machineB, machineC;
    ProcessingUnit Pu;
    GridServiceManager gsmA;
    Alert resolvedAlert;

    @BeforeMethod
    public void startSetUp() {

        log("waiting for 1 GSA");
        admin.getGridServiceAgents().waitFor(1);

        GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
        GridServiceAgent gsaA = agents[0];

        machineA = gsaA.getMachine();

        log("loading GSM");
        gsmA = loadGSM(machineA);

        log("loading 2 GSC's");
        AdminUtils.loadGSCWithSystemProperty(machineA, "-Xmx512m");
        AdminUtils.loadGSCWithSystemProperty(machineA, "-Xmx512m");

        log("deploying the processing unit");
        Pu = gsmA.deploy(new SpaceDeployment("Test").numberOfInstances(1)
                .numberOfBackups(1).maxInstancesPerVM(1).setContextProperty("cluster-config.groups.group.repl-policy.redo-log-memory-capacity", "10000"));
        ProcessingUnitUtils.waitForDeploymentStatus(Pu, DeploymentStatus.INTACT);

    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void redoLogOverflowToDisk() throws InterruptedException {

        final List<Alert> overflowAlerts = new ArrayList<Alert>();

        final CountDownLatch hiThreshold = new CountDownLatch(1);
        final CountDownLatch lowThreshold = new CountDownLatch(1);

        GigaSpace space = Pu.getSpace().getGigaSpace();

        final AlertManager alertManager = admin.getAlertManager();
        alertManager.setConfig(new ReplicationRedoLogOverflowToDiskAlertConfigurer()
                .create());

        alertManager.enableAlert(ReplicationRedoLogOverflowToDiskAlertConfiguration.class);

        alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
            public void alertTriggered(Alert alert) {
                if (alert.getStatus().equals(AlertStatus.RAISED)) {
                    overflowAlerts.add(alert);
                    hiThreshold.countDown();
                }
                if (alert.getStatus().equals(AlertStatus.RESOLVED)) {
                    resolvedAlert = alert;
                    lowThreshold.countDown();
                }
                LogUtils.log(alert.toString());
            }

        });

        int i = 0;
        while (i < 20500) {
            try {
                if (i == 500) {
                    ProcessingUnitInstance[] processingUnits = Pu.getInstances();
                    for (int j = 0; j < processingUnits.length; j++) {
                        ProcessingUnitInstance puInst = processingUnits[j];
                        if (puInst.getSpaceInstance().getMode().equals(SpaceMode.BACKUP)) {
                            puInst.getGridServiceContainer().kill();
                            break;
                        }
                    }
                }
                space.write(new Message(new Long(i), new byte[1024]));
                i++;
            } catch (SpaceMemoryShortageException e) {
                Assert.fail("Memory Shortage!");
            }
        }

        AdminUtils.loadGSCWithSystemProperty(machineA, "-Xmx512m");

        hiThreshold.await(20, TimeUnit.SECONDS);
        lowThreshold.await(20, TimeUnit.SECONDS);

        Assert.assertNotNull(resolvedAlert);
        Assert.assertTrue(overflowAlerts.size() > 0);

        for (Alert alert : overflowAlerts) {
            Assert.assertEquals(AlertSeverity.WARNING, alert.getSeverity());
            Assert.assertEquals(AlertStatus.RAISED, alert.getStatus());
            Assert.assertTrue(alert instanceof ReplicationRedoLogOverflowToDiskAlert);
            Assert.assertNotNull(((ReplicationRedoLogOverflowToDiskAlert)alert).getVirtualMachineUid());
        }

        Assert.assertEquals(AlertSeverity.WARNING, resolvedAlert.getSeverity());
        Assert.assertEquals(AlertStatus.RESOLVED, resolvedAlert.getStatus());
        Assert.assertTrue(resolvedAlert instanceof ReplicationRedoLogOverflowToDiskAlert);
        Assert.assertNotNull(((ReplicationRedoLogOverflowToDiskAlert)resolvedAlert).getVirtualMachineUid());
    }
}
