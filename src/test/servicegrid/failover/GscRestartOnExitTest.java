package test.servicegrid.failover;

import static test.utils.AdminUtils.loadGSM;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.hyperic.sigar.SigarException;
import org.junit.Assert;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.DeploymentUtils;
import test.utils.GridServiceContainersCounter;

import com.gigaspaces.grid.gsa.GSProcessRestartOnExit;

public class GscRestartOnExitTest extends AbstractTest {
	private static final long GSC_DISCOVERY_TIMEOUT_MILLISECONDS = 60 * 1000;
	GridServiceAgent gsa;
	GridServiceContainersCounter counter;
	private GridServiceManager gsm;
	
	enum KillMethod { GRACEFUL_KILL, FORCEFUL_KILL};
	
    @Override 
    @BeforeMethod
    public void beforeTest() {
    	super.beforeTest();
		counter = new GridServiceContainersCounter(admin);
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		gsm = loadGSM(gsa.getMachine());
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testAlwaysRestartOnForcefulKill() throws SigarException, IOException {
		Assert.assertTrue(checkIsGscRestarted(GSProcessRestartOnExit.ALWAYS, KillMethod.FORCEFUL_KILL));
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testAlwaysRestartOnGracefulKill() throws SigarException, IOException {
		Assert.assertTrue(checkIsGscRestarted(GSProcessRestartOnExit.ALWAYS, KillMethod.GRACEFUL_KILL));
	}

	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testNeverRestartOnForceKill() throws SigarException, IOException {
		Assert.assertFalse(checkIsGscRestarted(GSProcessRestartOnExit.NEVER, KillMethod.FORCEFUL_KILL));
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testNeverRestartOnGracefulKill() throws SigarException, IOException {
		Assert.assertFalse(checkIsGscRestarted(GSProcessRestartOnExit.NEVER, KillMethod.GRACEFUL_KILL));
	}

	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testOnErrorRestartOnForcefulKill() throws SigarException, IOException {
		Assert.assertTrue(checkIsGscRestarted(GSProcessRestartOnExit.ONERROR, KillMethod.FORCEFUL_KILL));
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testOnErrorRestartOnGracefulKill() throws SigarException, IOException {
		Assert.assertFalse(checkIsGscRestarted(GSProcessRestartOnExit.ONERROR, KillMethod.GRACEFUL_KILL));
	}
	
	boolean checkIsGscRestarted(GSProcessRestartOnExit restartOnExitPolicy, KillMethod killMethod) throws SigarException, IOException {
		
		startContainer(restartOnExitPolicy);
		
		final int removed = counter.getNumberOfGSCsRemoved();
		
		if (killMethod.equals(KillMethod.GRACEFUL_KILL)) {
			// System.exit(0) emulates gracefull shutdown
			injectSystemExitToContainer(0);
		}
		else {
			// System.exit(1) emulates SIGTERM signal
			injectSystemExitToContainer(1);
		}
		waitForAtLeastNumberOfRemovedContainers(removed + 1);
		return admin.getGridServiceContainers().waitFor(1, GSC_DISCOVERY_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
	}

	private void injectSystemExitToContainer(int exitCode) {
		File archive = DeploymentUtils.getArchive("simpleSystemExitPu.jar");
		gsm.deploy(
			new ProcessingUnitDeployment(archive)
			.setContextProperty("test.system-exit", String.valueOf(exitCode)));
	}
	private void waitForAtLeastNumberOfRemovedContainers(final int expectedNumberOfRemovedContainers) {
		// wait until there are no gscs discovered
		repetitiveAssertTrue("Expected container to be undiscovered", new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				return counter.getNumberOfGSCsRemoved() >= expectedNumberOfRemovedContainers;
			}
		}, OPERATION_TIMEOUT);
	}

	private GridServiceContainer startContainer(GSProcessRestartOnExit restartOnExit) {
		return gsa.startGridServiceAndWait(
				new GridServiceContainerOptions()
				.restartOnExit(restartOnExit));
	}
	
	
}
