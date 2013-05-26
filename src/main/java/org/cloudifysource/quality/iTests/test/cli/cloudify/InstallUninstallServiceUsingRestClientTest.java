package org.cloudifysource.quality.iTests.test.cli.cloudify;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang.StringUtils;
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
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.j_spaces.kernel.PlatformVersion;


/**
 * This test uses the rest API to install a simple service.
 * 
 * @author adaml, noak
 *
 */
public class InstallUninstallServiceUsingRestClientTest extends AbstractLocalCloudTest {
	
	private static final String SERVICE_FILE_NAME = "groovy-service.groovy";
	private static final String SERVICE_NAME = "groovy";
	private static final String SERVICE_DIR_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/groovy-simple");
	private static final int INSTALL_TIMEOUT_MILLIS = 60 * 15 * 1000;

	private static String oldVersion = PlatformVersion.getVersionNumber();	// for the old GSRestClient, long version format
	private static String newVersion = PlatformVersion.getVersion();		// for the new RestClient, short version format
	
	private URL baseUrl;
	private RestClient restClient;
	private GSRestClient oldRestClient;
	
	@BeforeTest
	public void init() 
	throws MalformedURLException, RestClientException, RestException {
		baseUrl = new URL(restUrl);
		restClient = new RestClient(baseUrl, "", "", newVersion);
		oldRestClient = new GSRestClient("", "", baseUrl, oldVersion);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
    public void testInstallService() 
    		throws RestException, IOException, PackagingException, 
    		DSLException, RestClientException {
		
		final File serviceFolder = new File(SERVICE_DIR_PATH);
		final File packedFile = Packager.pack(serviceFolder);
		final UploadResponse uploadResponse = restClient.upload(null, packedFile);
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
		restClient.installService(DEFAULT_APPLICATION_NAME, SERVICE_NAME, request);
		
		//Now we wait for the USM service state to become RUNNING.
		waitForServiceDeployment();
	}

	/**
	 * Waits until verified the service is deployed
	 */
	void waitForServiceDeployment() throws RestException {

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
					e.printStackTrace();
					//throw new RuntimeException("caught a RestException", e);
				}
				return false;
			}
		} , AbstractTestSupport.OPERATION_TIMEOUT * 3);
	}
	

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, dependsOnMethods= {"testInstallService"})
    public void testUninstallService() 
    		throws RestException, IOException, PackagingException, DSLException, RestClientException {
		
		restClient.uninstallService(DEFAULT_APPLICATION_NAME, SERVICE_NAME, 5/*timeout in minutes*/);
		
		//Now we wait for the USM service state to become RUNNING.
		waitForServiceUndeployment();
	}
	
	
	/**
	 * Waits until verified the service is un-deployed
	 */
	void waitForServiceUndeployment() {

		LogUtils.log("Retrieving list of processing units");
		AssertUtils.repetitiveAssertTrue(SERVICE_NAME + " was not uninstalled", new AssertUtils.RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				final String brokenServiceRestUrl = "ProcessingUnits/Names";
				final String serviceRestUrl = baseUrl + "/admin/" + brokenServiceRestUrl + "/" + DEFAULT_APPLICATION_NAME + "." + SERVICE_NAME;
				try {
					@SuppressWarnings("unchecked")
					List<String> puNames = (List<String>) oldRestClient.getAdminData(brokenServiceRestUrl).get("Names-Elements");
					return (!containsIgnoreCase(puNames, serviceRestUrl));
				} catch (RestException e) {
					throw new RuntimeException("caught a RestException", e);
				}
			}
		} , AbstractTestSupport.OPERATION_TIMEOUT * 3);
	}
	
	
	/**
	 * Checks if a given list of strings contains a specific string, ignoring its case.
	 * @param list list of strings
	 * @param string the searched string
	 * @return boolean. True if found, false otherwise.
	 */
	private static boolean containsIgnoreCase(final List<String> list, String string) {
		
		if (list == null) {
			return false;
		}
		
		boolean isFound = false;
				
		ListIterator<String> listIterator = list.listIterator();
		while (listIterator.hasNext()) {
			String item = listIterator.next();
			if (StringUtils.isNotBlank(item) && item.equalsIgnoreCase(string)) {
				isFound = true;
				break;
			}
		}
		
		return isFound;
	}
	
	
	@Override
	@AfterMethod(alwaysRun = true)
	public void cleanup() throws Exception {
		// no need - will be performed after class
		// this is here to override the super's @AfterMethod implementation
	}

	
	@AfterClass
	public void clean() throws Exception {
		super.cleanup();
	}
	
}
