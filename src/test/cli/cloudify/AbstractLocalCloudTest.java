package test.cli.cloudify;

import static framework.utils.LogUtils.log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;

import framework.utils.DumpUtils;

public class AbstractLocalCloudTest extends AbstractCommandTest {
	
	@BeforeClass
	public void beforeClass() throws FileNotFoundException, PackagingException, IOException, InterruptedException{		
		AdminFactory factory = new AdminFactory();
		factory.addLocator(InetAddress.getLocalHost().getHostAddress() + ":4168");
		this.admin = factory.create();
		
		if(admin.getGridServiceAgents() != null){
			if(admin.getGridServiceAgents().waitFor(1, 10, TimeUnit.SECONDS))
				for(GridServiceAgent gsa : admin.getGridServiceAgents().getAgents())
					gsa.shutdown();
		}
		runCommand("bootstrap-localcloud");
		assertTrue("Could not find LUS of local cloud", admin.getLookupServices().waitFor(1, 10, TimeUnit.SECONDS));
		this.restUrl = "http://" + InetAddress.getLocalHost().getHostAddress() + ":8100";			
	}
	
	@Override
	@BeforeMethod
	public void beforeTest(){
		
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
	
	@AfterClass
	public void afterClass() throws IOException, InterruptedException{	
		runCommand("teardown-localcloud");
		
		if(admin.getGridServiceAgents() != null){
			if(admin.getGridServiceAgents().waitFor(1, 10, TimeUnit.SECONDS))
				for(GridServiceAgent gsa : admin.getGridServiceAgents().getAgents())
					gsa.shutdown();
		}
	}
}
