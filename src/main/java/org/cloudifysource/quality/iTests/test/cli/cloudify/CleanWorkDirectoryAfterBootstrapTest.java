package org.cloudifysource.quality.iTests.test.cli.cloudify;

import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.junit.Ignore;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.gigaspaces.grid.gsa.AgentProcessDetails;

@Ignore
public class CleanWorkDirectoryAfterBootstrapTest extends AbstractLocalCloudTest {

	/***********
	 * Tests that after a teardown and bootstrap, there are only the three expected directories in CLOUDIFY/work/processing-units.
	 * Any old deployed services should be deleted on bootstrap. Tests CLOUDIFY-1942.

	 * IMPORANT: This test kills the GSA and GSC processes using Sigar. It then run bootstrap-localcloud again. 
	 * 
	 * @throws IOException .
	 * @throws InterruptedException .
	 */
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testCleanWorkDirectory() throws IOException, InterruptedException {

		final String buildDir = SGTestHelper.getBuildDir();

		final File workDir = new File(buildDir + "/work/processing-units");
		Assert.assertTrue(workDir.exists(), "work dir does not exist at: " + workDir);
		Assert.assertTrue(workDir.isDirectory(), "work dir is not a directory at: " + workDir);

		
		File[] children = getDirectoriesInWorkDir(workDir);

		Assert.assertTrue(children.length >= 3, "Expected at-least three service directories in work directory");
		if(children.length == 3) {
			// need to install something to verify
			ServiceInstaller installer = new ServiceInstaller(this.restUrl, "empty");
			installer.recipePath(CommandTestUtils.getPath("src/main/resources/apps/USM/usm/empty"));
			installer.install();
		}
		
		boolean gsaLocated = this.admin.getGridServiceAgents().waitFor(1, 1, TimeUnit.MINUTES);
		Assert.assertTrue(gsaLocated, "Could not find GSA of local cloud");
		
		final Set<String> pidsToKill = new HashSet<String>();
		GridServiceAgent gsa = this.admin.getGridServiceAgents().getAgents()[0];
		final long agentPid = gsa.getVirtualMachine().getDetails().getPid();
		pidsToKill.add(Long.toString(agentPid));
		AgentProcessDetails[] processDetails = gsa.getProcessesDetails().getProcessDetails();
		for (AgentProcessDetails agentProcessDetails : processDetails) {
			pidsToKill.add(Long.toString(agentProcessDetails.getProcessId()));
		}
		
		LogUtils.log("About to kill PIDS: " + pidsToKill);
		killProcessesByIDs(pidsToKill);
		children = getDirectoriesInWorkDir(workDir);
		
		Assert.assertTrue(children.length > 3,
				"Expected at-least three directories in work dir, found: " + Arrays.toString(children));
		
		CommandTestUtils.runCloudifyCommandAndWait("bootstrap-localcloud");

		children = getDirectoriesInWorkDir(workDir);
		Assert.assertEquals(children.length, 3,
				"Expected exactly three directories in work directory, got: " + Arrays.toString(children));

	}

	private File[] getDirectoriesInWorkDir(final File workDir) {
		final File[] children = workDir.listFiles(new FileFilter() {

			@Override
			public boolean accept(final File pathname) {
				return pathname.isDirectory();
			}
		});
		return children;
	}

}
