package test.security.spring;



import static test.utils.LogUtils.log;

import java.util.concurrent.atomic.AtomicLong;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.SecurityAccessException;
import org.openspaces.core.space.UrlSpaceConfigurer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.data.Person;
import test.utils.AdminUtils;
import test.utils.ProcessingUnitUtils;
import test.utils.ScriptUtils;
/**
 * the test uses 3 types of gigaspaces proxy retrieval in order to check in-memory-security
 * in-memory-security-config.xml and security.properties are copied to gigaspaces config/security/ directory when SG runs
 * 
 * @author rafi
 *
 */
public class SpringSecurityTest extends AbstractTest{
	
	interface GigaSpaceAction{
		public Object readWriteAction();
		public String getActionName();
		public void setGigaSpace(GigaSpace gigaSpace);
	}

	private static final String spaceName = "A";
	private String currentUserName;
	private boolean dicoverUnmanagedSpaces = false;
	private AtomicLong idGenerator = new AtomicLong();
	
	@BeforeMethod
	public void setup(){
	    
	    String securityConfigFilePath = ScriptUtils.getBuildPath()+"/config/security/spring-test-security.properties";
	    String[] props = { "-Dcom.gs.security.properties-file=" + securityConfigFilePath };
	    
		log("Waiting for machine");
		admin.getMachines().waitFor(1);
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		log("Loading gsm");
		
		GridServiceManager gsm = AdminUtils.loadGSM(gsa.getMachine());
		
		log("Loading gsc");
		
		AdminUtils.loadGSCWithSystemProperty(gsa.getMachine(), props);
		
		log("Deploying space");
		ProcessingUnit spacePU = gsm.deploy(new SpaceDeployment(spaceName)
		        .partitioned(1, 1)
		        .secured(true));
		
		ProcessingUnitUtils.waitForDeploymentStatus(spacePU, DeploymentStatus.INTACT);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1")
	public void test(){
		runAllTestSuites();
		log("finished all test suites with discoverUnamagedSpaces off");
		//now with discoverUnmanagedSpaces
		this.dicoverUnmanagedSpaces = true;
		runAllTestSuites();
		log("finished all test suites with discoverUnamagedSpaces on");
		log("starting undeploy test");
		testUndeploy();
	}

	private void runAllTestSuites() {
		log("starting read test suite");
		readTestSuite();
		log("finished read test suite, starting write test suite");
		writeTestSuite();
	}

	public void readTestSuite(){
		//create action implementation
		GigaSpaceAction readAction = new GigaSpaceAction(){
			
			private GigaSpace gigaSpace;
			
			public String getActionName() {
				return "READ";
			}

			public Object readWriteAction() {
				return gigaSpace.read(new Person());
			}

			public void setGigaSpace(GigaSpace gigaSpace) {
				this.gigaSpace = gigaSpace;
			}
		};
		
		actionSuite(readAction,"Edward","koala", true);
		actionSuite(readAction,"Rafi", "rafi", false);
	}
	
	public void writeTestSuite(){
		//create action implementation
		GigaSpaceAction writeAction = new GigaSpaceAction(){
			
			private GigaSpace gigaSpace;
			
			public String getActionName() {
				return "WRITE";
			}

			public Object readWriteAction() {
				Person p=new Person();
				p.setId(idGenerator.getAndIncrement());
				return gigaSpace.write(p);
			}

			public void setGigaSpace(GigaSpace gigaSpace) {
				this.gigaSpace = gigaSpace;
			}
		};
		
		actionSuite(writeAction, "Edward", "koala", false);
		actionSuite(writeAction, "Rafi", "rafi", true);
	}
	
	private void actionSuite(GigaSpaceAction action, String username, String password, boolean expectedTestResult) {
		//create admin - with no discoverUnamangedSpaces
		admin = getAdmin(username,password);
				
		//gets gigaSpace from spaceInstance
		GigaSpace gigaSpace = getGigaFromSpaceInstance();
		action.setGigaSpace(gigaSpace);
		
		testAction(action,expectedTestResult);
		
		//gets gigaSpace from admin -> getSpaces
		gigaSpace = getGigaFromAdminGetSpaces();
		action.setGigaSpace(gigaSpace);
		
		testAction(action,expectedTestResult);
		
		
		//gets gigaSpace from url
		gigaSpace = getGigaFromUrl(username,password);
		action.setGigaSpace(gigaSpace);
		
		testAction(action,expectedTestResult);
	}


	public void testAction(GigaSpaceAction action,boolean shouldSucceed){
		try{
			action.readWriteAction();
			if (!shouldSucceed){
				Assert.fail("user "+this.currentUserName+" should not be able to "+action.getActionName()+" from space");
			}
		}catch(SecurityAccessException e){
			if (shouldSucceed){
				Assert.fail("user "+this.currentUserName+" should be able to "+action.getActionName()+" from space but got this exception"+e.getMessage());
			}
		}
	}
			
	private GigaSpace getGigaFromSpaceInstance() {
		admin.getProcessingUnits().waitFor(spaceName);
		ProcessingUnitUtils.waitForActiveElection(admin.getProcessingUnits().getProcessingUnit(spaceName));
		return admin.getSpaces().waitFor(spaceName).getPartitions()[0].getPrimary().getGigaSpace();
	}
	
	private GigaSpace getGigaFromAdminGetSpaces() {
		admin.getSpaces().waitFor(spaceName);
		return admin.getSpaces().getSpaces()[0].getGigaSpace(); 
	}

	private GigaSpace getGigaFromUrl(String userName,String password) {
		admin.getSpaces().waitFor(spaceName);
		UrlSpaceConfigurer url = new UrlSpaceConfigurer("jini://*/*/"+spaceName).lookupGroups(admin.getGroups()[0]).userDetails(userName,password);
		GigaSpaceConfigurer gigaUrl=new GigaSpaceConfigurer(url.space());
		GigaSpace gigaSpace = gigaUrl.gigaSpace();
		return gigaSpace;
	}
	
	public Admin getAdmin(String userName, String password){
		AdminFactory factory = new AdminFactory().userDetails(userName, password).addGroup(admin.getGroups()[0]); 
		if (this.dicoverUnmanagedSpaces ){
			factory.discoverUnmanagedSpaces();
		}
		Admin admin=factory.createAdmin();
		this.currentUserName = userName;
		admin.getProcessingUnits().waitFor(spaceName);
		admin.getSpaces().waitFor(spaceName);
		return admin;
	}
			
	private void testUndeploy() {
		try{
			admin = getAdmin("Allen","kangaroo");
			ProcessingUnit pu = admin.getProcessingUnits().getProcessingUnit(spaceName);
			pu.undeploy();
		}catch(SecurityAccessException e){				
			Assert.fail("user "+currentUserName+" should be able to undeploy processingUnit");
		}		
	}	
}
