package test.security.custom;

import static test.utils.LogUtils.log;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminException;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.SecurityAccessException;
import org.openspaces.core.space.UrlSpaceConfigurer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.AbstractTestSuite;
import test.utils.AdminUtils;
import test.utils.ScriptUtils;

import com.gigaspaces.common.MessageSecure;
import com.gigaspaces.common.MyUserDetails;
import com.gigaspaces.security.AccessDeniedException;
import com.gigaspaces.security.SecurityException;
import com.j_spaces.core.IJSpace;

public class CustomSecurityTest extends AbstractTestSuite{
	
	private static final int FEEDER_BATCH_SIZE = 1000;
	private GridServiceManager m_gsm;
	private AtomicLong _idGenerator = new AtomicLong(0);
	
	@Override
	@BeforeClass
	public void beforeClass(){
		super.beforeClass();
		
	    String securityConfigFilePath = ScriptUtils.getBuildPath()+"/config/security/custom-test-security.properties";
	    String[] props = { "-Dcom.gs.security.properties-file=" + securityConfigFilePath };
		
		//load up 1 gsm and 2gscs
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		m_gsm = AdminUtils.loadGSM(gsa.getMachine());
		AdminUtils.loadGSCWithSystemProperty(gsa.getMachine(), props);
		AdminUtils.loadGSCWithSystemProperty(gsa.getMachine(), props);
		
		//deploy pu
		File file = new File("./apps/security/jars/processor.jar");
		ProcessingUnit processor = m_gsm.deploy(new ProcessingUnitDeployment(file));
		processor.waitForSpace();
	}
	
	public void basicFailureReadTest(Admin testAdmin){
		testAdmin.getSpaces().waitFor("processorSpace");
		GigaSpace gigaSpace = testAdmin.getSpaces().getSpaceByName("processorSpace").getGigaSpace();
		gigaSpace.read(null);
		Assert.fail("Expected an exception while trying to READ with forbidden user but didnt receive any");
	}
	
	public void basicFailureWriteTest(Admin testAdmin){
		testAdmin.getSpaces().waitFor("processorSpace");
		GigaSpace gigaSpace = testAdmin.getSpaces().getSpaceByName("processorSpace").getGigaSpace();
		MessageSecure m = new MessageSecure();
		m.setId((int)_idGenerator.getAndIncrement());
		gigaSpace.write(m);
		Assert.fail("Expected an exception while trying to WRITE with forbidden user but didnt receive any");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
	public void testReadWithPermittedUser(){
		try{
			Admin testAdmin = AdminUtils.createSecuredAdmin(new MyUserDetails("Reader", "reader"));
			testAdmin.getSpaces().waitFor("processorSpace");
			GigaSpace gigaSpace = testAdmin.getSpaces().getSpaceByName("processorSpace").getGigaSpace();
			gigaSpace.read(null);
		}catch(Exception e){
			Assert.fail("Received an exception while trying to READ with permitted user, exception: " + e);
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
	public void testReadWithForbiddenUser(){
		try{
			Admin testAdmin = AdminUtils.createSecuredAdmin(new MyUserDetails("Writer", "writer"));
			basicFailureReadTest(testAdmin);
		}catch(SecurityAccessException e){
			if (e.getCause() instanceof AccessDeniedException) {
				log("Received expected SecurityAccessException: "+ e);
			} else {
				throw e;
			}
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
	public void testWriteWithPermittedUser(){
		try{
			Admin testAdmin = AdminUtils.createSecuredAdmin(new MyUserDetails("Writer", "writer"));
			testAdmin.getSpaces().waitFor("processorSpace");
			GigaSpace gigaSpace = testAdmin.getSpaces().getSpaceByName("processorSpace").getGigaSpace();
			MessageSecure m = new MessageSecure();
			m.setId((int)_idGenerator.getAndIncrement());
			gigaSpace.write(m);
		}catch(Exception e){
			Assert.fail("Received an exception while trying to WRITE with permitted user, exception: " + e);
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
	public void testWriteWithForbiddenUser(){
		try{
			Admin testAdmin = AdminUtils.createSecuredAdmin(new MyUserDetails("Reader", "reader"));
			basicFailureWriteTest(testAdmin);
		}catch(SecurityAccessException e){
			if (e.getCause() instanceof AccessDeniedException) {
				log("Received expected SecurityAccessException: "+ e);
			} else {
				throw e;
			}
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
	public void testNoUser(){
		try{
			basicFailureReadTest(admin);
		}catch(AdminException e){
			if (e.getCause() instanceof SecurityException) {
				log("Received expected AdminException: "+ e);
			} else {
				throw e;
			}
		}
	}
		
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
	public void testNonExistingUser(){
		Admin testAdmin = AdminUtils.createSecuredAdmin(new MyUserDetails("bla", "bla"));
		int numOfSpaces = testAdmin.getSpaces().getSpaces().length;
		Assert.assertEquals(numOfSpaces, 0); // should not find any space
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
	public void testFeeder(){
		//get proxy
		MyUserDetails mud = new MyUserDetails("ReaderWriter", "readerwriter");
    	UrlSpaceConfigurer usc = new UrlSpaceConfigurer("jini://*/*/processorSpace?groups="+admin.getGroups()[0]).userDetails(mud);
        IJSpace space = usc.space();
        GigaSpace gigaSpace = new GigaSpaceConfigurer(space).gigaSpace();
        
        //start feeding
        for (int counter = 0; counter < FEEDER_BATCH_SIZE; counter++) {
            MessageSecure msg = new MessageSecure(counter, "Hello ");
            gigaSpace.write(msg);
        }
        log("Feeder wrote " + FEEDER_BATCH_SIZE + " messages");
        
        //read results
        MessageSecure template = new MessageSecure();
        log("Here is one of them printed out: " + gigaSpace.read(template));

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ie) { /*do nothing*/}

        int numInSpace = gigaSpace.count(template);
        log("There are " + numInSpace + " Message objects in the space now.");
	}
	
}
