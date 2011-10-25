package test.maven;

import static test.utils.LogUtils.log;
import static test.utils.ScriptUtils.getBuildPath;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.AdminUtils.loadGSCs;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
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

import test.AbstractTest;
import test.utils.MavenUtils;
import test.utils.SSHUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;
import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogEntryMatcher;
import com.gigaspaces.log.LogEntryMatchers;

public class MavenTemplatesTest extends AbstractTest {

	private static final int DEFAULT_RUN_TIMEOUT = 15000;
	private static final int MULE_TIMEOUT = 60000;
	private Machine machineA;
	private boolean muleJarsImported = false;
	private boolean mavenRepInstalled = false;

	@BeforeMethod
	public void startSetUp() throws Exception {	


		log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);


		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		machineA = gsaA.getMachine();

		if(!muleJarsImported) muleJarsImported = MavenUtils.importMuleJars(machineA);

		log("starting: 1 GSM and 2 GSC's on 1 machine");
		loadGSM(machineA); 
		loadGSCs(machineA, 2);

		if (!mavenRepInstalled) mavenRepInstalled = MavenUtils.installMavenRep(machineA);

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled=false)
	public void mavenTemplateRunBasic() throws Exception{
		mavenTemplateRun("basic");
	}
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled=true)
	public void mavenTemplateRunBasicXml() throws Exception{
		mavenTemplateRun("basic-xml");
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled=true)
	public void mavenTemplateRunAsyncPersistency() throws Exception{
		mavenTemplateRun("basic-async-persistency");
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled=true)
	public void mavenTemplateRunAsyncPersistencyXml() throws Exception{
		mavenTemplateRun("basic-async-persistency-xml");
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled=true)
	public void mavenTemplateRunMule() throws Exception{
		mavenTemplateRun("mule");
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled=true)
	public void mavenTemplateRunStandaloneBasic() throws Exception{
		mavenTemplateDeployOrStandalone("basic", true);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled=true)
	public void mavenTemplateStandaloneBasicXml() throws Exception{
		mavenTemplateDeployOrStandalone("basic-xml", true);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled=true)
	public void mavenTemplateStandaloneAsyncPersistency() throws Exception{
		mavenTemplateDeployOrStandalone("basic-async-persistency", true);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled=true)
	public void mavenTemplateStandaloneAsyncPersistencyXml() throws Exception{
		mavenTemplateDeployOrStandalone("basic-async-persistency-xml", true);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled=true)
	public void mavenTemplateStandaloneMule() throws Exception{
		mavenTemplateDeployOrStandalone("mule", true);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled=true)
	public void mavenTemplateDeployBasic() throws Exception{
		mavenTemplateDeployOrStandalone("basic", false);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled=true)
	public void mavenTemplateDeployBasicXml() throws Exception{
		mavenTemplateDeployOrStandalone("basic-xml", false);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled=true)
	public void mavenTemplateDeployAsyncPersistency() throws Exception{
		mavenTemplateDeployOrStandalone("basic-async-persistency", false);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled=true)
	public void mavenTemplateDeployAsyncPersistencyXml() throws Exception{
		mavenTemplateDeployOrStandalone("basic-async-persistency-xml", false);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled=true)
	public void mavenTemplateDeployMule() throws Exception{
		mavenTemplateDeployOrStandalone("mule", false);
	}




	public void mavenTemplateDeployOrStandalone(String template, boolean standalone) throws Exception {
		String commandOutput, exp;
		String host = machineA.getHostAddress(); 
		final String buildPath = getBuildPath();

		log("Creating the application...");
		SSHUtils.runCommand(host, AbstractTest.DEFAULT_TEST_TIMEOUT, 
				"cd " + buildPath+"/.." + ";" + MavenUtils.mavenCreate + template, MavenUtils.username , MavenUtils.password);
		System.out.println("Apllication created at location: " + buildPath+"/.." );
		assertTrue(MavenUtils.isAppExists(machineA.getHostName()));	

		log("Packaging...");	
		commandOutput = SSHUtils.runCommand(host, AbstractTest.DEFAULT_TEST_TIMEOUT, 
				"cd " + buildPath +  "/../my-app;" + MavenUtils.mavenPackage, MavenUtils.username , MavenUtils.password);

		assertBuilt(template, commandOutput);

		if(standalone){
			long timeout = 15000;
			if(template.startsWith("mule"))
				timeout = 30000;
			checkRun(MavenUtils.mavenRunStandalone, host, buildPath, timeout);
		}
		else{
			log("Deploying...");
			commandOutput = SSHUtils.runCommand(host, AbstractTest.DEFAULT_TEST_TIMEOUT, 
					"cd " + buildPath +  "/../my-app;" + MavenUtils.mavenDeploy + " -Dgroups="+admin.getGroups()[0], MavenUtils.username , MavenUtils.password);
			exp = "my-app ............................................ SUCCESS";
			assertTrue(commandOutput.contains(exp));
			assertTrue(commandOutput.contains("BUILD SUCCESS"));


			//now we test that the processing unit is really deployed
			Thread.sleep(10000);
			GridServiceContainer container1 = admin.getGridServiceContainers().getContainers()[0];
			GridServiceContainer container2 = admin.getGridServiceContainers().getContainers()[1];
			GridServiceManager gsm = admin.getGridServiceManagers().getManagers()[0];

			LogEntryMatcher matcher = LogEntryMatchers.containsString(" SEVERE ");

			LogEntries logEntriesContainer1 = container1.logEntries(matcher);
			if (logEntriesContainer1.getEntries().size() > 1) {
				for (LogEntry logEntry : logEntriesContainer1.logEntries()) {
					System.out.println(logEntry.getText());		
				}
				Assert.fail("Severe Errors during Deployment");
			}	

			LogEntries logEntriesContainer2 = container2.logEntries(matcher);
			if (logEntriesContainer2.getEntries().size() > 1) {
				for (LogEntry logEntry : logEntriesContainer2.logEntries()) {
					System.out.println(logEntry.getText());		
				}
				Assert.fail("Severe Errors during Deployment");
			}

			LogEntries logEntriesGsm = gsm.logEntries(matcher);
			if (logEntriesGsm.getEntries().size() > 1) {
				for (LogEntry logEntry : logEntriesGsm.logEntries()) {
					System.out.println(logEntry.getText());		
				}
				Assert.fail("Severe Errors during Deployment");
			}

			ProcessingUnit processor = admin.getProcessingUnits().getProcessingUnit("my-app-processor");
			ProcessingUnitInstance[] processorInst = processor.getInstances();
			int Backups = 0;
			int Primes = 0;
			DeploymentStatus processorStatus = processor.getStatus();
			assertTrue(processorStatus.equals(DeploymentStatus.INTACT));
			for (ProcessingUnitInstance puInst : processorInst) {			
				if (puInst.getSpaceInstance().getMode().equals(SpaceMode.BACKUP)) Backups++;	
				if (puInst.getSpaceInstance().getMode().equals(SpaceMode.PRIMARY)) Primes++;	
			}
			assertTrue((Backups == 2) && (Primes == 2));
			ProcessingUnit feeder = admin.getProcessingUnits().getProcessingUnit("my-app-feeder");
			DeploymentStatus feederStatus = feeder.getStatus();
			assertTrue(feederStatus.equals(DeploymentStatus.INTACT));

			if(template.startsWith("basic-async-persistency")){
				ProcessingUnit mirror = admin.getProcessingUnits().getProcessingUnit("my-app-mirror");
				DeploymentStatus mirrorStatus = mirror.getStatus();
				assertTrue(mirrorStatus.equals(DeploymentStatus.INTACT));
			}
		}
	}

	@AfterMethod
	public void finishTemplate() {
		log("Deleting the current app...");
		MavenUtils.deleteApp(machineA);
	}

