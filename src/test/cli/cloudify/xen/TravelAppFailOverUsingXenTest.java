package test.cli.cloudify.xen;

import java.io.File;
import java.util.ArrayList;

import org.junit.Assert;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;

import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;

import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class TravelAppFailOverUsingXenTest extends AbstractApplicationFailOverXenTest {
	
	private final String travelAppDirPath = ScriptUtils.getBuildPath() + "/examples/travel";
	private int tomcatPort;
	private int cassandraPort;
	private String travelHostIp;
		
	@BeforeClass
	public void beforeClass() {
		super.beforeTest();
		tomcatPort = tomcatPort();
		cassandraPort = cassandraPort();
		
		startAgent(0 ,"tomcat","cassandra");
	    assertEquals("Expecting exactly 2 grid service agents to be added", 2, getNumberOfGSAsAdded());
	    assertEquals("Expecting 0 agents to be removed", 0, getNumberOfGSAsRemoved());	 
	    travelHostIp = admin.getZones().getByName("cassandra").getGridServiceAgents().getAgents()[0].getMachine().getHostAddress();
	}
	@Override
	@BeforeMethod
	public void beforeTest(){
		
	}

	@Override
	@AfterMethod
	public void afterTest() {
		try {			
			CommandTestUtils.runCommandAndWait("connect " + restUrl + " ;uninstall-application travel");
		} catch (Exception e) {	}
		
		assertAppUninstalled("trave");	
	}
	
	@AfterClass
	public void afterClass(){
		super.afterTest();
	}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testTravelApp() throws Exception{
		assertInstallApp(tomcatPort, travelHostIp, cassandraPort, travelHostIp, travelAppDirPath);
		isTravelAppInstalled(cassandraPort , tomcatPort, travelHostIp);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testTravelAppTomcatPuInstFailOver() throws Exception{
		assertInstallApp(tomcatPort, travelHostIp, cassandraPort, travelHostIp, travelAppDirPath);
		isTravelAppInstalled(cassandraPort , tomcatPort, travelHostIp);
		
		ProcessingUnit tomcat = admin.getProcessingUnits().getProcessingUnit("tomcat");
		
		int tomcatPuInstancesAfterInstall = tomcat.getInstances().length;
		LogUtils.log("destroying the pu instance holding tomcat");
		tomcat.getInstances()[0].destroy();
		assertPuInstanceKilled("tomcat" , tomcatPort ,travelHostIp , tomcatPuInstancesAfterInstall);
		assertPuInstanceRessurected("tomcat" , tomcatPort ,travelHostIp, tomcatPuInstancesAfterInstall) ;
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testTravelAppTomcatGSCFailOver() throws Exception{
		assertInstallApp(tomcatPort, travelHostIp, cassandraPort, travelHostIp, travelAppDirPath);
		isTravelAppInstalled(cassandraPort , tomcatPort, travelHostIp);
		
		ProcessingUnit tomcat = admin.getProcessingUnits().getProcessingUnit("tomcat");
		int tomcatPuInstancesAfterInstall = tomcat.getInstances().length;
		LogUtils.log("restarting GSC containing tomcat");
		tomcat.getInstances()[0].getGridServiceContainer().kill();
		assertPuInstanceKilled("tomcat" , tomcatPort,travelHostIp , tomcatPuInstancesAfterInstall);
		assertPuInstanceRessurected("tomcat" , tomcatPort,travelHostIp , tomcatPuInstancesAfterInstall);
	}

/////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private void isTravelAppInstalled(int port1 ,int port2 ,String host) {
		ProcessingUnit cassandra = admin.getProcessingUnits().waitFor("cassandra");
		ProcessingUnit tomcat = admin.getProcessingUnits().waitFor("tomcat");
		Assert.assertNotNull("cassandra was not deployed", cassandra);
		Assert.assertNotNull("tomcat was not deployed", tomcat);
		
		boolean cassandraDeployed = cassandra.waitFor(cassandra.getTotalNumberOfInstances());
		boolean tomcatDeployed = tomcat.waitFor(tomcat.getTotalNumberOfInstances());
		
		Assert.assertTrue("cassandra pu didn't deploy all of it's instances", cassandraDeployed);
		Assert.assertTrue("tomcat pu didn't deploy all of it's instances", tomcatDeployed);
		
		if(port1 > 0 && port2 >0){
			LogUtils.log("asserting needed ports for application are taken");
			boolean port1Availible = portIsAvailible(port1 ,host);
			boolean port2Availible = portIsAvailible(port2 ,host);
			assertTrue("port was not occupied after installation - port number " + port1, !port1Availible);
			assertTrue("port was not occupied after installation - port number " + port2, !port2Availible);
		}	
		LogUtils.log("Travel application installed");
	}
	
	private int tomcatPort(){
		Service tomcatService = null;
		try {
			tomcatService = ServiceReader.getServiceFromDirectory(new File(travelAppDirPath + "/tomcat")).getService();
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return tomcatService.getNetwork().getPort();
	}
	
	private int cassandraPort(){
		Service cassandraService = null;
		try {
			cassandraService = ServiceReader.getServiceFromDirectory(new File(travelAppDirPath + "/cassandra")).getService();
		} catch (Exception e) {
			e.printStackTrace();
		} 
		ArrayList<Integer> cassandraPorts = (ArrayList<Integer>) cassandraService.getPlugins().get(0).getConfig().get("Port");
		return cassandraPorts.get(0);
	}
}
