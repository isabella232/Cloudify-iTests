package test.cli.cloudify;

import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceLifecycleEventListener;
import org.openspaces.pu.service.CustomServiceMonitors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;

import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.SSHUtils;
import framework.utils.ScriptUtils;


public class InternalUSMPuServiceDownTest extends AbstractTest {
	
	ProcessingUnit tomcat;
	Long tomcatPId;
	Machine machineA;
	WebClient client;
	Machine[] machines;

	@Override
	@BeforeMethod
	public void beforeTest() {
		super.beforeTest();	
		machines = admin.getMachines().getMachines();
		admin.close();
		client = new WebClient(BrowserVersion.getDefault());
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = true)
	public void tomcatServiceDownAndCorruptedTest() throws IOException, InterruptedException, PackagingException {
		
		String serviceDir = ScriptUtils.getBuildPath() + "/recipes/tomcat";
		String command = "bootstrap-localcloud ; install-service " + "--verbose -timeout 10 " + serviceDir;
		try {
			LogUtils.log("installing tomcat service using Cli");
			CommandTestUtils.runCommandAndWait(command);
			AdminFactory factory = new AdminFactory();
			for (Machine machine : machines) {
				LogUtils.log("adding locator to admin : " + machine.getHostName() + ":4168");
				factory.addLocator(machine.getHostAddress() + ":4168");
			}
			LogUtils.log("adding localhost locator to admin");
			factory.addLocator("127.0.0.1:4168");
			LogUtils.log("creating new admin");
			admin = factory.createAdmin();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		LogUtils.log("Retrieving tomcat process pid from admin");
		tomcat = admin.getProcessingUnits().waitFor("tomcat", 10, TimeUnit.SECONDS);
		assertNotNull(tomcat);
		ProcessingUnitUtils.waitForDeploymentStatus(tomcat, DeploymentStatus.INTACT);
		assertTrue(tomcat.getStatus().equals(DeploymentStatus.INTACT));
		
		ProcessingUnitInstance tomcatInstance = tomcat.getInstances()[0];	
		CustomServiceMonitors customServiceDetails = (CustomServiceMonitors) tomcatInstance.getStatistics().getMonitors().get("USM");
		GridServiceContainer container = tomcatInstance.getGridServiceContainer();
		machineA = container.getMachine();
		tomcatPId = (Long) customServiceDetails.getMonitors().get("Actual Process ID");
		
		client = new WebClient(BrowserVersion.getDefault());
		
		final CountDownLatch removed = new CountDownLatch(1);
		final CountDownLatch added = new CountDownLatch(2);
		
		LogUtils.log("adding a lifecycle listener to tomcat pu");
		ProcessingUnitInstanceLifecycleEventListener eventListener = new ProcessingUnitInstanceLifecycleEventListener() {
			
			@Override
			public void processingUnitInstanceRemoved(
					ProcessingUnitInstance processingUnitInstance) {
				LogUtils.log("USM processing unit instance has been removed due to tomcat failure");
				removed.countDown();	
			}
			
			@Override
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				LogUtils.log("USM processing unit instance has been added");
				added.countDown();	
			}
		};
		tomcat.addLifecycleListener(eventListener);
		
		String pathToTomcat;
		
		LogUtils.log("deleting catalina.sh/bat from pu folder");
		String catalinaPath = "/work/processing-units/tomcat_1/ext/install/apache-tomcat-7.0.22/bin/catalina.";
		String filePath = ScriptUtils.getBuildPath()+ catalinaPath;
		if (isWindows()) {
			pathToTomcat = filePath + "bat";
		}
		else {
			pathToTomcat = filePath + "sh";
		}
		
		File tomcatRun = new File(pathToTomcat);
		
		assertTrue(tomcatRun.delete());
		
		LogUtils.log("killing tomcat process");
		if (isWindows()) {	
			int result = ScriptUtils.killWindowsProcess(tomcatPId.intValue());
			assertTrue(result == 0);	
		}
		else {
			SSHUtils.killProcess(machineA.getHostAddress(), tomcatPId.intValue());
		}
		
		LogUtils.log("waiting for tomcat pu instances to decrease");
		removed.await();
		assertTrue("ProcessingUnitInstanceRemoved event has not been fired", removed.getCount() == 0);
		LogUtils.log("waiting for tomcat pu instances to increase");
		added.await();
		assertTrue("ProcessingUnitInstanceAdded event has not been fired", added.getCount() == 0);	
		LogUtils.log("verifiying tomcat service in running");
		assertTomcatPageExists(client);	
		LogUtils.log("all's well that ends well :)");
	}
	
	private boolean isWindows() {
		return (System.getenv("windir") != null);
	}
	
	private void assertTomcatPageExists(WebClient client) {
		
        HtmlPage page = null;
        try {
            page = client.getPage("http://" + machineA.getHostAddress() + ":8080");
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals("OK", page.getWebResponse().getStatusMessage());
		
	}
	
	@Override
	@AfterMethod
	public void afterTest() {
		try {
			LogUtils.log("tearing down local cloud");
			CommandTestUtils.runCommandAndWait("teardown-localcloud");
		} catch (IOException e) {
			LogUtils.log("teardown-localcloud failed", e);
		} catch (InterruptedException e) {
			LogUtils.log("teardown-localcloud failed", e);
		}
		super.afterTest();	
	}
}