//	@AfterTest
	public void finishTest() {
		log("Removing maven repository...");
		MavenUtils.deleteMavenRep(machineA);
	}

	private void assertBasicBuilt(String commandOutput) {
		String exp;
		exp = "my-app ............................................ SUCCESS";
		assertTrue(commandOutput.contains(exp));
		exp = "common ............................................ SUCCESS";
		assertTrue(commandOutput.contains(exp));
		exp = "processor ......................................... SUCCESS";
		assertTrue(commandOutput.contains(exp));
		exp = "feeder ............................................ SUCCESS";
		assertTrue(commandOutput.contains(exp));
		assertTrue(commandOutput.contains("BUILD SUCCESS"));
	}

	private void assertAsyncPersistencyBuilt(String commandOutput) {
		String exp;
		assertBasicBuilt(commandOutput);
		exp = "mirror ............................................ SUCCESS";
		assertTrue(commandOutput.contains(exp));
	}


	public void mavenTemplateRun(String template) throws Exception {
		String output;
		final String host = machineA.getHostAddress();
		final String buildPath = getBuildPath();
		log("Creating the application...");
		SSHUtils.runCommand(host, AbstractTest.DEFAULT_TEST_TIMEOUT, 
				"cd " + buildPath+"/.." + ";" + MavenUtils.mavenCreate + template, MavenUtils.username , MavenUtils.password);
		System.out.println("Apllication created at location: " + buildPath+"/.." );
		assertTrue(MavenUtils.isAppExists(machineA.getHostName()));	

		log("Compiling...");	
		output = SSHUtils.runCommand(host, AbstractTest.DEFAULT_TEST_TIMEOUT, 
				"cd " + buildPath +  "/../my-app;" + MavenUtils.mavenCompile, MavenUtils.username , MavenUtils.password);
		assertBuilt(template, output);

		long timeout = DEFAULT_RUN_TIMEOUT;
		if(template.startsWith("mule"))
			timeout = MULE_TIMEOUT;
		checkRun(MavenUtils.mavenRun, host, getBuildPath(), timeout);

	}

	private void assertBuilt(String template, String output) {
		if(template.startsWith("basic-async-persistency"))
			assertAsyncPersistencyBuilt(output);
		else 
			assertBasicBuilt(output);
	}

	private void checkRun(final String command, final String host, final String buildPath, long timout)
	throws InterruptedException {
		String output;
		output = getRunnigJavaPIDs(host);
		List<String> pids = Arrays.asList(output.split("\n"));

		final StringBuilder sb = new StringBuilder();

		log("Run... " + command);
		new Thread(new Runnable() {

			@Override
			public void run() {
				sb.append(SSHUtils.runCommand(host, AbstractTest.DEFAULT_TEST_TIMEOUT, 
						"cd " + buildPath +  "/../my-app;" + command + " -Dgroups="+admin.getGroups()[0] +
						"&", MavenUtils.username , MavenUtils.password));				
			}
		}).start();

		Thread.sleep(timout);				

		killAddedJavaProcess(host, pids);

		Assert.assertTrue(sb.toString().contains("--- FEEDER WROTE"));
		Assert.assertTrue(sb.toString().contains("------ PROCESSED"));
	}

	private void killAddedJavaProcess(final String host, List<String> pids) {
		String output;
		output = getRunnigJavaPIDs(host);
		List<String> newPids = Arrays.asList(output.split("\n"));
		for (String pid : newPids.subList(0, newPids.size())) {
			if(!pids.subList(0, pids.size()).contains(pid))
				SSHUtils.runCommand(host, AbstractTest.DEFAULT_TEST_TIMEOUT, 
						"kill -9 " + pid, MavenUtils.username , MavenUtils.password);
		}
	}

	private String getRunnigJavaPIDs(final String host) {
		String output;
		output = SSHUtils.runCommand(host, AbstractTest.DEFAULT_TEST_TIMEOUT, 
				"ps ux | awk '/java/ && !/awk/ {print $2}'", MavenUtils.username , MavenUtils.password);
		return output;
	}
}
