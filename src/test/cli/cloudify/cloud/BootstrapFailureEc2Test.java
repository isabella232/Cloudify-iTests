package test.cli.cloudify.cloud;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.jclouds.compute.domain.ComputeMetadata;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.LogUtils;
import test.cli.cloudify.CloudTestUtils;
import test.cli.cloudify.cloud.AbstractCloudTest;
import test.cli.cloudify.cloud.services.ec2.Ec2CloudService;

public class BootstrapFailureEc2Test extends AbstractCloudTest{

	private Ec2CloudService service;

	@BeforeMethod(enabled = false)
	public void bootstrap() throws IOException, InterruptedException {	
		service = new Ec2CloudService();
		service.setAdditionalPropsToReplace(new HashMap<String, String>());
		service.setMachinePrefix(this.getClass().getName() + CloudTestUtils.SGTEST_MACHINE_PREFIX);	
	}
	
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = false)
	public void installTest() throws IOException, InterruptedException{
		
//		try {
//			service.bootstrapCloud();
//		} catch (AssertionError ae) {
//			super.setService(service);
//		}
		
		Set<? extends ComputeMetadata> nodes = Ec2Utils.getAllNodes();
		for(ComputeMetadata n : nodes){
			System.out.println(n.getName() + " = " + n.getId());
		}
	}

	@AfterMethod(enabled = false)
	public void teardown() throws IOException {
		try {
//			service.teardownCloud();
		}
		catch (Throwable e) {
			LogUtils.log("caught an exception while tearing down ec2", e);
			sendTeardownCloudFailedMail("ec2", e);
		}
		LogUtils.log("restoring original bootstrap-management file");
	}
	
}
