package test.examples;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
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

public class SpaceAccessDeploymentTest extends AbstractTest {
	
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

		log("starting: 1 GSM and 4 GSC's on 1 machine");
		GridServiceManager gsmA = loadGSM(machineA); 
		loadGSCs(machineA, 4);		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups = "1")
	public void sessionWebApp() throws InterruptedException {
		
		int numberOfSpaceAccessInstances;
		
		ProcessingUnit space, puWeb, puMySpace;
		
		String buildPath = ScriptUtils.getBuildPath();
		String exampleRoot = buildPath + "/examples/web/space-access";
		String commandOutput = null;
		
		LogUtils.log("building Space Access Web App example...");
		commandOutput = SSHUtils.runCommand(machineA.getHostAddress(), DEFAULT_TEST_TIMEOUT, 
				"cd " + exampleRoot + ";" + buildExample , 
				username, password);
		assertTrue(commandOutput.contains("BUILD SUCCESS"));
		
		LogUtils.log("distribute Space Access Web App example...");
		commandOutput = SSHUtils.runCommand(machineA.getHostAddress(), DEFAULT_TEST_TIMEOUT, 
				"cd " + exampleRoot + ";" + distExample , 
				username, password);
		assertTrue(commandOutput.contains("BUILD SUCCESS"));
		
		LogUtils.log("deploying Space Access Web App example...");
		SSHUtils.runCommand(machineA.getHostAddress(), DEFAULT_TEST_TIMEOUT, 
				"export LOOKUPGROUPS=sgtest" + ";" + "cd " + exampleRoot + ";" + deployExample , 
				username, password);
		
		space = admin.getProcessingUnits().getProcessingUnit("mySpace");
		ProcessingUnitInstance[] processorInst = space.getInstances();
		int backups = 0;
		int primes = 0;
		DeploymentStatus processorStatus = space.getStatus();
		assertTrue(processorStatus.equals(DeploymentStatus.INTACT));
		for (ProcessingUnitInstance puInst : processorInst) {			
			if (puInst.getSpaceInstance().getMode().equals(SpaceMode.BACKUP)) backups++;	
			if (puInst.getSpaceInstance().getMode().equals(SpaceMode.PRIMARY)) primes++;	
		}
		assertTrue(backups == 2 && primes == 2);
		
		numberOfSpaceAccessInstances = admin.getProcessingUnits().getProcessingUnit("SpaceAccess").getInstances().length;
		assertTrue(numberOfSpaceAccessInstances == 3);
		
		// find a container running an SpaceAccess instance
		
		GridServiceContainer containerToBeKilled = null;
		GridServiceContainer[] containers = admin.getGridServiceContainers().getContainers();
		for (GridServiceContainer container : containers) {
			if (container.getProcessingUnitInstances("SpaceAccess").length != 0) {
				containerToBeKilled = container;
				break;
			}
		}
		LogUtils.scanContainerLogsFor(containerToBeKilled, "SEVERE");
		
		// killing one container running an instance if SpaceAccess
		
		LogUtils.log("Killing GSC...");
		containerToBeKilled.kill();
		admin.getGridServiceContainers().waitFor(3);
		
		LogUtils.log("Verfying deployment...");
		
		// verify mySpace deployment remains the same
		
		space = admin.getProcessingUnits().getProcessingUnit("mySpace");
		ProcessingUnitUtils.waitForDeploymentStatus(space, DeploymentStatus.INTACT);
		int second = 0;
		while (second < 30) {
			space = admin.getProcessingUnits().getProcessingUnit("mySpace");
			processorInst = space.getInstances();
			backups = 0;
			primes = 0;
			for (ProcessingUnitInstance puInst : processorInst) {			
				if (puInst.getSpaceInstance().getMode().equals(SpaceMode.BACKUP)) backups++;	
				if (puInst.getSpaceInstance().getMode().equals(SpaceMode.PRIMARY)) primes++;	
			}
			if ((backups == 2) && (primes == 2)) break;
			Thread.sleep(1000);
			second++;
		}
		Assert.assertTrue("processor pu has not returned to original state", second != 30);
			
		// verify there are still 3 instances of SpaceAccess
		
		puWeb = admin.getProcessingUnits().getProcessingUnit("SpaceAccess");
		ProcessingUnitUtils.waitForDeploymentStatus(puWeb, DeploymentStatus.INTACT);
		second = 0;
		while (second < 30) {
			puWeb = admin.getProcessingUnits().getProcessingUnit("SpaceAccess");
			if (puWeb.getInstances().length == 3) break;
			Thread.sleep(1000);
			second++;
		}
		Assert.assertTrue("SpaceAccess pu has not returned to original state", second != 30);
		
		puMySpace = admin.getProcessingUnits().getProcessingUnit("mySpace");
		ProcessingUnitUtils.waitForDeploymentStatus(puMySpace, DeploymentStatus.INTACT);
		
		LogUtils.log("Scanning logs...");
		LogUtils.scanManagerLogsFor(admin.getGridServiceManagers().getManagers()[0], "SEVERE");	
		LogUtils.scanContainerLogsFor(admin.getGridServiceContainers().getContainers()[0], "SEVERE");
		LogUtils.scanContainerLogsFor(admin.getGridServiceContainers().getContainers()[1], "SEVERE");
		LogUtils.scanContainerLogsFor(admin.getGridServiceContainers().getContainers()[2], "SEVERE");	
	}
	
	@AfterMethod
	public void undeployPu() {
		ProcessingUnit httpSession = admin.getProcessingUnits().getProcessingUnit("SpaceAccess");
		if (httpSession != null) {
			httpSession.undeploy();
		}
		ProcessingUnit mySpace = admin.getProcessingUnits().getProcessingUnit("mySpace");
		if (mySpace != null) {
			mySpace.undeploy();
		}
	}



}
