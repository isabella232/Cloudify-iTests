package org.cloudifysource.quality.iTests.test.webui.security;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.MainNavigation;
import com.gigaspaces.webuitf.dashboard.DashboardTab;
import com.gigaspaces.webuitf.services.ServicesTab;
import com.gigaspaces.webuitf.topology.TopologyTab;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.quality.iTests.framework.utils.LocalCloudBootstrapper;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractSecuredLocalCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants;
import org.cloudifysource.quality.iTests.test.webui.WebuiTestUtils;
import org.cloudifysource.quality.iTests.test.webui.cloud.PermittedServicesWrapper;
import org.cloudifysource.quality.iTests.test.webui.cloud.WebSecurityAuthorizationHelper;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LocalCloudAuthorizationWebLdapTest extends AbstractSecuredLocalCloudTest {

	private static long assertWaitingTime = 10000;
	
	private final static String AUTH_GROUPS_1 = "bezeq";
	private final static String AUTH_GROUPS_2 = "GE";
	
	private final static String APP_NAME_1 = GROOVY_APP_NAME + String.valueOf( 1 ) + "_" + AUTH_GROUPS_1;
	private final static String APP_NAME_2 = GROOVY_APP_NAME + String.valueOf( 2 ) + "_" + AUTH_GROUPS_2;
	
	private DashboardTab dashboardTab;
	private TopologyTab topologyTab;
	private ServicesTab servicesTab;
	private MainNavigation mainNav;	
	
	private WebuiTestUtils webuiHelper;


	@Override
	@BeforeClass
	public void bootstrap() throws Exception {

        final String nonSecuredRestUrl = "http://" + InetAddress.getLocalHost().getHostAddress() + ":" + CloudifyConstants.DEFAULT_REST_PORT;

        if(isNonSecuredRestPortResponding()){
            LogUtils.log("tearing down a previous non-secured bootstrap");
            LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
            bootstrapper.timeoutInMinutes(15);
            bootstrapper.setRestUrl(nonSecuredRestUrl);
            bootstrapper.teardown();
        }

		LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.secured(true).securityFilePath(SecurityConstants.LDAP_SECURITY_FILE_PATH);
		bootstrapper.keystoreFilePath(SecurityConstants.DEFAULT_KEYSTORE_FILE_PATH).keystorePassword(SecurityConstants.DEFAULT_KEYSTORE_PASSWORD);
		super.bootstrap(bootstrapper);
		
		try {
			webuiHelper = new WebuiTestUtils( admin );
		} 
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		} 
		catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}		
	}

	@AfterClass(alwaysRun = true)
	public void cleanup() throws Exception{
		
		super.teardown();
		
		if(webuiHelper != null){
			webuiHelper.close();
		}
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void basicTest() throws Exception {
		LogUtils.log("Starting test - webui localcloud");
		
		LogUtils.log( "Starting installing [" + APP_NAME_1 + "] with authGroups [" + AUTH_GROUPS_1 + "]" );		
		installApplicationAndWait(GROOVY_APP_PATH, APP_NAME_1, TIMEOUT_IN_MINUTES, 
				SecurityConstants.USER_PWD_ALL_ROLES, SecurityConstants.USER_PWD_ALL_ROLES, false, AUTH_GROUPS_1 );
		LogUtils.log( "Installing of [" + APP_NAME_1 + "] completed" );
		
		LogUtils.log( "Starting installing [" + APP_NAME_2 + "] with authGroups [" + AUTH_GROUPS_2 + "]" );		
		installApplicationAndWait(GROOVY_APP_PATH, APP_NAME_2, TIMEOUT_IN_MINUTES, 
				SecurityConstants.USER_PWD_ALL_ROLES, SecurityConstants.USER_PWD_ALL_ROLES, false, AUTH_GROUPS_2 );
		LogUtils.log( "Installing of [" + APP_NAME_2 + "] completed" );
		
		// begin tests on the webui 
		LoginPage loginPage = webuiHelper.getLoginPage();
		
		String userAndPassword = SecurityConstants.USER_PWD_ALL_ROLES;//Superuser
		PermittedServicesWrapper permittedServicesWrapper =
										createPemittedServicesWrapperForAllRoles( userAndPassword );
		WebSecurityAuthorizationHelper.performLoginAndAllViewsTests(
                loginPage, userAndPassword, userAndPassword, permittedServicesWrapper );
		
		loginPage = mainNav.logout();
		userAndPassword = SecurityConstants.USER_PWD_APP_MANAGER;//Dan
		permittedServicesWrapper = createPemittedServicesWrapperForAppManager( userAndPassword );
		WebSecurityAuthorizationHelper.performLoginAndAllViewsTests( 
						loginPage, userAndPassword, userAndPassword, permittedServicesWrapper );
		
		loginPage = mainNav.logout();
		userAndPassword = SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER;//Dana
		permittedServicesWrapper = createPemittedServicesWrapperForCloudAdminAndAppManager( userAndPassword );
		WebSecurityAuthorizationHelper.performLoginAndAllViewsTests( 
						loginPage, userAndPassword, userAndPassword, permittedServicesWrapper );
		
		loginPage = mainNav.logout();
		userAndPassword = SecurityConstants.USER_PWD_NO_ROLE;//Jane
		permittedServicesWrapper = createPemittedServicesWrapperForNoRole( userAndPassword );
		WebSecurityAuthorizationHelper.performLoginAndAllViewsTests( 
						loginPage, userAndPassword, userAndPassword, permittedServicesWrapper );
		
		loginPage = mainNav.logout();
		userAndPassword = SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER;//Don
		permittedServicesWrapper = createPemittedServicesWrapperForManagerAndViewer( userAndPassword );
		WebSecurityAuthorizationHelper.performLoginAndAllViewsTests( 
				loginPage, userAndPassword, userAndPassword, permittedServicesWrapper );
		
		loginPage = mainNav.logout();
		userAndPassword = SecurityConstants.USER_PWD_VIEWER;//John
		permittedServicesWrapper = createPemittedServicesWrapperForViewer( userAndPassword );
		WebSecurityAuthorizationHelper.performLoginAndAllViewsTests( 
				loginPage, userAndPassword, userAndPassword, permittedServicesWrapper );
		
		LogUtils.log("End of test - ec2 webui cloud");
	}
	
	//Superuser
	private static PermittedServicesWrapper createPemittedServicesWrapperForAllRoles( String userName ) {
		
		Set<String> applicationNames = new HashSet<String>(3);
		applicationNames.add(MANAGEMENT_APPLICATION_NAME);
		applicationNames.add(APP_NAME_1);
		applicationNames.add(APP_NAME_2);
		Map<String,Integer> services = new HashMap<String,Integer>(6);
		services.put( "webui", 1 );
		services.put( "rest", 1 );
		services.put( APP_NAME_1 + ".groovy", 2 );
		services.put( APP_NAME_1 + ".groovy2", 2 );
		services.put( APP_NAME_2 + ".groovy", 2 );
		services.put( APP_NAME_2 + ".groovy2", 2 );
		
		Set<String> machineAddresses = new HashSet<String>();
		machineAddresses.add( "localhost" );
		
		final int expectedEsmCount = 1;
		final int expectedGsaCount = 1;
		final int expectedGscCount = 7;
		final int expectedGsmCount = 1;
		final int expectedLusCount = 1;
		final int expectedHostsCount = 1;
		
		final int expectedStatefullPuCount = 1;
		final int expectedStatelessPuCount = 0;
		final int expectedWebModuleCount = 2;
		final int expectedWebServerCount = 4;
		
		PermittedServicesWrapper permittedServicesWrapper = 
				new PermittedServicesWrapper( userName, applicationNames, services, 
						machineAddresses, expectedEsmCount, expectedGsaCount, expectedGscCount, 
						expectedGsmCount, expectedLusCount, expectedHostsCount,
						expectedStatefullPuCount, expectedStatelessPuCount, expectedWebModuleCount,
						expectedWebServerCount );

		return permittedServicesWrapper;
	}
	
	//John
	private static PermittedServicesWrapper createPemittedServicesWrapperForViewer( String userName ) {
		Set<String> applicationNames = new HashSet<String>(2);
		applicationNames.add(MANAGEMENT_APPLICATION_NAME);
		applicationNames.add(APP_NAME_2);
		
		Map<String,Integer> services = new HashMap<String,Integer>(1);
		Set<String> machineAddresses = new HashSet<String>();
		 
		final int expectedEsmCount = 1;
		final int expectedGsaCount = 1;
		final int expectedGscCount = 7;
		final int expectedGsmCount = 1;
		final int expectedLusCount = 1;
		final int expectedHostsCount = 1;
		
		final int expectedStatefullPuCount = 1;
		final int expectedStatelessPuCount = 0;
		final int expectedWebModuleCount = 2;
		final int expectedWebServerCount = 2;
		
		PermittedServicesWrapper permittedServicesWrapper = 
				new PermittedServicesWrapper( userName, applicationNames, services, 
						machineAddresses, expectedEsmCount, expectedGsaCount, expectedGscCount, 
						expectedGsmCount, expectedLusCount, expectedHostsCount,
						expectedStatefullPuCount, expectedStatelessPuCount, expectedWebModuleCount,
						expectedWebServerCount );

		return permittedServicesWrapper;
	}

	//Don
	private static PermittedServicesWrapper createPemittedServicesWrapperForManagerAndViewer( String userName ) {
		Set<String> applicationNames = new HashSet<String>(1);
		applicationNames.add(MANAGEMENT_APPLICATION_NAME);
		
		Map<String,Integer> services = new HashMap<String,Integer>(1);
		Set<String> machineAddresses = new HashSet<String>();

		final int expectedEsmCount = 1;
		final int expectedGsaCount = 1;
		final int expectedGscCount = 7;
		final int expectedGsmCount = 1;
		final int expectedLusCount = 1;
		final int expectedHostsCount = 1;
		
		final int expectedStatefullPuCount = 1;
		final int expectedStatelessPuCount = 0;
		final int expectedWebModuleCount = 2;
		final int expectedWebServerCount = 0;
		
		PermittedServicesWrapper permittedServicesWrapper = 
				new PermittedServicesWrapper( userName, applicationNames, services, machineAddresses, 
						expectedEsmCount, expectedGsaCount, expectedGscCount, expectedGsmCount, 
						expectedLusCount, expectedHostsCount,
						expectedStatefullPuCount, expectedStatelessPuCount, expectedWebModuleCount,
						expectedWebServerCount );

		return permittedServicesWrapper;
	}

	//Jane
	private static PermittedServicesWrapper createPemittedServicesWrapperForNoRole(  String userName ) {
		Set<String> applicationNames = new HashSet<String>(1);
		Map<String,Integer> services = new HashMap<String,Integer>(1);
		Set<String> machineAddresses = new HashSet<String>();
		 
		final int expectedEsmCount = 0;
		final int expectedGsaCount = 0;
		final int expectedGscCount = 0;
		final int expectedGsmCount = 0;
		final int expectedLusCount = 0;
		final int expectedHostsCount = 0;
		
		final int expectedStatefullPuCount = 0;
		final int expectedStatelessPuCount = 0;
		final int expectedWebModuleCount = 0;
		final int expectedWebServerCount = 0;
		
		PermittedServicesWrapper permittedServicesWrapper = 
				new PermittedServicesWrapper( userName, applicationNames, services, machineAddresses, 
						expectedEsmCount, expectedGsaCount, expectedGscCount, expectedGsmCount, 
						expectedLusCount, expectedHostsCount,
						expectedStatefullPuCount, expectedStatelessPuCount, expectedWebModuleCount,
						expectedWebServerCount );

		return permittedServicesWrapper;
	}

	//Dana
	private static PermittedServicesWrapper createPemittedServicesWrapperForCloudAdminAndAppManager( String userName ){

		Set<String> applicationNames = new HashSet<String>(2);
		applicationNames.add(MANAGEMENT_APPLICATION_NAME);
		applicationNames.add(APP_NAME_1);
		
		Map<String,Integer> services = new HashMap<String,Integer>(1);
		Set<String> machineAddresses = new HashSet<String>();
		 
		final int expectedEsmCount = 1;
		final int expectedGsaCount = 1;
		final int expectedGscCount = 7;
		final int expectedGsmCount = 1;
		final int expectedLusCount = 1;
		final int expectedHostsCount = 1;
		
		final int expectedStatefullPuCount = 1;
		final int expectedStatelessPuCount = 0;
		final int expectedWebModuleCount = 2;
		final int expectedWebServerCount = 2;
		
		PermittedServicesWrapper permittedServicesWrapper =  new PermittedServicesWrapper( userName,
					applicationNames, services, machineAddresses, expectedEsmCount, expectedGsaCount, 
					expectedGscCount, expectedGsmCount, expectedLusCount, expectedHostsCount,
					expectedStatefullPuCount, expectedStatelessPuCount, expectedWebModuleCount,
					expectedWebServerCount );

		return permittedServicesWrapper;
	}
	
	//Dan
	private static PermittedServicesWrapper createPemittedServicesWrapperForAppManager( String userName ) {
		Set<String> applicationNames = new HashSet<String>(2);
		applicationNames.add(MANAGEMENT_APPLICATION_NAME);
		applicationNames.add(APP_NAME_2);

		Map<String,Integer> services = new HashMap<String,Integer>(1);
		Set<String> machineAddresses = new HashSet<String>();
		 
		final int expectedEsmCount = 1;
		final int expectedGsaCount = 1;
		final int expectedGscCount = 7;
		final int expectedGsmCount = 1;
		final int expectedLusCount = 1;
		final int expectedHostsCount = 1;
		
		final int expectedStatefullPuCount = 1;
		final int expectedStatelessPuCount = 0;
		final int expectedWebModuleCount = 2;
		final int expectedWebServerCount = 2;
		
		PermittedServicesWrapper permittedServicesWrapper =  new PermittedServicesWrapper( 
					userName, applicationNames, services, machineAddresses, expectedEsmCount, 
					expectedGsaCount, expectedGscCount, expectedGsmCount, expectedLusCount, 
					expectedHostsCount, expectedStatefullPuCount, expectedStatelessPuCount, 
					expectedWebModuleCount, expectedWebServerCount );

		return permittedServicesWrapper;
	}
}