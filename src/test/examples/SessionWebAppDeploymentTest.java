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

public class SessionWebAppDeploymentTest extends AbstractTest { 
	
	private final String username = "tgrid";
	private final String password = "tgrid";
	public static final String buildExample = "sh build.sh build";
	public static final String distExample = "sh build.sh dist";
	public static final String deployExample = "sh build.sh deploy";
	
	Machine machineA, machineB;
	
	@BeforeMethod
	public void startSetup() {
		
		log("waiting for 2 machines");
		admin.getMachines().waitFor(2);

		log("waiting for 2 GSA's");
		admin.getGridServiceAgents().waitFor(2);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];
		GridServiceAgent gsaB = agents[1];

		machineA = gsaA.getMachine();
		machineB = gsaB.getMachine();

		log("starting: 1 GSM and 2 GSC's on first machine");
		GridServiceManager gsmA = loadGSM(machineA); 
		loadGSCs(machineA, 2);
		log("starting: 2 GSC's on second machine");
		loadGSCs(machineB, 2);
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups = "2")
	public void sessionWebApp() throws InterruptedException {
		
		int numberOfHttpInstances;
		
		ProcessingUnit space, puWeb;
		
		String buildPath = ScriptUtils.getBuildPath();
		String exampleRoot = buildPath + "/examples/web/session";
		String commandOutput = null;
		
		LogUtils.log("building Session Web App example...");
		commandOutput = SSHUtils.runCommand(machineA.getHostAddress(), DEFAULT_TEST_TIMEOUT, 
				"cd " + exampleRoot + ";" + buildExample , 
				username, password);
		assertTrue(commandOutput.contains("BUILD SUCCESS"));
		
		LogUtils.log("distribute Session Web App example...");
		commandOutput = SSHUtils.runCommand(machineA.getHostAddress(), DEFAULT_TEST_TIMEOUT, 
				"cd " + exampleRoot + ";" + distExample , 
				username, password);
		assertTrue(commandOutput.contains("BUILD SUCCESS"));
		
		LogUtils.log("deploying Session Web App example...");
		SSHUtils.runCommand(machineA.getHostAddress(), DEFAULT_TEST_TIMEOUT, 
				"export LOOKUPGROUPS=sgtest" + ";" + "cd " + exampleRoot + ";" + deployExample , 
				username, password);	
		
		space = admin.getProcessingUnits().getProcessingUnit("mySpace");
		ProcessingUnitUtils.waitForDeploymentStatus(space, DeploymentStatus.INTACT);
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
		
		puWeb = admin.getProcessingUnits().getProcessingUnit("HttpSession");
		ProcessingUnitUtils.waitForDeploymentStatus(puWeb, DeploymentStatus.INTACT);
		numberOfHttpInstances = puWeb.getInstances().length;
		assertTrue(numberOfHttpInstances == 3);
		
		// find a container running an httpSession instance
		
		GridServiceContainer containerToBeKilled = null;
		GridServiceContainer[] containers = admin.getGridServiceContainers().getContainers();
		for (GridServiceContainer container : containers) {
			if (container.getProcessingUnitInstances("HttpSession").length != 0) {
				containerToBeKilled = container;
				break;
			}
		}
		LogUtils.scanContainerLogsFor(containerToBeKilled, "SEVERE");
		
		// killing one container running an instance if HttpSession
		
		LogUtils.log("Killing GSC...");
		containerToBeKilled.kill();
		admin.getGridServiceContainers().waitFor(3);
		
		
		LogUtils.log("Verifying deployment...");
		
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
		
		// verify there are still 3 instances of HttpSession
		
		puWeb = admin.getProcessingUnits().getProcessingUnit("HttpSession");
		ProcessingUnitUtils.waitForDeploymentStatus(puWeb, DeploymentStatus.INTACT);
		second = 0;
		while (second < 30) {
			puWeb = admin.getProcessingUnits().getProcessingUnit("HttpSession");
			if (puWeb.getInstances().length == 3) break;
			Thread.sleep(1000);
			second++;
		}
		Assert.assertTrue("HttpSession pu has not returned to original state", second != 30);
		
		LogUtils.log("Scanning logs...");
		LogUtils.scanManagerLogsFor(admin.getGridServiceManagers().getManagers()[0], "SEVERE");	
		LogUtils.scanContainerLogsFor(admin.getGridServiceContainers().getContainers()[0], "SEVERE");
		LogUtils.scanContainerLogsFor(admin.getGridServiceContainers().getContainers()[1], "SEVERE");
		LogUtils.scanContainerLogsFor(admin.getGridServiceContainers().getContainers()[2], "SEVERE");
	}
	
	@AfterMethod
	public void undeployPu() {
		ProcessingUnit httpSession = admin.getProcessingUnits().getProcessingUnit("HttpSession");
		if (httpSession != null) {
			httpSession.undeploy();
		}
		ProcessingUnit mySpace = admin.getProcessingUnits().getProcessingUnit("mySpace");
		if (mySpace != null) {
			mySpace.undeploy();
		}
	}

}
