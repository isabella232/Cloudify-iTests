package org.cloudifysource.quality.iTests.test.cli.cloudify;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.rest.response.UploadResponse;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.exceptions.RestException;
import org.testng.annotations.Test;

import com.j_spaces.kernel.PlatformVersion;

/**
 * This test uses the rest API to install a simple service.
 * 
 * @author adaml
 *
 */
public class InstallServiceUsingRestClientTest extends AbstractLocalCloudTest {
	
	private static final String SERVICE_NAME = "simple";
	private static final String SERVICE_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simple");
	private static final int INSTALL_TIMEOUT_MILLIS = 60 * 15 * 1000;

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
    public void testInstallService() 
    		throws RestException, IOException, PackagingException, 
    		DSLException, RestClientException {
		final String version = PlatformVersion.getVersion();
		final URL url = new URL(this.restUrl);
		final RestClient client = new RestClient(url, "", "", version);
		
		final File serviceFolder = new File(SERVICE_DIR_PATH);
		final File packedFile = Packager.pack(serviceFolder);
		final UploadResponse uploadResponse = client.upload(packedFile.getName(), packedFile);
		final String uploadKey = uploadResponse.getUploadKey();
		
		InstallServiceRequest request = new InstallServiceRequest();
		request.setServiceFolderUploadKey(uploadKey);

		//Test will run in unsecured mode.
		request.setAuthGroups("");
		//no debugging.
		request.setDebugAll(false);
		request.setSelfHealing(true);
		request.setServiceFileName(packedFile.getName());
		//set timeout
		request.setTimeoutInMillis(INSTALL_TIMEOUT_MILLIS);
		
		//make install service API call
		client.installService(DEFAULT_APPLICATION_NAME, SERVICE_NAME, request);
		
		//Now we wait for the USM service state to become RUNNING.
		waitForServiceDeployment(version, url);
	}

	void waitForServiceDeployment(final String version, final URL url)
			throws RestException {
		final GSRestClient oldRestClient = new GSRestClient("", "", url, version);
		LogUtils.log("Waiting for USM_State to be " + CloudifyConstants.USMState.RUNNING);
		AssertUtils.repetitiveAssertTrue(SERVICE_NAME + " service did not reach USM_State of RUNNING", new AssertUtils.RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				final String brokenServiceRestUrl = "ProcessingUnits/Names/default." + SERVICE_NAME;
				try {
					String usmState = (String) oldRestClient.getAdminData(brokenServiceRestUrl + "/Instances/0/Statistics/Monitors/USM/Monitors/USM_State").get("USM_State");
					LogUtils.log("USMState is " + usmState);
					return (Integer.valueOf(usmState) == CloudifyConstants.USMState.RUNNING.ordinal());
				} catch (RestException e) {
					throw new RuntimeException("caught a RestException", e);
				}
			}
		} , AbstractTestSupport.OPERATION_TIMEOUT * 3);
	}
}
