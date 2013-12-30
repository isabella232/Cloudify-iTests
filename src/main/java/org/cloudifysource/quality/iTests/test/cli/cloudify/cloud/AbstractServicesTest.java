package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestException;
import org.testng.annotations.AfterMethod;

import com.j_spaces.kernel.PlatformVersion;

public abstract class AbstractServicesTest extends NewAbstractCloudTest {

    public static final long ASSERT_TIMEOUT_MILLI = 10 * 1000;
    private static final String STATUS_PROPERTY = "DeclaringClass-Enumerator";
    private static final int DEFAULT_INSTALLATION_TIMEOUT = 50;
	private static String serviceName;

	@AfterMethod
	public void cleanup() throws IOException, InterruptedException {
		super.uninstallServiceIfFound(serviceName);
		super.scanForLeakedAgentNodes();
	}


	public void testService(String serviceFolderPath, String overrideServiceName) throws Exception {
		testService(serviceFolderPath, overrideServiceName, DEFAULT_INSTALLATION_TIMEOUT);
	}

 	public void testService(String serviceFolderPath, String overrideServiceName,
                            final int timeoutMins) throws Exception {
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
		final GSRestClient client = new GSRestClient("", "", new URL(restUrl), PlatformVersion.getVersionNumber());
        LogUtils.log("Querying status of service " + serviceName + " from rest");

        final StringBuilder serviceStatus = new StringBuilder();

        AssertUtils.repetitiveAssertTrue("expected:<intact> but was:<" + serviceStatus.toString() + ">", new AssertUtils.RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
                try {
                    serviceStatus.delete(0, serviceStatus.length());
                    serviceStatus.append(client.getAdminData("ProcessingUnits/Names/default." + serviceName + "/Status").get(STATUS_PROPERTY));
                } catch (RestException e) {
                    e.printStackTrace();
                }
                return "intact".equalsIgnoreCase(serviceStatus.toString());
            }
        }, ASSERT_TIMEOUT_MILLI);
		uninstallServiceAndWait(serviceName);
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
}
