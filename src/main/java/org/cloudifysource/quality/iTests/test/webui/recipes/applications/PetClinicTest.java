package org.cloudifysource.quality.iTests.test.webui.recipes.applications;

import static org.testng.AssertJUnit.fail;
import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.MainNavigation;
import com.gigaspaces.webuitf.dashboard.DashboardTab;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.ApplicationServicesGrid;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.ApplicationsMenuPanel;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.Icon;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.InfrastructureServicesGrid;
import com.gigaspaces.webuitf.services.ServicesTab;
import com.gigaspaces.webuitf.topology.TopologyTab;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationMap;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationNode;

public class PetClinicTest extends AbstractSeleniumApplicationRecipeTest {
	
	private static final String PETCLINIC_APPLICATION_NAME = "petclinic";
	private static final String MONGOD_SERVICE_NAME = "mongod";
	private static final String MONGOS_SERVICE_NAME = "mongos";
	private static final String MONGOCFG_SERVICE_NAME = "mongoConfig";
	private static final String TOMCAT_SERVICE_NAME = "tomcat";
	private static final String APACHELB_SERVICE_NAME = "apacheLB";

	@Override
	@BeforeMethod
	public void install() throws IOException, InterruptedException {
		setCurrentApplication(PETCLINIC_APPLICATION_NAME);
		super.install();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void petClinicDemoTest() throws Exception {
		
		// get new login page
		LoginPage loginPage = getLoginPage();

		MainNavigation mainNav = loginPage.login();

		DashboardTab dashboardTab = mainNav.switchToDashboard();
		
		final InfrastructureServicesGrid infrastructureServicesGrid = dashboardTab.getServicesGrid().getInfrastructureGrid();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				return ((infrastructureServicesGrid.getESMInst().getCount() == 1) 
						&& (infrastructureServicesGrid.getESMInst().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue("No esm in showing in the dashboard", condition, waitingTime);

		ApplicationsMenuPanel appMenuPanel = dashboardTab.getServicesGrid().getApplicationsMenuPanel();

		appMenuPanel.selectApplication(MANAGEMENT_APPLICATION_NAME);

		final ApplicationServicesGrid applicationServices = dashboardTab.getServicesGrid().getApplicationServicesGrid();

		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				return ((applicationServices.getWebModule().getCount() == 2)
						&& (applicationServices.getWebModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		appMenuPanel.selectApplication(PETCLINIC_APPLICATION_NAME);
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				return ((applicationServices.getWebServerModule().getCount() == 1)
						&& (applicationServices.getWebServerModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				return ((applicationServices.getNoSqlDbModule().getCount() == 3)
						&& (applicationServices.getNoSqlDbModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		TopologyTab topologyTab = mainNav.switchToTopology();

		final ApplicationMap appMap = topologyTab.getApplicationMap();

		topologyTab.selectApplication(MANAGEMENT_APPLICATION_NAME);

		takeScreenShot(this.getClass(),"petClinicDemoTest", "management-application");

		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode restNode = appMap.getApplicationNode("rest");
				return ((restNode != null) && (restNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "petClinicDemoTest", "failed");

		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode webuiNode = appMap.getApplicationNode("webui");
				return ((webuiNode != null) && (webuiNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "petClinicDemoTest", "failed");
		
		takeScreenShot(this.getClass(), "petClinicDemoTest","passed-topology");
		
		topologyTab.selectApplication(PETCLINIC_APPLICATION_NAME);
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode mongodNode = appMap.getApplicationNode(MONGOD_SERVICE_NAME);
				return mongodNode != null && 
						appMap.getApplicationNodeStatus( MONGOD_SERVICE_NAME ) != null && 
						appMap.getApplicationNodeStatus( MONGOD_SERVICE_NAME ).equals( ApplicationMap.CONN_STATUS_OK );
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(),"petClinicDemoTest", "failed");
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode mongosNode = appMap.getApplicationNode(MONGOS_SERVICE_NAME);
				return mongosNode != null && 
						appMap.getApplicationNodeStatus( MONGOS_SERVICE_NAME ) != null && 
						appMap.getApplicationNodeStatus( MONGOS_SERVICE_NAME ).equals( ApplicationMap.CONN_STATUS_OK );
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "petClinicDemoTest","failed");
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode mongocfgNode = appMap.getApplicationNode(MONGOCFG_SERVICE_NAME);
				return mongocfgNode != null && 
						appMap.getApplicationNodeStatus( MONGOCFG_SERVICE_NAME ) != null && 
						appMap.getApplicationNodeStatus( MONGOCFG_SERVICE_NAME ).equals( ApplicationMap.CONN_STATUS_OK );
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(),"petClinicDemoTest", "failed");	
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode tomcatNode = appMap.getApplicationNode(TOMCAT_SERVICE_NAME);
				return tomcatNode != null && 
						appMap.getApplicationNodeStatus( TOMCAT_SERVICE_NAME ) != null && 
						appMap.getApplicationNodeStatus( TOMCAT_SERVICE_NAME ).equals( ApplicationMap.CONN_STATUS_OK );
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "petClinicDemoTest","failed");
		
		condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				ApplicationNode apachelbNode = appMap.getApplicationNode(APACHELB_SERVICE_NAME);
				return apachelbNode != null && 
						appMap.getApplicationNodeStatus( APACHELB_SERVICE_NAME ) != null && 
						appMap.getApplicationNodeStatus( APACHELB_SERVICE_NAME ).equals( ApplicationMap.CONN_STATUS_OK );
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "petClinicDemoTest","failed");
		
		
/*		ApplicationNode applicationNodeTomcat = appMap.getApplicationNode(TOMCAT_FULL_SERVICE_NAME);
		List<Connector> tomcatConnectors = applicationNodeTomcat.getConnectors();
		
		final ApplicationNode applicationNodeMongos = appMap.getApplicationNode(MONGOS_FULL_SERVICE_NAME);
		List<Connector> mongosConnectors = applicationNodeMongos.getConnectors();
		
		ApplicationNode applicationNodeMongod = appMap.getApplicationNode(MONGOD_FULL_SERVICE_NAME);
		
		ApplicationNode applicationNodeMongoConfig = appMap.getApplicationNode(MONGOCFG_FULL_SERVICE_NAME);
		
		ApplicationNode applicationNodeApacheLB = appMap.getApplicationNode(APACHELB_FULL_SERVICE_NAME);
*/
		
		Collection<String> tomcatConnectorTargets = appMap.getConnectorTargets( TOMCAT_SERVICE_NAME );
		Collection<String> tomcatConnectorSources = appMap.getConnectorSources( TOMCAT_SERVICE_NAME );
		
		LogUtils.log( "Sources for service [" + TOMCAT_SERVICE_NAME + "] are: " + 
				Arrays.toString( tomcatConnectorSources.toArray( new String[tomcatConnectorSources.size()] ) ) );
		LogUtils.log( "Targets for service [" + TOMCAT_SERVICE_NAME + "] are: " + 
				Arrays.toString( tomcatConnectorTargets.toArray( new String[tomcatConnectorTargets.size()] ) ) );		
		
		assertEquals( "[" + TOMCAT_SERVICE_NAME + "] connector tagets must be 1", tomcatConnectorTargets.size(), 1 );
		assertEquals( "[" + TOMCAT_SERVICE_NAME + "] connector sources must be 1", tomcatConnectorSources.size(), 1 );
		
		assertTrue( "[" + TOMCAT_SERVICE_NAME + "] connector source must be [" + 
				MONGOS_SERVICE_NAME + "]", tomcatConnectorSources.contains( MONGOS_SERVICE_NAME ) );
		assertTrue( "[" + TOMCAT_SERVICE_NAME + "] connector target must be [" + 
				APACHELB_SERVICE_NAME + "]", tomcatConnectorTargets.contains( APACHELB_SERVICE_NAME ) );
		
//		for (Connector c : tomcatConnectors) {
//			String name = c.getTarget().getName();
//			assertTrue(name.equals(applicationNodeMongos.getName()) ||name.equals(applicationNodeApacheLB.getName()));
//		}
		
		takeScreenShot(this.getClass(), "petClinicDemoTest", "petClinicDemoTest");
		
		// there are 3 connectors attached to mongos. 2 going out, 1 coming in.
        // mongos ==> mongoConfig
        // mongos ==> mongod
        // tomcat ==> mongos
        
//		assertEquals( "checking number of mongos connectors (the edges connected to mongos)", 3, mongosConnectors.size() );
		Collection<String> mongosConnectorTargets = appMap.getConnectorTargets( MONGOS_SERVICE_NAME );
		Collection<String> mongosConnectorSources = appMap.getConnectorSources( MONGOS_SERVICE_NAME );
		
		LogUtils.log( "Sources for service [" + MONGOS_SERVICE_NAME + "] are: " + 
				Arrays.toString( mongosConnectorSources.toArray( new String[mongosConnectorSources.size()] ) ) );
		LogUtils.log( "Targets for service [" + MONGOS_SERVICE_NAME + "] are: " + 
				Arrays.toString( mongosConnectorTargets.toArray( new String[mongosConnectorTargets.size()] ) ) );
		
		assertEquals( "[" + MONGOS_SERVICE_NAME + "] connector tagets must be 1", mongosConnectorTargets.size(), 2 );
		assertEquals( "[" + MONGOS_SERVICE_NAME + "] connector sources must be 1", mongosConnectorSources.size(), 1 );

		//TODO CHANGE CONNECTORS TESTS		
        // lets filter only the connections going out of mongos.
		/*
        Predicate predicate = new Predicate() {
            @Override
            public boolean evaluate( Object o ) {
                return !applicationNodeMongos.getName().equalsIgnoreCase( ( ( Connector ) o ).getSource().getName() );
            }
        };

        // filter only connections going out of mongos
        CollectionUtils.filter( mongosConnectors, predicate );

*/
        ServicesTab servicesTab = mainNav.switchToServices();
		
		takeScreenShot(this.getClass(), "petClinicDemoTest","passed-services");
		
		assertPetclinicPageExists();
		
		uninstallApplication(PETCLINIC_APPLICATION_NAME, true);
		
	}
	
	private void assertPetclinicPageExists() {
		
		Machine localMachine = admin.getMachines().getMachines()[0];
		
		WebClient client = new WebClient(BrowserVersion.getDefault());
		
        HtmlPage page = null;
        try {
            page = client.getPage("http://" + localMachine.getHostAddress() + ":8090");
        } catch (IOException e) {
            fail("Could not get a resposne from the petclinic URL " + e.getMessage());
        }
        assertEquals("OK", page.getWebResponse().getStatusMessage());
		
		
	}

}
