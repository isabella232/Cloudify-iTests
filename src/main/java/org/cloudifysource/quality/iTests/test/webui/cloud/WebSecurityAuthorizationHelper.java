package org.cloudifysource.quality.iTests.test.webui.cloud;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import iTests.framework.utils.LogUtils;

import java.util.Set;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.MainNavigation;
import com.gigaspaces.webuitf.dashboard.DashboardTab;
import com.gigaspaces.webuitf.dashboard.ServicesGrid;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.ApplicationServicesGrid;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.ApplicationServicesGrid.StatefullModule;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.ApplicationServicesGrid.StatelessModule;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.ApplicationServicesGrid.WebModule;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.ApplicationServicesGrid.WebServerModule;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.ApplicationsMenuPanel;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.InfrastructureServicesGrid;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.InfrastructureServicesGrid.ESMInst;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.InfrastructureServicesGrid.GSAInst;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.InfrastructureServicesGrid.GSCInst;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.InfrastructureServicesGrid.GSMInst;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.InfrastructureServicesGrid.Hosts;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.InfrastructureServicesGrid.LUSInst;
import com.gigaspaces.webuitf.services.HostsAndServicesGrid;
import com.gigaspaces.webuitf.services.ServicesTab;
import com.gigaspaces.webuitf.topology.TopologyTab;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationMap;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationNode;
import com.gigaspaces.webuitf.util.AjaxUtils;

public class WebSecurityAuthorizationHelper {
	
	private static long assertWaitingTime = 10 * 1000;
	private static final int SLEEP_TIMEOUT = 10 * 1000;

	public static MainNavigation performLoginAndAllViewsTests( LoginPage loginPage, String username, String password, 
			PermittedServicesWrapper permittedServicesWrapper ) throws Exception{

		logStartValidationForUser( username );

		loginPage.inputUsernameAndPassword( username, password );

		MainNavigation mainNav = loginPage.login();

		boolean invokeSleep = false;
		
		if( permittedServicesWrapper.getEsmCount() == 0 ||
			permittedServicesWrapper.getGsaCount() == 0	||
			permittedServicesWrapper.getGscCount() == 0	||
			permittedServicesWrapper.getGsmCount() == 0	||
			permittedServicesWrapper.getLusCount() == 0 ||
			permittedServicesWrapper.getHostsCount() == 0 ){
			
			invokeSleep = true;
		}

		DashboardTab dashboardTab = mainNav.switchToDashboard();

		//let to panel time to be initialized if one of expected values is 0
		if( invokeSleep ){
			AbstractTestSupport.sleep( SLEEP_TIMEOUT );
		}
		checkDashboard( dashboardTab, permittedServicesWrapper );  

		TopologyTab topologyTab = mainNav.switchToTopology();				
		
		//let to panel time to be initialized if one of expected values is 0
		if( invokeSleep ){
			AbstractTestSupport.sleep( SLEEP_TIMEOUT );
		}
		checkApplicationsTab( topologyTab, permittedServicesWrapper );

		ServicesTab servicesTab = mainNav.switchToServices();

		//let to panel time to be initialized if one of expected values is 0
		if( invokeSleep ){
			AbstractTestSupport.sleep( SLEEP_TIMEOUT );
		}		
		checkServicesTab( servicesTab, permittedServicesWrapper ); 		

		logValidationCompletedForUser(username);
		
		return mainNav;
	}

	private static void checkServicesTab(ServicesTab servicesTab, PermittedServicesWrapper permittedServicesWrapper) {
		LogUtils.log("Testing the services tab");

		HostsAndServicesGrid hostAndServicesGrid = servicesTab.getHostAndServicesGrid();

		checkServicesHostsView(hostAndServicesGrid, permittedServicesWrapper);

		LogUtils.log("services tab is OK");		
	}



	private static void checkServicesHostsView( HostsAndServicesGrid hostAndServicesGrid, 
			PermittedServicesWrapper permittedServicesWrapper ) {

//		hostAndServicesGrid.countNumberOf(  );
	}

	/*
	private static void checkServicesProcessingUnitsView( PuTreeGrid servicesTree, 
			PermittedServicesWrapper permittedServicesWrapper ) {

		Map<String,Integer> services= permittedServicesWrapper.getServices();
		LogUtils.log( "> expected services for user [" + permittedServicesWrapper.getUserName() + "] are:" + services );

		Set<String> keySet = services.keySet();

		for( String serviceName : keySet ){
			WebUIProcessingUnit processingUnit = servicesTree.getProcessingUnit( serviceName );
			LogUtils.log( "> processingUnit object for service [" + serviceName + "] is:" + processingUnit );
			AssertUtils.assertNotNull( "Processing Unit [" + serviceName + "] is not displayed", processingUnit );
			if( processingUnit != null ){

				LogUtils.log( "> actual instances count for service [" + serviceName + "] is:" + 
								processingUnit.getActualInstances() + 
								", expected number is:" + services.get( serviceName ) );
				AssertUtils.assertEquals( "> actual instances count for service [" + serviceName + "] is:" + 
							processingUnit.getActualInstances() +  ", expected number is:" + 
							services.get( serviceName ), processingUnit.getActualInstances(), 
							services.get( serviceName ) );	
			}
		}
	}
*/

