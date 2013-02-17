package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestException;
import org.testng.annotations.AfterMethod;

import com.j_spaces.kernel.PlatformVersion;

import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.ScriptUtils;

public abstract class AbstractServicesTest extends NewAbstractCloudTest {

	private static final String STATUS_PROPERTY = "DeclaringClass-Enumerator";
	private String recipesDirPath = ScriptUtils.getBuildPath() + "/recipes/services/";

	private String name;
	
	@AfterMethod
	public void cleanup() throws IOException, InterruptedException {
		super.uninstallServiceIfFound(name);
		super.scanForLeakedAgentNodes();
	}
	
	
	public void testService(String serviceFolderName, String serviceName) throws IOException, InterruptedException, RestException{
		this.name = serviceName;
		installServiceAndWait(recipesDirPath + serviceFolderName, serviceName);
		String restUrl = getRestUrl();
		GSRestClient client = new GSRestClient("", "", new URL(restUrl), PlatformVersion.getVersionNumber());
		Map<String, Object> entriesJsonMap  = client.getAdminData("ProcessingUnits/Names/default." + serviceName + "/Status");
		String serviceStatus = (String)entriesJsonMap.get(STATUS_PROPERTY);
		
		AssertUtils.assertTrue("service is not intact", serviceStatus.equalsIgnoreCase("INTACT"));	
		
		uninstallServiceAndWait(serviceName);
	}	
	
	@Override
	protected boolean isReusableCloud() {
		return false;
	}

	@Override
	protected void customizeCloud() {
		
	}
	
}
