package test.cli.cloudify;

import static framework.utils.LogUtils.log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.AdminFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;

import framework.utils.DumpUtils;
import framework.utils.LogUtils;

public class AbstractLocalCloudTest extends AbstractCommandTest {
	
	protected final int WAIT_FOR_TIMEOUT = 20;
	
	@BeforeClass
	public void beforeClass() throws FileNotFoundException, PackagingException, IOException, InterruptedException{
		AdminFactory factory = new AdminFactory();
		factory.addLocator(InetAddress.getLocalHost().getHostAddress() + ":4168");	
		runCommand("teardown-localcloud");
		runCommand("bootstrap-localcloud");
		this.admin = factory.create();
		assertTrue("Could not find LUS of local cloud", admin.getLookupServices().waitFor(1, WAIT_FOR_TIMEOUT, TimeUnit.SECONDS));
		this.restUrl = "http://" + InetAddress.getLocalHost().getHostAddress() + ":8100";			
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