	@SuppressWarnings("unchecked")
	private static void checkApplicationsTab( TopologyTab topologyTab, 
											PermittedServicesWrapper permittedServicesWrapper ) {
		
//		LogUtils.log("Testing the application tab");
//
//
//
//		LogUtils.log("application tab is OK");
	}


	private static void checkDashboard( DashboardTab dashboardTab, PermittedServicesWrapper permittedServicesWrapper) {

		LogUtils.log("Testing services dashboard tab");

		ServicesGrid servicesGrid = dashboardTab.getServicesGrid();

		ApplicationsMenuPanel applicationsMenuPanel = servicesGrid.getApplicationsMenuPanel();
		checkDashboardApplicationsMenu( applicationsMenuPanel, permittedServicesWrapper );

		InfrastructureServicesGrid infrastructureGrid = servicesGrid.getInfrastructureGrid();
		checkDashboardInfrastructure( infrastructureGrid, permittedServicesWrapper );

		ApplicationServicesGrid applicationServicesGrid = servicesGrid.getApplicationServicesGrid();
		checkDashboardApplicationServices( applicationServicesGrid, permittedServicesWrapper );

		LogUtils.log("dashboard tab is OK");
	}

	private static void checkDashboardApplicationsMenu( ApplicationsMenuPanel applicationsMenuPanel,
			PermittedServicesWrapper permittedServicesWrapper) {

		Set<String> applicationNames = permittedServicesWrapper.getApplicationNames();

		LogUtils.log( "> expected application names=" + applicationNames );

		for( String appName : applicationNames ){
			AssertUtils.assertTrue( "Application [" + appName + "] must be displayed for user [" + 
					permittedServicesWrapper.getUserName() + "]",
					applicationsMenuPanel.isApplicationPresent( appName ) );
		}
		
		
		//TBD check displayed application list if redundant application not displayed there
	}


	private static void checkDashboardInfrastructure( InfrastructureServicesGrid infrastructureGrid, 
			final PermittedServicesWrapper permittedServicesWrapper ){

		final ESMInst esmInst = infrastructureGrid.getESMInst();
		final GSAInst gsaInst = infrastructureGrid.getGSAInst();
		final GSCInst gscInst = infrastructureGrid.getGSCInst();
		final GSMInst gsmInst = infrastructureGrid.getGSMInst();
		final LUSInst lusInst = infrastructureGrid.getLUSInst();
		final Hosts hosts = infrastructureGrid.getHosts();

        final int expectedEsmCount = permittedServicesWrapper.getEsmCount();
		final int expectedGsaCount = permittedServicesWrapper.getGsaCount();
		final int expectedGscCount = permittedServicesWrapper.getGscCount();
		final int expectedGsmCount = permittedServicesWrapper.getGsmCount();
		final int expectedLusCount = permittedServicesWrapper.getLusCount();
		final int expectedHostsCount = permittedServicesWrapper.getHostsCount();

        AjaxUtils.repetitiveAssertTrue("Actual esm count is different from expected value [" +
        		expectedEsmCount + "]",
                new com.gigaspaces.webuitf.util.RepetitiveConditionProvider() {
                    @Override
                    public boolean getCondition() {
                    	int displayedEsmCount = esmInst.getCount();
                    	LogUtils.log( "In esm condition, displayedEsmCount=" + displayedEsmCount + ",expectedEsmCount=" + expectedEsmCount );
                    	return expectedEsmCount == displayedEsmCount;
                    }
        }, assertWaitingTime );
        
		
        AjaxUtils.repetitiveAssertTrue("Actual gsa count is different from expected value [" +
        	expectedGsaCount + "]", new com.gigaspaces.webuitf.util.RepetitiveConditionProvider() {
                @Override
                public boolean getCondition() {
                	int displayedGsaCount = gsaInst.getCount();
                	LogUtils.log( "In gsa condition, displayedGsaCount=" + displayedGsaCount + ",expectedGsaCount=" + expectedGsaCount );
                	return expectedGsaCount == displayedGsaCount;
                }
        }, assertWaitingTime );

        
        AjaxUtils.repetitiveAssertTrue("Actual gsc count is different from expected value [" +
            	expectedGscCount + "]", new com.gigaspaces.webuitf.util.RepetitiveConditionProvider() {
                    @Override
                    public boolean getCondition() {
                    	int displayedGscCount = gscInst.getCount();
                    	LogUtils.log( "In gsc condition, displayedGscCount=" + displayedGscCount + ",expectedGscCount=" + expectedGscCount );                    	
                    	return expectedGscCount == displayedGscCount;
                    }
            }, assertWaitingTime );

        
        AjaxUtils.repetitiveAssertTrue("Actual gsm count is different from expected value [" +
            	expectedGsmCount + "]", new com.gigaspaces.webuitf.util.RepetitiveConditionProvider() {
                    @Override
                    public boolean getCondition() {
                    	int displayedGsmCount = gsmInst.getCount();
                    	LogUtils.log( "In gsm condition, displayedGsmCount=" + displayedGsmCount + ",expectedGsmCount=" + expectedGsmCount );
                    	return expectedGsmCount == displayedGsmCount;
                    }
            }, assertWaitingTime );        

        AjaxUtils.repetitiveAssertTrue("Actual lus count is different from expected value [" +
            	expectedLusCount + "]", new com.gigaspaces.webuitf.util.RepetitiveConditionProvider() {
                    @Override
                    public boolean getCondition() {
                    	int displayedLusCount = lusInst.getCount();
                    	LogUtils.log( "In lus condition, displayedLusCount=" + displayedLusCount + ",expectedLusCount=" + expectedLusCount );
                    	return expectedLusCount == displayedLusCount;
                    }
            }, assertWaitingTime );               

        
        AjaxUtils.repetitiveAssertTrue("Actual hosts count is different from expected value [" +
            	expectedHostsCount + "]", new com.gigaspaces.webuitf.util.RepetitiveConditionProvider() {
                    @Override
                    public boolean getCondition() {
                    	int displayedHostsCount = hosts.getCount();
                    	LogUtils.log( "In hosts condition, displayedHostsCount=" + displayedHostsCount + ",expectedHostsCount=" + expectedHostsCount );
                    	return expectedHostsCount == displayedHostsCount;
                    }
            }, assertWaitingTime );           
	}

