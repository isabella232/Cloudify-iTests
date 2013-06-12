package org.cloudifysource.quality.iTests.test.cli.cloudify;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyConstants.DeploymentState;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.rest.response.ApplicationDescription;
import org.cloudifysource.dsl.rest.response.InstallServiceResponse;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.dsl.rest.response.UninstallServiceResponse;
import org.cloudifysource.dsl.rest.response.UploadResponse;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.RestException;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.testng.annotations.Test;

import com.j_spaces.kernel.PlatformVersion;

/**
 * This test uses the rest API to install a simple service.
 * 
 * @author adaml
 *
 */
public class InstallServiceUsingRestClientTest extends AbstractLocalCloudTest {
	
	private static final String SERVICE_FILE_NAME = "simple-service.groovy";
	private static final String SERVICE_NAME = "simple";
	private static final String SERVICE_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/simple");
	private static final int INSTALL_TIMEOUT_MILLIS = 60 * 15 * 1000;

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
    public void testInstallService() 
    		throws IOException, PackagingException, 
    		DSLException, RestClientException, CLIException, RestException {
		final String version = PlatformVersion.getVersion();
		final URL url = new URL(restUrl);
		final RestClient client = new RestClient(url, "", "", version);
        client.connect();
		
		final File serviceFolder = new File(SERVICE_DIR_PATH);
		final File packedFile = Packager.pack(serviceFolder);
		final UploadResponse uploadResponse = client.upload(null, packedFile);
		final String uploadKey = uploadResponse.getUploadKey();
		
		InstallServiceRequest request = new InstallServiceRequest();
		request.setServiceFolderUploadKey(uploadKey);

		//Test will run in unsecured mode.
		request.setAuthGroups("");
		//no debugging.
		request.setDebugAll(false);
		request.setSelfHealing(true);
		request.setServiceFileName(SERVICE_FILE_NAME);
		//set timeout
		request.setTimeoutInMillis(INSTALL_TIMEOUT_MILLIS);
		
		//make install service API call
		InstallServiceResponse installServiceRespone = client.installService(DEFAULT_APPLICATION_NAME, SERVICE_NAME, request);
		assertNotNull("install-service response does not contain deploymentID",  installServiceRespone.getDeploymentID());
		waitForServiceInstall(url);
		UninstallServiceResponse uninstallServiceRespone = client.uninstallService(DEFAULT_APPLICATION_NAME, SERVICE_NAME, 5);
		assertNotNull("uninstall-service response does not contain deploymentID",  uninstallServiceRespone.getDeploymentID());
		waitForServiceUninstall();
		//Now we wait for the USM service state to become RUNNING.
		
		
	}
	
	void waitForServiceUninstall()
			throws MalformedURLException, RestException {
		final GSRestClient client = new GSRestClient("","", new URL(restUrl), PlatformVersion.getVersionNumber());

		AssertUtils.repetitiveAssertTrue("uninstall service failed " + SERVICE_NAME,
				new AssertUtils.RepetitiveConditionProvider() {
					@Override
					public boolean getCondition() {
						try {
							Map<String, Object> adminData = client.getAdminData("zones/Names");
							List<String> names = (List<String>) adminData.get("Names-Elements");
							for (String string : names) {
								if (string.contains(SERVICE_NAME)) {
									return false;
								}
							}
							return true;
						
						} catch (RestException e) {
							// TODO Auto-generated catch block
							LogUtils.log("Failed getting zones list.");
							e.printStackTrace();
						}
						return false;
					}
				}, AbstractTestSupport.OPERATION_TIMEOUT * 3);
	}

	void waitForServiceInstall(final URL url)
			throws CLIException {
		final RestAdminFacade adminFacade = new RestAdminFacade();
		adminFacade.connect(null, null, url.toString(), false);
		
		LogUtils.log("Waiting for application deployment state to be " + DeploymentState.STARTED) ;
		AssertUtils.repetitiveAssertTrue(SERVICE_NAME + " service failed to deploy", new AssertUtils.RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				try {
					ApplicationDescription servicesDescriptionList = adminFacade.getServicesDescriptionList(DEFAULT_APPLICATION_NAME);
					for (ServiceDescription description : servicesDescriptionList.getServicesDescription()) {
						if (description.getServiceName().equals(SERVICE_NAME)) {
							if (description.getServiceState().equals(DeploymentState.STARTED)) {
								return true;
							}
						}
					}
				} catch (final CLIException e) {
					LogUtils.log("Failed getting service description");
				}
				return false;
			}
		} , AbstractTestSupport.OPERATION_TIMEOUT * 3);
	}
}
