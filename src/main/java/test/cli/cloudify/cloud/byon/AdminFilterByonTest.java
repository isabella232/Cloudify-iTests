package test.cli.cloudify.cloud.byon;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.esm.ElasticServiceManagers;
import org.openspaces.admin.gsa.GridServiceAgents;
import org.openspaces.admin.gsc.GridServiceContainers;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.lus.LookupServices;
import org.openspaces.admin.machine.Machines;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.security.AdminFilter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import framework.utils.AssertUtils;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;


/**
 * This test installs petclinic-simple on BYON after changing the byon-cloud.groovy file to contain machine names instead of IPs.
 * <p>It checks whether the bootstrap and install have succeeded.
 * <p>Note: this test uses 3 machines
 */
public class AdminFilterByonTest extends AbstractByonCloudTest {

	private String applicationName = "petclinic";
	
	private final int expectedGsmAmount = 0;
	private final int expectedEsmAmount = 0;
	private final int expectedLusAmount = 0;
	private final int expectedGsaAmount = 2;
	private final int expectedGscAmount = 2;
	private final int expectedPuAmount = 2;
	private final int expectedMachinesAmount = 2;

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 3, enabled = true)
	public void testPetclinic() throws IOException, InterruptedException{

		installApplicationAndWait(ScriptUtils.getBuildPath() + "/recipes/apps/petclinic-simple", "petclinic");
		assertTrue("petclinic.mongod is not up - install failed", admin.getProcessingUnits().waitFor("petclinic.mongod", OPERATION_TIMEOUT, TimeUnit.MILLISECONDS) != null);
		assertTrue("petclinic.tomcat is not up - install failed", admin.getProcessingUnits().waitFor("petclinic.tomcat", OPERATION_TIMEOUT, TimeUnit.MILLISECONDS) != null);
				
		GridServiceAgents gridServiceAgents = admin.getGridServiceAgents();
		GridServiceContainers gridServiceContainers = admin.getGridServiceContainers();
		GridServiceManagers gridServiceManagers = admin.getGridServiceManagers();
		ElasticServiceManagers elasticServiceManagers = admin.getElasticServiceManagers();
		LookupServices lookupServices = admin.getLookupServices();
		ProcessingUnits processingUnits = admin.getProcessingUnits();
		Machines machines = admin.getMachines();
		
		AssertUtils.assertTrue( "Number of gsa must be " + expectedGsaAmount, gridServiceAgents.waitFor( expectedGsaAmount, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
		AssertUtils.assertTrue( "Number of gsc must be " + expectedGscAmount, gridServiceContainers.waitFor( expectedGscAmount, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
		AssertUtils.assertTrue( "Number of gsm must be " + expectedGsmAmount, gridServiceManagers.waitFor( expectedGsmAmount, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
		AssertUtils.assertTrue( "Number of esm must be " + expectedEsmAmount, elasticServiceManagers.waitFor( expectedEsmAmount, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
		AssertUtils.assertTrue( "Number of lus must be " + expectedLusAmount, lookupServices.waitFor( expectedLusAmount, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
		AssertUtils.assertTrue( "Number of machines must be " + expectedMachinesAmount, machines.waitFor( expectedMachinesAmount, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
		
		LogUtils.log( "Number of gsa:" + gridServiceAgents.getSize() );
		LogUtils.log( "Number of gsc:" + gridServiceContainers.getSize() );
		LogUtils.log( "Number of esm:" + elasticServiceManagers.getSize() );
		LogUtils.log( "Number of gsm:" + gridServiceManagers.getSize() );
		LogUtils.log( "Number of lus:" + lookupServices.getSize() );
		LogUtils.log( "Number of pu:" + processingUnits.getSize() );
		LogUtils.log( "Number of machines:" + machines.getSize() );
		
		assertEquals( "Number of gsa must be " + 
							expectedGsaAmount, expectedGsaAmount, gridServiceAgents.getSize() );
		assertEquals( "Number of gsc must be " + expectedGscAmount, 
							expectedGscAmount, gridServiceContainers.getSize() );
		assertEquals( "Number of machines must be " + expectedMachinesAmount, 
							expectedMachinesAmount, machines.getSize() );
		assertEquals( "Number of esm must be " + expectedEsmAmount, 
							expectedEsmAmount, elasticServiceManagers.getSize() );
		assertEquals( "Number of gsm must be " + expectedGsmAmount, 
							expectedGsmAmount, gridServiceManagers.getSize() );
		assertEquals( "Number of lus must be " + expectedLusAmount, 
							expectedLusAmount, lookupServices.getSize() );
		assertEquals( "Number of pu must be expectedPuAmount", 
							expectedPuAmount, processingUnits.getSize() );
		
		uninstallApplicationAndWait( applicationName );
		
		super.scanForLeakedAgentNodes();
	}
	
	@Override
	protected AdminFactory createAdminFactory() {
		return super.createAdminFactory().adminFilter(new AdminFilterImpl());		
	}
	
	@Override 
	protected boolean isFilteredAdmin() {
		return true;
	}

	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

	/**
	 * Simple implementation of AdminFilter
	 * @author evgenyf
	 *
	 */
	private class AdminFilterImpl implements AdminFilter{

		private static final String GIGASPACES_MODE = "GIGASPACES_MODE"; // see bootstrap-management.sh in SGTest

		@Override
		public boolean acceptJavaVirtualMachine( Map<String,String> envVariables, Map<String,String> sysProperties ){

			LogUtils.log( "> envVariables=" + envVariables + "sysProperties=" + sysProperties);
			String gigaspacesMode = envVariables.get( GIGASPACES_MODE );
			boolean isAuthorized = gigaspacesMode != null && gigaspacesMode.equals( "my-agent" );
			LogUtils.log( "> gigaspacesMode=" + gigaspacesMode + ", isAuthorized=" + isAuthorized );
			return isAuthorized;
		}

		@Override
		public boolean acceptProcessingUnit( Map<String,String> contextProperties ){

			LogUtils.log( ">contextProperties=" + contextProperties );
			String applicationName = contextProperties.get( "com.gs.application" );
			boolean isAuthorized = applicationName == null || !applicationName.equals( "management" ); 

			LogUtils.log( ">applicationName=" + applicationName + ", isAuthorized=" + isAuthorized );

			return isAuthorized;
		}
	}	
}