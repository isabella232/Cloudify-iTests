package test.examples;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.core.GigaSpace;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;
import test.utils.SSHUtils;
import test.utils.ScriptUtils;

public class HellowWorldDeploymentTest extends AbstractTest {
	
	private final String username = "tgrid";
	private final String password = "tgrid";
	public static final String buildExample = "sh build.sh build";
	public static final String distExample = "sh build.sh dist";
	public static final String deployExample = "sh build.sh deploy-processor";
	public static final String runFeeder = "sh build.sh run-feeder";
	
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
	public void hellowWorldExampleTest() {
		
		String buildPath = ScriptUtils.getBuildPath();
		String exampleRoot = buildPath + "/examples/helloworld";
		String commandOutput = null;

		LogUtils.log("building Hellow World example...");
		commandOutput = SSHUtils.runCommand(machineA.getHostAddress(), DEFAULT_TEST_TIMEOUT, 
				"cd " + exampleRoot + ";" + buildExample , 
				username, password);
		assertTrue(commandOutput.contains("BUILD SUCCESS"));
		
		LogUtils.log("distributing Hellow World example...");
		commandOutput = SSHUtils.runCommand(machineA.getHostAddress(), DEFAULT_TEST_TIMEOUT, 
				"cd " + exampleRoot + ";" + distExample , 
				username, password);
		assertTrue(commandOutput.contains("BUILD SUCCESS"));
		
		LogUtils.log("deploying processor...");
		commandOutput =  SSHUtils.runCommand(machineA.getHostAddress(), DEFAULT_TEST_TIMEOUT, 
				"export LOOKUPGROUPS=sgtest" + ";" + "cd " + exampleRoot + ";" + deployExample , 
				username, password);
		assertTrue(commandOutput.contains("BUILD SUCCESS"));
		
		LogUtils.log("Running feeder...");
		commandOutput = SSHUtils.runCommand(machineA.getHostAddress(), DEFAULT_TEST_TIMEOUT, 
				"export LOOKUPGROUPS=sgtest" + ";" + "cd " + exampleRoot + ";" + runFeeder , 
				username, password);	
		assertTrue(commandOutput.contains("Feeder WROTE 1000 messages"));
		assertTrue(commandOutput.contains("There are 1000 Message objects in the space now"));
		
		ProcessingUnit processor = admin.getProcessingUnits().getProcessingUnit("hello-processor");
		ProcessingUnitUtils.waitForDeploymentStatus(processor, DeploymentStatus.INTACT);
		
		GigaSpace gigaSpace = processor.waitForSpace().getGigaSpace();
		assertTrue(gigaSpace.count(null) == 1000);
		
		LogUtils.log("Scanning logs...");
		LogUtils.scanManagerLogsFor(admin.getGridServiceManagers().getManagers()[0], "SEVERE");
		LogUtils.scanContainerLogsFor(admin.getGridServiceContainers().getContainers()[0], "SEVERE");
		LogUtils.scanContainerLogsFor(admin.getGridServiceContainers().getContainers()[1], "SEVERE");
		
	}
	
	

}
