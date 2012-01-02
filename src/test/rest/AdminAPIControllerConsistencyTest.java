package test.rest;

import java.io.IOException;

import junit.framework.Assert;

import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AdminUtils;
import framework.utils.DeploymentUtils;

import test.AbstractTest;


public class AdminAPIControllerConsistencyTest extends AbstractTest {

	protected String restPrefix;
	protected final int PUs_CONTAINING_RESTFUL = 1;	
	private String TEST_PU = "simpleFaultyStatelessPu";
	GridServiceManager gsm;
	@Override
	@BeforeMethod
	public void beforeTest() {
		super.beforeTest();
		AdminUtils.loadGSM(admin.getGridServiceAgents().waitForAtLeastOne());
		AdminUtils.loadGSC(admin.getGridServiceAgents().waitForAtLeastOne());
		gsm = admin.getGridServiceManagers().waitForAtLeastOne();
		restPrefix = RestConsistencyTestUtil.deployRestServer(admin);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 5, groups = "1", enabled = false)
	// actually this tests the loading of a gsm and gsc in addition to being a simple test case
	public void simpleTest() throws IOException{
		RestConsistencyTestUtil.runConsistencyTest(admin, restPrefix);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 5, groups = "1", enabled = false)
	public void puManipulationTest() throws Exception{
		
		ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive(TEST_PU + ".jar")));
		pu.waitFor(pu.getTotalNumberOfInstances());
		Assert.assertEquals("Admin could not deploy PU" , PUs_CONTAINING_RESTFUL + 1 , admin.getProcessingUnits().getProcessingUnits().length);
		pu.incrementInstance();
		pu.waitFor(2);
		RestConsistencyTestUtil.runConsistencyTest(admin, restPrefix);
		
		pu.decrementInstance();
		Thread.sleep(1000 * 2); 
		Assert.assertEquals("The admin didn't decrease the PU's instance number" , 1 ,pu.getInstances().length);
		RestConsistencyTestUtil.runConsistencyTest(admin, restPrefix);
		
		pu.getInstances()[0].restartAndWait();
		Thread.sleep(1000 * 2); // the admin needs more time to synchronize after restart( even with restartAndWait() )
		Assert.assertEquals("The admin didn't decrease the PU's instance number" , 1 ,pu.getInstances().length);
		RestConsistencyTestUtil.runConsistencyTest(admin, restPrefix);
		
		pu.undeploy();
		Thread.sleep(1000 * 2);
		Assert.assertEquals("The admin didn't undeploy the PU" , null ,admin.getProcessingUnits().getProcessingUnit(TEST_PU));
		RestConsistencyTestUtil.runConsistencyTest(admin, restPrefix);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 5, groups = "1", enabled = false)
	public void restartGSCTest() throws Exception{
		admin.getGridServiceContainers().getContainers()[0].restart();
		admin.getGridServiceContainers().waitFor(1);
		admin.getGridServiceContainers().getContainers()[0].waitFor("rest", 1);
		RestConsistencyTestUtil.runConsistencyTest(admin, restPrefix);
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 5, groups = "1", enabled = false)
	public void restartGSMTest() throws Exception{
		admin.getGridServiceManagers().getManagers()[0].restart();
		admin.getGridServiceManagers().waitFor(1);
		RestConsistencyTestUtil.runConsistencyTest(admin, restPrefix);
	}
		
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 5, groups = "1", enabled = false)
	public void deployESMAndRestartTest() throws Exception{
		
		AdminUtils.loadESM(admin.getGridServiceAgents().waitForAtLeastOne());
		RestConsistencyTestUtil.runConsistencyTest(admin, restPrefix);
		
		admin.getElasticServiceManagers().getManagers()[0].restart();
		admin.getElasticServiceManagers().waitFor(1);
		Thread.sleep(1000 * 2);
		RestConsistencyTestUtil.runConsistencyTest(admin, restPrefix);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 5, groups = "1", enabled = false)
	public void spaceManipulationTest() throws IOException{
		
		ProcessingUnit pu = admin.getGridServiceManagers().deploy(new SpaceDeployment("testSpace").numberOfInstances(1));
		pu.waitForSpace();
		Assert.assertEquals("Admin could not deploy space" , 1 , admin.getSpaces().getSpaces().length);
		RestConsistencyTestUtil.runConsistencyTest(admin, restPrefix);
	}
}