	private static void checkDashboardApplicationServices( ApplicationServicesGrid applicationServicesGrid, 
			PermittedServicesWrapper permittedServicesWrapper ){

		StatefullModule statefullModule = applicationServicesGrid.getStatefullModule();
		StatelessModule statelessModule = applicationServicesGrid.getStatelessModule();
		WebModule webModule = applicationServicesGrid.getWebModule();
		WebServerModule webServerModule = applicationServicesGrid.getWebServerModule();		

		int displayedStatefulCount = statefullModule.getCount();
		int displayedStatelessCount = statelessModule.getCount();
		int displayedWebModuleCount = webModule.getCount();
		int displayedWebServerCount = webServerModule.getCount();

		int expectedStatefulPuCount = permittedServicesWrapper.getStatefulPuCount();
		int expectedStatelessPuCount = permittedServicesWrapper.getStatelessPuCount();
		int expectedWebModuleCount = permittedServicesWrapper.getWebModuleCount();
		int expectedWebServerCount = permittedServicesWrapper.getWebServerCount();

		LogUtils.log( "> displayedStatefulCount=" + displayedStatefulCount + 
				", expectedStatefulPuCount=" + expectedStatefulPuCount );
		LogUtils.log( "> displayedStatelessCount=" + displayedStatelessCount + 
				", expectedStatelessPuCount=" + expectedStatelessPuCount );
		LogUtils.log( "> displayedWebModuleCount=" + displayedWebModuleCount + 
				", expectedWebModuleCount=" + expectedWebModuleCount );
		LogUtils.log( "> displayedWebServerCount=" + displayedWebServerCount + 
				", expectedWebServerCount=" + expectedWebServerCount );

		AssertUtils.assertEquals( "Actual stateful pu count is different from expected value, displayed [" + 
				displayedStatefulCount + "], expected [" + expectedStatefulPuCount + "]", 
				expectedStatefulPuCount, displayedStatefulCount );

		AssertUtils.assertEquals( "Actual stateless pu count is different from expected value, displayed [" + 
				displayedStatelessCount + "], expected [" + expectedStatelessPuCount + "]", 
				expectedStatelessPuCount, displayedStatelessCount );

		AssertUtils.assertEquals( "Actual web module count is different from expected value, displayed [" + 
				displayedWebModuleCount + "], expected [" + expectedWebModuleCount + "]", 
				expectedWebModuleCount, displayedWebModuleCount );

		AssertUtils.assertEquals( "Actual web server count is different from expected value, displayed [" + 
				displayedWebServerCount + "], expected [" + expectedWebServerCount + "]", 
				expectedWebServerCount, displayedWebServerCount );		
	}

	private static void tryToGetApplicationNode( final ApplicationMap appMap ,final String name ){
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				ApplicationNode simple = appMap.getApplicationNode(name);
				return simple != null;
			}
		};

		AssertUtils.repetitiveAssertTrue("could not find application node after 10 seconds", condition, assertWaitingTime);
	}
	
	private static void logStartValidationForUser( String userAndPassword ){
		LogUtils.log( "~~~~~~  Checking services availability for user [" + userAndPassword +"]" );
	}
	
	private static void logValidationCompletedForUser( String userAndPassword ){
		LogUtils.log( "Completed checking services availability for user [" + userAndPassword +"]" );
	}
}