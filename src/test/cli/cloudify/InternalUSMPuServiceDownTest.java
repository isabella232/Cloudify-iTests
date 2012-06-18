package test.cli.cloudify;

import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.shutdown.DefaultProcessKiller;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceLifecycleEventListener;
import org.openspaces.pu.service.CustomServiceMonitors;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.usm.USMTestUtils;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.ScriptUtils;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;


public class InternalUSMPuServiceDownTest extends AbstractLocalCloudTest {
	

	WebClient client;
	private final String TOMCAT_URL = "http://127.0.0.1:8080";
	private String serviceDir = ScriptUtils.getBuildPath() + "/recipes/services/tomcat";
	private ProcessingUnitInstanceLifecycleEventListener eventListener;

	@Override
	@BeforeMethod
	public void beforeTest() {
		super.beforeTest();	
		client = new WebClient(BrowserVersion.getDefault());
	}
	
	@SuppressWarnings("deprecation")
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = false)
	public void tomcatServiceDownAndCorruptedTest() throws IOException, InterruptedException {
		
		installTomcat();
		
		final CountDownLatch removed = new CountDownLatch(1);
		final CountDownLatch added = new CountDownLatch(2);
		
		addLifecycleListnersToTomcatPu(removed, added);
		
		deleteCatalinaExec(serviceDir);
		
		Long tomcatPId = getTomcatPId();
		
		killTomcatProcess(tomcatPId);
		
		waitForServiceRecovery(removed, added);
	}

	@SuppressWarnings("deprecation")
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = true)
	public void tomcatServiceDownAndCorruptedTwiceTest() throws IOException, InterruptedException {
		
		installTomcat();
		
		CountDownLatch removed;
		CountDownLatch added;
		
		for (int i = 0; i < 2; i++) {
			LogUtils.log("Starting the " + (i + 1) + " consequtive hard failover.");
			removed = new CountDownLatch(1);
			added = new CountDownLatch(2);
			
			addLifecycleListnersToTomcatPu(removed, added);
			
			deleteCatalinaExec(serviceDir);
			
			Long tomcatPId = getTomcatPId();
			
			killTomcatProcess(tomcatPId);
			
			waitForServiceRecovery(removed, added);
			
			removeLifecycleListnersFromTomcatPu();
		}
		
	}
	private void removeLifecycleListnersFromTomcatPu() {
		ProcessingUnit tomcatPu = getTomcatPu();
		tomcatPu.removeLifecycleListener(this.eventListener);
	}

	private ProcessingUnitInstance getTomcatInstance(){
		ProcessingUnit tomcatPu = getTomcatPu();
		ProcessingUnitInstance tomcatInstance = tomcatPu.getInstances()[0];	
		return tomcatInstance;
	}
	
	private Long getTomcatPId() {
		ProcessingUnitInstance tomcatInstance = getTomcatInstance();
		CustomServiceMonitors customServiceDetails = (CustomServiceMonitors) tomcatInstance.getStatistics().getMonitors().get("USM");
		Long tomcatPId = (Long) customServiceDetails.getMonitors().get("USM_Actual Process ID");
		return tomcatPId;
	}
	

	private void waitForServiceRecovery(final CountDownLatch removed,
			final CountDownLatch added) throws InterruptedException {
		LogUtils.log("waiting for tomcat pu instances to decrease");
		assertTrue("Tomcat PU instance was not decresed", removed.await(240, TimeUnit.SECONDS));
		LogUtils.log("waiting for tomcat pu instances to increase");
		added.await(60 * 6, TimeUnit.SECONDS);
		assertTrue("ProcessingUnitInstanceAdded event has not been fired", added.getCount() == 0);	
		LogUtils.log("verifiying tomcat service in running");
		assertTomcatPageExists(client);	
		LogUtils.log("all's well that ends well :)");
		

	}

	private void addLifecycleListnersToTomcatPu(final CountDownLatch removed,
			final CountDownLatch added) {
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
		ProcessingUnit tomcat = getTomcatPu();
		tomcat.addLifecycleListener(eventListener);
		this.eventListener = eventListener;
	}

	private void deleteCatalinaExec(String serviceDir)
			throws MalformedURLException {
		String pathToTomcat;
		
		LogUtils.log("deleting catalina.sh/bat from pu folder");
		ConfigObject tomcatConfig = new ConfigSlurper().parse(new File(serviceDir, "tomcat-service.properties").toURI().toURL());
		String tomcatVersion = (String) tomcatConfig.get("version");
		String catalinaPath = "/work/processing-units/default_tomcat_1/ext/apache-tomcat-" + tomcatVersion + "/bin/catalina.";
		String filePath = ScriptUtils.getBuildPath()+ catalinaPath;
		if (isWindows()) {
			pathToTomcat = filePath + "bat";
		}
		else {
			pathToTomcat = filePath + "sh";
		}
		
		File tomcatRun = new File(pathToTomcat);
		
		assertTrue("failed while deleting file: " + tomcatRun, tomcatRun.delete());
	}

	private void killTomcatProcess(long tomcatPId) throws IOException {
		LogUtils.log("killing tomcat process : " + tomcatPId);
		DefaultProcessKiller dpk = new DefaultProcessKiller();
		try {
			dpk.killProcess(tomcatPId);
		} catch (USMException e) {
			AssertFail("failed to kill tomcat process with pid: " + tomcatPId);
		}
		
		int responseCode = getResponseCode(TOMCAT_URL);
		assertTrue("Tomcat service is still running. Request returned response code: " + responseCode, HttpStatus.SC_NOT_FOUND == responseCode);
	}

	private void installTomcat() throws UnknownHostException {
		String command = "connect " + this.restUrl + ";" + "install-service " + "--verbose -timeout 10 " + this.serviceDir;
		try {
			LogUtils.log("installing tomcat service using Cli");
			CommandTestUtils.runCommandAndWait(command);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		LogUtils.log("Retrieving tomcat process pid from admin");
		ProcessingUnit tomcat = getTomcatPu();
		assertTrue("USM Service state is not RUNNING", USMTestUtils.waitForPuRunningState("default.tomcat", 100, TimeUnit.SECONDS, admin));
		ProcessingUnitUtils.waitForDeploymentStatus(tomcat, DeploymentStatus.INTACT);
		assertTrue(tomcat.getStatus().equals(DeploymentStatus.INTACT));
		assertTrue("Tomcat instance is not in RUNNING State", USMTestUtils.waitForPuRunningState("default.tomcat", 60, TimeUnit.SECONDS, admin));
	}
	
	private ProcessingUnit getTomcatPu() {
		ProcessingUnit tomcat = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("default", "tomcat"), 10, TimeUnit.SECONDS);
		assertNotNull("Tomcat instace is null", tomcat);
		return tomcat;
	}

	private boolean isWindows() {
		return (System.getenv("windir") != null);
	}
	
	private void assertTomcatPageExists(WebClient client) {
		ProcessingUnitInstance tomcatInstance = getTomcatInstance();
		GridServiceContainer container = tomcatInstance.getGridServiceContainer();		
		Machine tomcatMachine = container.getMachine();
		
        HtmlPage page = null;
        try {
            page = client.getPage("http://" + tomcatMachine.getHostAddress() + ":8080");
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals("OK", page.getWebResponse().getStatusMessage());
		
	}
	
	//if service is down, this method will return a 404 not found exception.
	private int getResponseCode(String urlString) throws IOException{
		URL url = new URL ( urlString );
		URLConnection connection = url.openConnection();
		try {
			connection.connect();
		}catch (ConnectException e){
			LogUtils.log("The connection to " + urlString + " has failed.");
			return HttpStatus.SC_NOT_FOUND;
		}
		HttpURLConnection httpConnection = (HttpURLConnection) connection;
		int code = httpConnection.getResponseCode();
		return code;

	}
}
