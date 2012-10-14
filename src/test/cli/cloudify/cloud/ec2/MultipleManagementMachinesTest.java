package test.cli.cloudify.cloud.ec2;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestException;
import org.junit.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.NewAbstractCloudTest;
import test.cli.cloudify.cloud.services.ec2.Ec2CloudService;

import com.j_spaces.kernel.PlatformVersion;

import framework.utils.LogUtils;


public class MultipleManagementMachinesTest extends NewAbstractCloudTest {
	private String restUrl;
	private final String firstSpaceInstanceUrl = "Spaces/SpaceInstances/0";
	private final String secondSpaceInstanceUrl = "Spaces/SpaceInstances/1";
	private final String RestMachinesUrl = "Machines";
	private Ec2CloudService service;

	
	
	@BeforeClass
	public void bootstrap(ITestContext iTestContext) {	
		try {
			super.bootstrap(iTestContext);
		} catch (AssertionError ae) {
			LogUtils.log(ae.getMessage());
		}						
	}
			
		
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void multipleManagementMachinesTest() throws IOException, InterruptedException, RestException {			
		restUrl = getRestUrl();	
		GSRestClient client = new GSRestClient("", "", new URL(this.restUrl), PlatformVersion.getVersionNumber());
		Map<String, Object> entriesJsonMap  = client.getAdminData(RestMachinesUrl);
		Integer numOfManagmentMachines = (Integer)entriesJsonMap.get("Machines-Size");
		// asserting that there are 2 management machines
		Assert.assertEquals("Number of managment machines isn't 2 ",2, numOfManagmentMachines.intValue());
		
		Map<String, Object> firstSpace = client.getAdminData(firstSpaceInstanceUrl);
		Map<String, Object> secondSpace = client.getAdminData(secondSpaceInstanceUrl);
		
		String firstSpaceMode = (String)firstSpace.get("Mode-Enumerator");
		String secondSpaceMode = (String)secondSpace.get("Mode-Enumerator");
		
		// asserting that one space is primary and one is backup
		boolean condition = ((firstSpaceMode.equals("PRIMARY")) && (secondSpaceMode.equals("BACKUP"))) 
							|| ((firstSpaceMode.equals("BACKUP")) && (secondSpaceMode.equals("PRIMARY")));
		
		Assert.assertTrue("there is no one primary and one backup spaces", condition);
		
	}

	@AfterClass
	public void teardown() {		
		super.teardown();					
	}

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

	@Override
	protected void customizeCloud() throws Exception {
		service = (Ec2CloudService) cloud;
		service.setNumberOfManagementMachines(2);

	};
		
}
