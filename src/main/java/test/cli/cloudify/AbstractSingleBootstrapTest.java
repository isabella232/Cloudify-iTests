package test.cli.cloudify;

import static framework.utils.LogUtils.log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import net.jini.discovery.Constants;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import test.AbstractTest;
import framework.utils.DumpUtils;
import framework.utils.LogUtils;



@Deprecated
public class AbstractSingleBootstrapTest extends AbstractTest {
	
	protected final int WAIT_FOR_TIMEOUT = 20;
	protected String restUrl;
	
	@BeforeClass
	public void beforeClass() throws Exception{
		AdminFactory factory = new AdminFactory();
		factory.addLocator(InetAddress.getLocalHost().getHostAddress() + ":" + CloudifyConstants.DEFAULT_LOCALCLOUD_LUS_PORT);	
		runCommand("teardown-localcloud");
		runCommand("bootstrap-localcloud");
		this.admin = getAdminWithLocators();
		assertTrue("Could not find LUS of local cloud", admin.getLookupServices().waitFor(1, WAIT_FOR_TIMEOUT, TimeUnit.SECONDS));
		this.restUrl = "http://" + InetAddress.getLocalHost().getHostAddress() + ":8100";			
	}
	
	private Admin getAdminWithLocators() throws UnknownHostException {
		// admin = newAdmin();
		//Class LocalhostGridAgentBootsrapper defines the locator discovery addresses.
		String nicAddress = Constants.getHostAddress();
		//int defaultLusPort = Constants.getDiscoveryPort();
		AdminFactory factory = new AdminFactory();
		LogUtils.log("adding locator to admin: " + nicAddress + ":" + CloudifyConstants.DEFAULT_LOCALCLOUD_LUS_PORT);
		factory.addLocator(nicAddress + ":" + CloudifyConstants.DEFAULT_LOCALCLOUD_LUS_PORT);
		return factory.createAdmin();
	}
	
	protected String runCommand(String command) throws IOException, InterruptedException {
		return CommandTestUtils.runCommandAndWait(command);
	}
	
	@Override
	@BeforeMethod
	public void beforeTest(){
		LogUtils.log("Test Configuration Started: "+ this.getClass());
	}
	
	@Override
	@AfterMethod
	public void afterTest(){
		if (admin != null) {
	    	try {
	            DumpUtils.dumpLogs(admin);
	        } catch (Throwable t) {
	            log("failed to dump logs", t);
	        }
	    }
	}
	
	@AfterClass(alwaysRun = true)
	public void afterClass() throws IOException, InterruptedException{	
		runCommand("teardown-localcloud");
	}
}