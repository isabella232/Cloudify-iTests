package test.cli.cloudify;

import java.io.IOException;

import org.openspaces.admin.machine.Machine;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.cloudify.dsl.utils.ServiceUtils;
import com.gigaspaces.cloudify.shell.commands.CLIException;

import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class PetClinicApplicationTest extends AbstractLocalCloudTest {
	
	private static final String PETCLINIC_APPLICTION_NAME = "petclinic-mongo";
	Machine[] machines;
	
	@Override
	@BeforeMethod
	public void beforeTest() {
		super.beforeTest();	
		machines = admin.getMachines().getMachines();
		admin.close();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = true)
	public void testPetClinincApplication() throws CLIException {
		
		String applicationDir = ScriptUtils.getBuildPath() + "/examples/petclinic";
		String command = "connect " + this.restUrl + ";" + "install-application " + "--verbose -timeout 25 " + applicationDir;
		try {
			CommandTestUtils.runCommandAndWait(command);
			int currentNumberOfInstances;
			
			currentNumberOfInstances = getProcessingUnitInstanceCount(ServiceUtils.getAbsolutePUName(PETCLINIC_APPLICTION_NAME, "mongod"));
			assertTrue("Expected 2 PU instances. Actual number of instances is " + currentNumberOfInstances, currentNumberOfInstances == 2);
			
			currentNumberOfInstances = getProcessingUnitInstanceCount(ServiceUtils.getAbsolutePUName(PETCLINIC_APPLICTION_NAME, "mongo-cfg"));
			assertTrue("Expected 1 PU instances. Actual number of instances is " + currentNumberOfInstances, currentNumberOfInstances == 1);
			
			currentNumberOfInstances = getProcessingUnitInstanceCount(ServiceUtils.getAbsolutePUName(PETCLINIC_APPLICTION_NAME, "mongos"));
			assertTrue("Expected 1 PU instances. Actual number of instances is " + currentNumberOfInstances,currentNumberOfInstances == 1);

			currentNumberOfInstances = getProcessingUnitInstanceCount(ServiceUtils.getAbsolutePUName(PETCLINIC_APPLICTION_NAME, "tomcat"));
			assertTrue("Expected 1 PU instances. Actual number of instances is " + currentNumberOfInstances, currentNumberOfInstances == 1);
			
		} catch (IOException e) {
			LogUtils.log("bootstrap-localcloud failed", e);
			afterTest();
		} catch (InterruptedException e) {
			LogUtils.log("bootstrap-localcloud failed", e);
			afterTest();
		}
		
	}
	
	@Override
	@AfterMethod
	public void afterTest() {
		try {
			LogUtils.log("tearing down local cloud");
			CommandTestUtils.runCommandAndWait("teardown-localcloud");
		} catch (IOException e) {
			LogUtils.log("teardown-localcloud failed", e);
		} catch (InterruptedException e) {
			LogUtils.log("teardown-localcloud failed", e);
		}
		super.afterTest();	
	}

}
