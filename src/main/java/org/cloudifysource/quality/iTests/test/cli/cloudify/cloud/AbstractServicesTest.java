package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestException;
import org.testng.annotations.AfterMethod;

import com.j_spaces.kernel.PlatformVersion;

public abstract class AbstractServicesTest extends NewAbstractCloudTest {

	private static final String STATUS_PROPERTY = "DeclaringClass-Enumerator";

	private static String serviceName;

	@AfterMethod
	public void cleanup() throws IOException, InterruptedException {
		super.uninstallServiceIfFound(serviceName);
		super.scanForLeakedAgentNodes();
	}


	public void testService(String serviceFolderPath, String overrideServiceName) throws IOException, InterruptedException, RestException, PackagingException, DSLException{
		testService(serviceFolderPath, overrideServiceName, 10);
	}

 	public void testService(String serviceFolderPath, String overrideServiceName, final int timeoutMins) throws IOException, InterruptedException, RestException, PackagingException, DSLException{
		LogUtils.log("Reading Service from file : " + serviceFolderPath);
		Service service = ServiceReader.readService(new File(serviceFolderPath));
		LogUtils.log("Succesfully read Service : " + service);

		serviceName = service.getName();

		if (overrideServiceName != null) {
			LogUtils.log("Overriding service name with " + overrideServiceName);
			serviceName = overrideServiceName;
		}


        installServiceAndWait(serviceFolderPath, serviceName, timeoutMins);
 		String restUrl = getRestUrl();
		GSRestClient client = new GSRestClient("", "", new URL(restUrl), PlatformVersion.getVersionNumber());
        LogUtils.log("Querying status of service " + serviceName + " from rest");
		Map<String, Object> entriesJsonMap  = client.getAdminData("ProcessingUnits/Names/default." + serviceName + "/Status");
		String serviceStatus = (String)entriesJsonMap.get(STATUS_PROPERTY);

		AssertUtils.assertEquals("intact", serviceStatus.toLowerCase());

		uninstallServiceAndWait(serviceName);
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
}
