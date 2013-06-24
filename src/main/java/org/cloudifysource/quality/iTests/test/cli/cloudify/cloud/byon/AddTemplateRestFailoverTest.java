package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import iTests.framework.utils.LogUtils;
import iTests.framework.utils.SSHUtils;
import iTests.framework.utils.WebUtils;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.apache.commons.lang.time.DateUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.admin.vm.VirtualMachine;
import org.openspaces.admin.vm.VirtualMachineDetails;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AddTemplateRestFailoverTest extends AbstractByonAddRemoveTemplatesTest {

	private static final long TIMEOUT_SECONDS = 120;
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {		
		super.bootstrap();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	@Override
	public int getNumOfMngMachines() {
		return 1;
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void addTemplateRestFailoverTest() {
		addTemplate();
		ProcessingUnit restPU = getRestPU();
		long restPid = getRestPid(restPU);
		killRest(restPid);
		waitForConnectionRefused(TIMEOUT_SECONDS * DateUtils.MILLIS_PER_SECOND);
		waitForConnectionAccepted(TIMEOUT_SECONDS * DateUtils.MILLIS_PER_SECOND);
		boolean waitFor = restPU.waitFor(1, TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assert.assertTrue(waitFor);
	}

	private void waitForConnectionRefused(final long timeoutMilliseconds) {
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				try {
					return WebUtils.isURLAvailable(new URL(getRestUrl()));
				} catch (final Exception e) {
					throw new IllegalStateException(e);
				}
			}
		};
		
		AssertUtils.repetitiveAssertFalse("Rest connection is still available after process death", condition, timeoutMilliseconds);
		LogUtils.log("Rest connection is not available.");
	}

	
	private void waitForConnectionAccepted(long timeoutMilliseconds) {
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				try {
					return WebUtils.isURLAvailable(new URL(getRestUrl()));
				} catch (final Exception e) {
					throw new IllegalStateException(e);
				}
			}
		};
		
		AssertUtils.repetitiveAssertTrue("Rest connection is not available", condition, timeoutMilliseconds);	
		LogUtils.log("Rest connection is available.");
	}
	
	protected void addTemplate() {
		TemplatesBatchHandler templatesHandler = new TemplatesBatchHandler();
		try {
			templatesHandler.addTemplates(1);
			String command = "connect " + getRestUrl() + ";add-templates " + templatesHandler.getTemplatesFolder();
			String output = CommandTestUtils.runCommandAndWait(command);
			Assert.assertTrue(output.contains("Templates added successfully"));
		} catch (Exception e) {
			LogUtils.log(e.getMessage(), e);
			Assert.fail(e.getMessage());
		}
		assertExpectedListTemplates();
	}
	
	private void killRest(long pid) {
		Machine mngMachine = getManagementMachines().get(0);
		SSHUtils.runCommand(
				mngMachine.getHostAddress(), 
				AbstractTestSupport.OPERATION_TIMEOUT/3, 
				"kill -9 " + pid, USER, PASSWORD);
		LogUtils.log("killed REST process " + pid);
	}
	
	private ProcessingUnit getRestPU() {
		ProcessingUnits pus = this.admin.getProcessingUnits();
		Assert.assertNotNull(pus);
		final ProcessingUnit restPU = pus.waitFor("rest", TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assert.assertNotNull("rest processing unit was not found.", restPU);
		return restPU;
	}
	
	private long getRestPid(ProcessingUnit pu) {
		boolean waitFor = pu.waitFor(1);
		Assert.assertTrue(waitFor);
		ProcessingUnitInstance[] instances = pu.getInstances();
		Assert.assertTrue(instances.length > 0);
		ProcessingUnitInstance puInstance = instances[0];
		Assert.assertNotNull(puInstance);
		GridServiceContainer gsc = puInstance.getGridServiceContainer();
		Assert.assertNotNull(gsc);
		VirtualMachine vm = gsc.getVirtualMachine();
		Assert.assertNotNull(vm);
		VirtualMachineDetails details = vm.getDetails();
		Assert.assertNotNull(details);
		long pid = details.getPid();
		Assert.assertNotNull(pid);
		return pid;
	}
	
}
