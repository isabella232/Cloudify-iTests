package test.alerts;

import junit.framework.Assert;
import org.openspaces.admin.alert.AlertManager;
import org.openspaces.admin.alert.config.AlertConfiguration;
import org.openspaces.admin.alert.config.CpuUtilizationAlertConfiguration;
import org.openspaces.admin.alert.config.GarbageCollectionAlertConfiguration;
import org.openspaces.admin.alert.config.HeapMemoryUtilizationAlertConfiguration;
import org.openspaces.admin.alert.config.PhysicalMemoryUtilizationAlertConfiguration;
import org.openspaces.admin.alert.config.ReplicationChannelDisconnectedAlertConfiguration;
import org.openspaces.admin.alert.config.ReplicationRedoLogOverflowToDiskAlertConfiguration;
import org.openspaces.admin.alert.config.ReplicationRedoLogSizeAlertConfiguration;
import org.openspaces.admin.alert.config.parser.XmlAlertConfigurationParser;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.ScriptUtils;

/**
 * this test checks the alerts configuration taken from the alert.xml configuration file.
 * it asserts that all default alert values are as they should be
 * @author elip
 *
 */
public class AlertXmlConfigurationTest extends AbstractTest {
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups = "1")
	public void alertXmlConfiguration() throws InterruptedException {
		
		String buildPath = ScriptUtils.getBuildPath();
		String alertXmlFilePath = buildPath + "/config/alerts/alerts.xml";
		AlertConfiguration[] configurations = new XmlAlertConfigurationParser(alertXmlFilePath).parse();
		AlertManager alertManager = admin.getAlertManager();
		alertManager.configure(configurations);
		
		CpuUtilizationAlertConfiguration cpuUtilizationAlertConfiguration = alertManager.getConfig(CpuUtilizationAlertConfiguration.class);
		Assert.assertTrue(cpuUtilizationAlertConfiguration.isEnabled());
		Assert.assertTrue(cpuUtilizationAlertConfiguration.getHighThresholdPerc() == 80);
		Assert.assertTrue(cpuUtilizationAlertConfiguration.getLowThresholdPerc() == 60);
		Assert.assertTrue(cpuUtilizationAlertConfiguration.getMeasurementPeriod() == 60000);
		
		PhysicalMemoryUtilizationAlertConfiguration physicalMemoryUtilizationAlertConfiguration = alertManager.getConfig(PhysicalMemoryUtilizationAlertConfiguration.class);
		Assert.assertTrue(physicalMemoryUtilizationAlertConfiguration.isEnabled());
		Assert.assertTrue(physicalMemoryUtilizationAlertConfiguration.getHighThresholdPerc() == 80);
		Assert.assertTrue(physicalMemoryUtilizationAlertConfiguration.getLowThresholdPerc() == 60);
		Assert.assertTrue(physicalMemoryUtilizationAlertConfiguration.getMeasurementPeriod() == 60000);
		
		HeapMemoryUtilizationAlertConfiguration heapMemoryUtilizationAlertConfiguration = alertManager.getConfig(HeapMemoryUtilizationAlertConfiguration.class);
		Assert.assertTrue(heapMemoryUtilizationAlertConfiguration.isEnabled());
		Assert.assertTrue(heapMemoryUtilizationAlertConfiguration.getHighThresholdPerc() == 80);
		Assert.assertTrue(heapMemoryUtilizationAlertConfiguration.getLowThresholdPerc() == 60);
		Assert.assertTrue(heapMemoryUtilizationAlertConfiguration.getMeasurementPeriod() == 60000);
		
		GarbageCollectionAlertConfiguration garbageCollectionAlertConfiguration = alertManager.getConfig(GarbageCollectionAlertConfiguration.class);
		Assert.assertTrue(garbageCollectionAlertConfiguration.isEnabled());
		Assert.assertTrue(garbageCollectionAlertConfiguration.getLongGcPausePeriod() == 10000);
		Assert.assertTrue(garbageCollectionAlertConfiguration.getShortGcPausePeriod() == 1000);
		
		ReplicationChannelDisconnectedAlertConfiguration replicationChannelDisconnectedAlertConfiguration = alertManager.getConfig(ReplicationChannelDisconnectedAlertConfiguration.class);
		Assert.assertTrue(replicationChannelDisconnectedAlertConfiguration.isEnabled());
		
		ReplicationRedoLogOverflowToDiskAlertConfiguration replicationRedoLogOverflowToDiskAlertConfiguration = alertManager.getConfig(ReplicationRedoLogOverflowToDiskAlertConfiguration.class);
		Assert.assertTrue(replicationRedoLogOverflowToDiskAlertConfiguration.isEnabled());
		
		ReplicationRedoLogSizeAlertConfiguration replicationRedoLogSizeAlertConfiguration = alertManager.getConfig(ReplicationRedoLogSizeAlertConfiguration.class);
		Assert.assertTrue(replicationRedoLogSizeAlertConfiguration.isEnabled());
		Assert.assertTrue(replicationRedoLogSizeAlertConfiguration.getHighThresholdRedoLogSize() == 100000);
		Assert.assertTrue(replicationRedoLogSizeAlertConfiguration.getLowThresholdRedoLogSize() == 1000);
	}

}
