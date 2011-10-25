package test.examples;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.AdminUtils.loadGSC;
import static test.utils.LogUtils.log;
import junit.framework.Assert;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.cluster.activeelection.SpaceMode;

import test.AbstractTest;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;
import test.utils.SSHUtils;
import test.utils.ScriptUtils;

public class DataDeploymentTest extends AbstractTest {
	
	private final String username = "tgrid";
	private final String password = "tgrid";
	public static final String buildExample = "sh build.sh build";
	public static final String distExample = "sh build.sh dist";
	public static final String deployExample = "sh build.sh deploy";
	
	Machine machineA;
	
	@BeforeMethod
	public void startSetup() {
		
		log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		machineA = gsaA.getMachine();

		log("starting: 1 GSM and 2 GSC's on 1 machine");
		GridServiceManager gsmA = loadGSM(machineA); 
		loadGSCs(machineA, 2);
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups = "1")
	public void dataExampleTest() throws InterruptedException {
		
		String buildPath = ScriptUtils.getBuildPath();
		String exampleRoot = buildPath + "/examples/data";
		String commandOutput = null;
		
		LogUtils.log("building data example...");
		commandOutput = SSHUtils.runCommand(machineA.getHostAddress(), DEFAULT_TEST_TIMEOUT, 
				"cd " + exampleRoot + ";" + buildExample , 
				username, password);
		assertTrue(commandOutput.contains("BUILD SUCCESS"));
		
		LogUtils.log("distributing data example...");
		commandOutput = SSHUtils.runCommand(machineA.getHostAddress(), DEFAULT_TEST_TIMEOUT, 
				"cd " + exampleRoot + ";" + distExample , 
				username, password);
		assertTrue(commandOutput.contains("BUILD SUCCESS"));
		
		LogUtils.log("deploying data example...");
		SSHUtils.runCommand(machineA.getHostAddress(), DEFAULT_TEST_TIMEOUT, 
				"export LOOKUPGROUPS=sgtest" + ";" + "cd " + exampleRoot + ";" + deployExample , 
				username, password);
		
		Thread.sleep(10000);
		
		ProcessingUnit processor = admin.getProcessingUnits().getProcessingUnit("processor");
		ProcessingUnitUtils.waitForDeploymentStatus(processor, DeploymentStatus.INTACT);
		ProcessingUnitInstance[] processorInst = processor.getInstances();
		int Backups = 0;
		int Primes = 0;
		for (ProcessingUnitInstance puInst : processorInst) {			
			if (puInst.getSpaceInstance().getMode().equals(SpaceMode.BACKUP)) Backups++;	
			if (puInst.getSpaceInstance().getMode().equals(SpaceMode.PRIMARY)) Primes++;	
		}
		assertTrue((Backups == 2) && (Primes == 2));
		ProcessingUnit feeder = admin.getProcessingUnits().getProcessingUnit("feeder");
		ProcessingUnitUtils.waitForDeploymentStatus(feeder, DeploymentStatus.INTACT);
		
		// now we start a new gsc and play with it
		
		LogUtils.log("Loading a new GSC..");
		GridServiceContainer containerToRelocateTo = loadGSC(machineA);
		
		ProcessingUnitInstance puProcessorInstToRelocate = admin.getProcessingUnits().getProcessingUnit("processor").getInstances()[0];
		ProcessingUnitInstance puFeederInst = admin.getProcessingUnits().getProcessingUnit("feeder").getInstances()[0]; 
		
		LogUtils.log("Relocating pu instance...");
		puProcessorInstToRelocate.relocateAndWait(containerToRelocateTo);
		puFeederInst.relocateAndWait(containerToRelocateTo);
		
		// verify deployment status remains the same
		LogUtils.log("Verifying deployment...");
		
		feeder = admin.getProcessingUnits().getProcessingUnit("feeder");
		ProcessingUnitUtils.waitForDeploymentStatus(feeder, DeploymentStatus.INTACT);
		assertTrue(feeder.getInstances().length == 1);
		
		processor = admin.getProcessingUnits().getProcessingUnit("processor");
		ProcessingUnitUtils.waitForDeploymentStatus(processor, DeploymentStatus.INTACT);
		int second = 0;
		while (second < 30) {
			processor = admin.getProcessingUnits().getProcessingUnit("processor");
			processorInst = processor.getInstances();
			Backups = 0;
			Primes = 0;
			for (ProcessingUnitInstance puInst : processorInst) {			
				if (puInst.getSpaceInstance().getMode().equals(SpaceMode.BACKUP)) Backups++;	
				if (puInst.getSpaceInstance().getMode().equals(SpaceMode.PRIMARY)) Primes++;	
			}
			if ((Backups == 2) && (Primes == 2)) break;
			Thread.sleep(1000);
			second++;
		}
		Assert.assertTrue("processor pu has not returned to original state", second != 30);
		LogUtils.scanContainerLogsFor(containerToRelocateTo, "SEVERE");
		
		// kill a gsc running a certain processing unit instance and the feeder
		
		LogUtils.log("Killing a GSC...");
		containerToRelocateTo.kill();
		admin.getGridServiceContainers().waitFor(2);
		feeder.waitFor(1);
		processor.waitFor(4);
		
		// verify deployment status remains the same
		LogUtils.log("Verifying deployment...");
		
		feeder = admin.getProcessingUnits().getProcessingUnit("feeder");
		ProcessingUnitUtils.waitForDeploymentStatus(feeder, DeploymentStatus.INTACT);
		assertTrue(feeder.getInstances().length == 1);
		
		processor = admin.getProcessingUnits().getProcessingUnit("processor");
		ProcessingUnitUtils.waitForDeploymentStatus(processor, DeploymentStatus.INTACT);
		second = 0;
		while (second < 30) {
			processor = admin.getProcessingUnits().getProcessingUnit("processor");
			processorInst = processor.getInstances();
			Backups = 0;
			Primes = 0;
			for (ProcessingUnitInstance puInst : processorInst) {			
				if (puInst.getSpaceInstance().getMode().equals(SpaceMode.BACKUP)) Backups++;	
				if (puInst.getSpaceInstance().getMode().equals(SpaceMode.PRIMARY)) Primes++;	
			}
			if ((Backups == 2) && (Primes == 2)) break;
			Thread.sleep(1000);
			second++;
		}
		Assert.assertTrue("processor pu has not returned to original state", second != 30);
		
		// scan the logs for SEVERE errors
		
		LogUtils.log("Scanning logs...");
		LogUtils.scanManagerLogsFor(admin.getGridServiceManagers().getManagers()[0], "SEVERE");
		LogUtils.scanContainerLogsFor(admin.getGridServiceContainers().getContainers()[0], "SEVERE");
		LogUtils.scanContainerLogsFor(admin.getGridServiceContainers().getContainers()[1], "SEVERE");	
	}
	
	@AfterMethod
	public void undeployPu() {
		ProcessingUnit feeder = admin.getProcessingUnits().getProcessingUnit("feeder");
		if (feeder != null) {
			feeder.undeploy();
		}
		
		ProcessingUnit processor = admin.getProcessingUnits().getProcessingUnit("processor");
		if (processor != null) {
			processor.undeploy();
		}
	}

}

