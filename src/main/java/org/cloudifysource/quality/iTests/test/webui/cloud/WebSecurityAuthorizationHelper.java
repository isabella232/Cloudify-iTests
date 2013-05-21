package org.cloudifysource.quality.iTests.test.webui.cloud;

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
import com.gigaspaces.webuitf.dashboard.ServicesGrid.InfrastructureServicesGrid.*;
import com.gigaspaces.webuitf.services.HostsAndServicesGrid;
import com.gigaspaces.webuitf.services.PuTreeGrid;
import com.gigaspaces.webuitf.services.PuTreeGrid.WebUIProcessingUnit;
import com.gigaspaces.webuitf.services.ServicesTab;
import com.gigaspaces.webuitf.topology.TopologyTab;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationMap;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationNode;
import com.gigaspaces.webuitf.util.AjaxUtils;
import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import iTests.framework.utils.LogUtils;

import java.util.Map;
import java.util.Set;

public class WebSecurityAuthorizationHelper {
	
	private static long assertWaitingTime = 10000;

	public static MainNavigation performLoginAndAllViewsTests( LoginPage loginPage, String username, String password, 
			PermittedServicesWrapper permittedServicesWrapper ) throws Exception{

		logStartValidationForUser( username );

		loginPage.inputUsernameAndPassword( username, password );

		MainNavigation mainNav = loginPage.login();

		DashboardTab dashboardTab = mainNav.switchToDashboard();

		checkDashboard( dashboardTab, permittedServicesWrapper );  

		TopologyTab topologyTab = mainNav.switchToTopology();				

		checkApplicationsTab( topologyTab, permittedServicesWrapper );

		ServicesTab servicesTab = mainNav.switchToServices();

		checkServicesTab( servicesTab, permittedServicesWrapper ); 		

		logValidationCompletedForUser(username);
		
		return mainNav;
	}

	private static void checkServicesTab(ServicesTab servicesTab, PermittedServicesWrapper permittedServicesWrapper) {
		LogUtils.log("Testing the services tab");

		HostsAndServicesGrid hostAndServicesGrid = servicesTab.getHostAndServicesGrid();
		PuTreeGrid servicesTree = servicesTab.getPuTreeGrid();

		checkServicesHostsView(hostAndServicesGrid, permittedServicesWrapper);
		checkServicesProcessingUnitsView( servicesTree, permittedServicesWrapper);

		LogUtils.log("services tab is OK");		
	}



	private static void checkServicesHostsView( HostsAndServicesGrid hostAndServicesGrid, 
			PermittedServicesWrapper permittedServicesWrapper ) {

//		hostAndServicesGrid.countNumberOf(  );
	}

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
			PermittedServicesWrapper permittedServicesWrapper ){

		final ESMInst esmInst = infrastructureGrid.getESMInst();
		final GSAInst gsaInst = infrastructureGrid.getGSAInst();
		final GSCInst gscInst = infrastructureGrid.getGSCInst();
		final GSMInst gsmInst = infrastructureGrid.getGSMInst();
		final LUSInst lusInst = infrastructureGrid.getLUSInst();
		final Hosts hosts = infrastructureGrid.getHosts();


        // we want to wait for significant values as initially values are all '0', so we use repetitive assert

        AjaxUtils.repetitiveAssertTrue("failed to wait for significant values in infrastructure grid",
                new com.gigaspaces.webuitf.util.RepetitiveConditionProvider() {
                    @Override
                    public boolean getCondition() {
                        return  esmInst.getCount() != 0 &&
                                gsaInst.getCount() != 0 &&
                                gscInst.getCount() != 0 &&
                                gsmInst.getCount() != 0 &&
                                lusInst.getCount() != 0 &&
                                hosts.getCount() != 0;
                    }
                }, 10 * 1000);


        int displayedEsmCount = esmInst.getCount();
        int displayedGsaCount = gsaInst.getCount();
        int displayedGscCount = gscInst.getCount();
        int displayedGsmCount = gsmInst.getCount();
        int displayedLusCount = lusInst.getCount();
        int displayedHostsCount = hosts.getCount();

        int expectedEsmCount = permittedServicesWrapper.getEsmCount();
		int expectedGsaCount = permittedServicesWrapper.getGsaCount();
		int expectedGscCount = permittedServicesWrapper.getGscCount();
		int expectedGsmCount = permittedServicesWrapper.getGsmCount();
		int expectedLusCount = permittedServicesWrapper.getLusCount();
		int expectedHostsCount = permittedServicesWrapper.getHostsCount();

		LogUtils.log( "> displayed esm count=" + displayedEsmCount + 
				", expectedEsmCount=" + expectedEsmCount );
		LogUtils.log( "> displayed gsa count=" + 
				displayedGsaCount + ", expectedGsaCount=" + expectedGsaCount );
		LogUtils.log( "> displayed gsc count=" + 
				displayedGscCount + ", expectedGscCount=" + expectedGscCount );
		LogUtils.log( "> displayed gsm count=" + 
				displayedGsmCount + ", expectedGsmCount=" + expectedGsmCount );
		LogUtils.log( "> displayed lus count=" + 
				displayedLusCount + ", expectedLusCount=" + expectedLusCount );
		LogUtils.log( "> displayed hosts count=" + 
				displayedHostsCount + ", expectedHostsCount=" + expectedHostsCount );

		AssertUtils.assertEquals( "Actual esm count is different from expected value, displayed [" + 
				displayedEsmCount + "], expected [" + expectedEsmCount + "]", 
				expectedEsmCount, displayedEsmCount );

		AssertUtils.assertEquals( "Actual gsa count is different from expected value, displayed [" + 
				displayedGsaCount + "], expected [" + expectedGsaCount + "]", 
				expectedGsaCount, displayedGsaCount );

		AssertUtils.assertEquals( "Actual gsc count is different from expected value, displayed [" + 
				displayedGscCount + "], expected [" + expectedGscCount + "]", 
				expectedGscCount, displayedGscCount );

		AssertUtils.assertEquals( "Actual gsm count is different from expected value, displayed [" + 
				displayedGsmCount + "], expected [" + expectedGsmCount + "]", 
				expectedGsmCount, displayedGsmCount );

		AssertUtils.assertEquals( "Actual lus count is different from expected value, displayed [" + 
				displayedLusCount + "], expected [" + expectedLusCount + "]", 
				expectedLusCount, displayedLusCount );

		AssertUtils.assertEquals( "Actual machines count is different from expected value, displayed [" + 
				displayedHostsCount + "], expected [" + expectedHostsCount + "]", 
				expectedHostsCount, displayedHostsCount );
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