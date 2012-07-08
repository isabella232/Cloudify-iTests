package test.cli.cloudify.cloud.byon;

import java.io.IOException;

import org.cloudifysource.dsl.internal.DSLException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;





/**
 * This test installs petclinic with 3 different templates on a byon cloud. 
 * <p>for two of the three templates the test injects to byon-cloud.groovy names of machines instead of IPs.
 * <p>It checks that each service was assigned to the correct template, according to byon-cloud.groovy.
 * <p>After the installation completes, the test checks the uninstall and teardown operations.
 * 
 * <p>Note: for two of the three templates the test injects to byon-cloud.groovy names of machines instead of IPs.
 * 
 * <p>Note: this test uses 5 fixed machines - 192.168.9.115,192.168.9.116,192.168.9.120,192.168.9.125,192.168.9.126.
 */
public class MultipleTemplatesWithNamesAsIPsTest extends MultipleMachineTemplatesTest{

	@BeforeClass(enabled = true)
	public void before() throws IOException, InterruptedException, DSLException {
		TEMPLATE_1_IPs = "pc-lab95,pc-lab96";
		TEMPLATE_2_IPs = "192.168.9.120,192.168.9.125";
		TEMPLATE_3_IPs = "pc-lab106";
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true, priority = 1)
	public void testPetclinic() throws Exception {
		TEMPLATE_1_IPs = "192.168.9.115,192.168.9.116";
		TEMPLATE_3_IPs = "192.168.9.126";
		super.testPetclinic();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true, priority = 2)
	public void testPetclinicUninstall(){
		super.testPetclinicUninstall();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true, priority = 3)
	public void testPetclinicTeardownByon(){
		super.testPetclinicTeardownByon();
	}

}
	