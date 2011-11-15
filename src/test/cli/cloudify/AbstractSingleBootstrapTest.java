package test.cli.cloudify;

import static framework.utils.LogUtils.log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import net.jini.discovery.Constants;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;

import framework.utils.DumpUtils;
import framework.utils.LogUtils;

public class AbstractSingleBootstrapTest extends AbstractCommandTest {
	
	protected final int WAIT_FOR_TIMEOUT = 20;
	
	@BeforeClass
	public void beforeClass() throws FileNotFoundException, PackagingException, IOException, InterruptedException{
		AdminFactory factory = new AdminFactory();
		factory.addLocator(InetAddress.getLocalHost().getHostAddress() + ":4168");	
		runCommand("teardown-localcloud");
		runCommand("bootstrap-localcloud");
		this.admin = getAdminWithLocators();
		assertTrue("Could not find LUS of local cloud", admin.getLookupServices().waitFor(1, WAIT_FOR_TIMEOUT, TimeUnit.SECONDS));
		this.restUrl = "http://" + InetAddress.getLocalHost().getHostAddress() + ":8100";			
	}
	
	private Admin getAdminWithLocators() throws UnknownHostException {
		admin = newAdmin();
		//Class LocalhostGridAgentBootsrapper defines the locator discovery addresses.
		String nicAddress = Constants.getHostAddress();
		//int defaultLusPort = Constants.getDiscoveryPort();
		AdminFactory factory = new AdminFactory();
		LogUtils.log("adding locator to admin : " + nicAddress + ":4168");
		factory.addLocator(nicAddress + ":4168");
		return factory.createAdmin();
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