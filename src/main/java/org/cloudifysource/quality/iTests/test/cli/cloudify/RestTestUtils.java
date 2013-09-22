package org.cloudifysource.quality.iTests.test.cli.cloudify;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.commons.lang.time.DateUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyConstants.DeploymentState;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.rest.request.InstallApplicationRequest;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.rest.response.ApplicationDescription;
import org.cloudifysource.dsl.rest.response.DeploymentEvent;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.cloudifysource.dsl.rest.response.InstallApplicationResponse;
import org.cloudifysource.dsl.rest.response.InstallServiceResponse;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.dsl.rest.response.UninstallApplicationResponse;
import org.cloudifysource.dsl.rest.response.UninstallServiceResponse;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.testng.AssertJUnit;

import com.j_spaces.kernel.PlatformVersion;

public class RestTestUtils {

	private static HttpResponse sendRestPostRequest(final String uri, final Map<String, File> filesToPost,
			final Map<String, String> params)
			throws ClientProtocolException, IOException {

		final MultipartEntity reqEntity = new MultipartEntity();
		for (final Entry<String, File> entry : filesToPost.entrySet()) {
			final FileBody fileBody = new FileBody(entry.getValue());
			reqEntity.addPart(entry.getKey(), fileBody);
		}

		if (params != null) {
			for (final Map.Entry<String, String> param : params.entrySet()) {
				reqEntity.addPart(param.getKey(), new StringBody(param.getValue(), Charset.forName("UTF-8")));
			}
		}

		final HttpPost post = new HttpPost(uri);
		post.setEntity(reqEntity);

		final DefaultHttpClient httpClient = new DefaultHttpClient();
		return httpClient.execute(post);
	}

	
	public static InstallApplicationResponse installApplicationUsingNewRestApi(final String restUrl, final String applicationName, 
			final File applicationFolder, final File applicationOverridesFile,
			final String expectedFailureMsg) 
			throws IOException, DSLException, PackagingException {
		// create application zip file
		final Application application =
				ServiceReader.getApplicationFromFile(applicationFolder, applicationOverridesFile).getApplication();
		final File packedAppFolder = Packager.packApplication(application, applicationFolder);
		// create rest client and connect
		RestClient restClient = createAndConnect(restUrl);
		// uploads
		String applicationFolderUploadKey = upload(restClient, packedAppFolder);
		String applicationOverridesUploadKey = upload(restClient, applicationOverridesFile);
		//create the request
		InstallApplicationRequest request = new InstallApplicationRequest();
		request.setApplcationFileUploadKey(applicationFolderUploadKey);
		request.setApplicationOverridesUploadKey(applicationOverridesUploadKey);
		request.setApplicationName(applicationName);
		// install application and wait
		InstallApplicationResponse installationResponse = null;
		try {
			installationResponse = restClient.installApplication(applicationName, request);
			String deploymentID = installationResponse.getDeploymentID();
			waitForApplicationInstallation(restClient, restUrl, applicationName, deploymentID);
		} catch (final RestClientException e) {
			String actualMsg = e.getMessageFormattedText();
			if (expectedFailureMsg != null) {
				Assert.assertTrue("error message " + actualMsg + " doesn't contain the expected failure messgae [" + expectedFailureMsg + "]", 
						actualMsg.contains(expectedFailureMsg));
			} else {
				Assert.fail("failed to install application [" + applicationFolder.getAbsolutePath()
						+ "], error message: " + actualMsg);
			}
		}
		return installationResponse;		
	}

	public static Properties createServiceContextProperties(final Service service, final String serviceGroovyFileName) {
		final Properties contextProperties = new Properties();

		// contextProperties.setProperty("com.gs.application.services",
		// serviceNamesString);
		if (service.getDependsOn() != null) {
			contextProperties.setProperty(
					CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON, service
							.getDependsOn().toString());
		}
		if (service.getType() != null) {
			contextProperties.setProperty(
					CloudifyConstants.CONTEXT_PROPERTY_SERVICE_TYPE,
					service.getType());
		}
		if (service.getIcon() != null) {
			contextProperties.setProperty(
					CloudifyConstants.CONTEXT_PROPERTY_SERVICE_ICON,
					CloudifyConstants.SERVICE_EXTERNAL_FOLDER
							+ service.getIcon());
		}
		if (service.getNetwork() != null) {
			if (service.getNetwork().getProtocolDescription() != null) {
				contextProperties
						.setProperty(
								CloudifyConstants.CONTEXT_PROPERTY_NETWORK_PROTOCOL_DESCRIPTION,
								service.getNetwork().getProtocolDescription());
			}
		}
		if (serviceGroovyFileName != null) {
			contextProperties
					.setProperty(CloudifyConstants.CONTEXT_PROPERTY_SERVICE_FILE_NAME, serviceGroovyFileName);
		}

		contextProperties.setProperty(
				CloudifyConstants.CONTEXT_PROPERTY_ELASTIC,
				Boolean.toString(service.isElastic()));

		return contextProperties;

	}
	
	
	public static InstallServiceResponse installServiceUsingNewRestAPI(final String restUrl, final File serviceFolder,
			final String applicationName, final String serviceName, final int timeoutInMinutes) {
		return installServiceUsingNewRestAPI(restUrl, serviceFolder, applicationName, serviceName, timeoutInMinutes, null);
	}

	public static InstallServiceResponse installServiceUsingNewRestAPI(final String restUrl, final File serviceFolder,
			final String applicationName, final String serviceName, final int timeoutInMinutes, String expectedFailureMsg) {
		return installServiceUsingNewRestAPI(restUrl, serviceFolder, applicationName, serviceName, null, timeoutInMinutes, expectedFailureMsg);
	}
	
	public static InstallServiceResponse installServiceUsingNewRestAPI(final String restUrl, final File serviceFolder,
			final String applicationName, final String serviceName, final File serviceOverridesFile, final int timeoutInMinutes, String expectedFailureMsg) {
		File packedFile = null;
		try {
			// create zip file
			final Service service = ServiceReader.readService(null, serviceFolder, null, true, serviceOverridesFile);
			packedFile = Packager.pack(serviceFolder, service, new LinkedList<File>());
		} catch (final Exception e) {
			AssertJUnit.fail("failed to zip templates folder: " + e.getMessage());
		}
		
		// create rest client and connect
		final RestClient restClient = createAndConnect(restUrl);

		// upload
		final InstallServiceRequest request = new InstallServiceRequest();
		String serviceFolderUploadKey = upload(restClient, packedFile);
		request.setServiceFolderUploadKey(serviceFolderUploadKey);
		String serviceOverridesUploadKey = upload(restClient, serviceOverridesFile);
		request.setServiceOverridesUploadKey(serviceOverridesUploadKey);
		request.setTimeoutInMillis(DateUtils.MILLIS_PER_MINUTE * timeoutInMinutes);
		
		// install service and wait
		InstallServiceResponse installService = null;
		try {
			installService = restClient.installService(applicationName, serviceName, request);
			String deploymentID = installService.getDeploymentID();
			waitForServiceInstallation(restClient, restUrl, applicationName, serviceName, deploymentID);
		} catch (final RestClientException e) {
			String actualMsg = e.getMessageFormattedText();
			if (expectedFailureMsg != null) {
				Assert.assertTrue("error message " + actualMsg + " doesn't contain the expected failure messgae [" + expectedFailureMsg + "]", 
						actualMsg.contains(expectedFailureMsg));
			} else {
				Assert.fail("failed to install service [" + serviceFolder.getAbsolutePath()
						+ "], error message: " + actualMsg);
			}
		}
		return installService;
	}

	private static String upload(final RestClient restClient, final File toUploadFile) {
		if (toUploadFile == null) {
			return null;
		}
		String serviceFolderUploadKey = null;
		try {
			serviceFolderUploadKey = restClient.upload(null, toUploadFile).getUploadKey();
		} catch (final RestClientException e) {
			Assert.fail("failed to upload [" + toUploadFile
					+ "], error message: " + e.getMessageFormattedText());
		}
		return serviceFolderUploadKey;
	}

	public static UninstallServiceResponse uninstallServiceUsingNewRestClient(final String restUrl, final String serviceName, final String deploymentID, final int timeoutInMinutes) {
		// create rest client and connect
		final RestClient restClient = createAndConnect(restUrl);
		// uninstall service and wait
		UninstallServiceResponse uninstallService = null;
		try {
			uninstallService = restClient.uninstallService(CloudifyConstants.DEFAULT_APPLICATION_NAME, serviceName, timeoutInMinutes);
			waitForServiceUninstall(restUrl, CloudifyConstants.DEFAULT_APPLICATION_NAME, serviceName, deploymentID);
		} catch (RestClientException e) {
			Assert.fail("failed to uninstall service [" + serviceName
					+ "], error message: " + e.getMessageFormattedText());
		}
		return uninstallService;
	}
	
	public static UninstallApplicationResponse uninstallApplicationUsingNewRestClient(
			final String restUrl, final String appName, final String deploymentID, final int timeoutInMinutes) {
		// create rest client and connect
		final RestClient restClient = createAndConnect(restUrl);
		// uninstall service and wait
		UninstallApplicationResponse uninstallResposne = null;
		try {
			uninstallResposne = restClient.uninstallApplication(appName, timeoutInMinutes);
			waitForApplicationUninstall(restUrl, appName, deploymentID);
		} catch (RestClientException e) {
			Assert.fail("failed to uninstall application [" + appName
					+ "], error message: " + e.getMessageFormattedText());
		}
		return uninstallResposne;
	}

	private static void waitForServiceInstallation(final RestClient restClient, final String restUrl, final String appName, final String serviceName, final String deploymentId)
			throws RestClientException {
		LogUtils.log("Waiting for service deployment state to be " + DeploymentState.STARTED) ;
		AssertUtils.repetitiveAssertTrue(serviceName + " service failed to deploy", new AssertUtils.RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				try {
					ServiceDescription serviceDescription = restClient.getServiceDescription(appName, serviceName);
					if (serviceDescription != null && serviceDescription.getServiceName().equals(serviceName)) {
						if (DeploymentState.STARTED.equals(serviceDescription.getServiceState())) {
							return true;
						}
						return false;
					}
				} catch (final RestClientException e) {
					LogUtils.log("Failed getting service description with deploymentId " + deploymentId 
							+ " error message: " + e.getMessageFormattedText());
				}
				return false;
			}
		} , AbstractTestSupport.OPERATION_TIMEOUT * 3);
	}
	
	private static void waitForApplicationInstallation(final RestClient restClient, final String restUrl, 
			final String applicationName, final String deploymentID) {

		LogUtils.log("Waiting for service deployment state to be " + DeploymentState.STARTED) ;
		AssertUtils.repetitiveAssertTrue(applicationName + " service failed to deploy", new AssertUtils.RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				try {
					ApplicationDescription appDescription = restClient.getApplicationDescription(applicationName);
					if (appDescription != null && appDescription.getApplicationName().equals(applicationName)) {
						if (DeploymentState.STARTED.equals(appDescription.getApplicationState())) {
							return true;
						}
						return false;
					}
				} catch (final RestClientException e) {
					LogUtils.log("Failed getting service description with deploymentId " + deploymentID 
							+ " error message: " + e.getMessageFormattedText());
				}
				return false;
			}
		} , AbstractTestSupport.OPERATION_TIMEOUT * 3);		
	}
	
	private static void waitForServiceUninstall(final String restUrl, final String appName, final String serviceName, final String deploymentId) {		
		try {
		 final RestClient restClient = createAndConnect(restUrl);
		AssertUtils.repetitiveAssertTrue("uninstall service failed " + serviceName,
				new AssertUtils.RepetitiveConditionProvider() {
					@Override
					public boolean getCondition() {
						try {
							DeploymentEvents deploymentEvents = restClient.getDeploymentEvents(deploymentId, 0, -1);
							List<DeploymentEvent> events = deploymentEvents.getEvents();
							for (DeploymentEvent deploymentEvent : events) {
								String description = deploymentEvent.getDescription();
								if (description.equals(CloudifyConstants.UNDEPLOYED_SUCCESSFULLY_EVENT)) {
									return true;
								}
							}
						} catch (RestClientException e) {
							LogUtils.log("Failed getting deployment events with deploymentId " + deploymentId 
									+ " error message: " + e.getMessageFormattedText());
						}
						return false;
					}
				}, AbstractTestSupport.OPERATION_TIMEOUT * 3);
		} catch (Exception e1) {
			Assert.fail("failed to create GSRestClient with url " + restUrl + " and version " + PlatformVersion.getVersionNumber());
		}
	}
	
	private static void waitForApplicationUninstall(final String restUrl, final String appName, final String deploymentId) {		
		try {
		 final RestClient restClient = createAndConnect(restUrl);
		AssertUtils.repetitiveAssertTrue("uninstall application failed " + appName,
				new AssertUtils.RepetitiveConditionProvider() {
					@Override
					public boolean getCondition() {
						try {
							DeploymentEvents deploymentEvents = restClient.getDeploymentEvents(deploymentId, 0, -1);
							List<DeploymentEvent> events = deploymentEvents.getEvents();
							for (DeploymentEvent deploymentEvent : events) {
								String description = deploymentEvent.getDescription();
								if (description.equals(CloudifyConstants.UNDEPLOYED_SUCCESSFULLY_EVENT)) {
									return true;
								}
							}
						} catch (RestClientException e) {
							LogUtils.log("Failed getting deployment events with deploymentId " + deploymentId 
									+ " error message: " + e.getMessageFormattedText());
						}
						return false;
					}
				}, AbstractTestSupport.OPERATION_TIMEOUT * 3);
		} catch (Exception e1) {
			Assert.fail("failed to create GSRestClient with url " + restUrl + " and version " + PlatformVersion.getVersionNumber());
		}
	}
	
	public static RestClient createAndConnect(final String restUrl) {
		RestClient restClient = null;
		try {
			final String apiVersion = PlatformVersion.getVersion();
			restClient = new RestClient(new URL(restUrl), "", "", apiVersion);
		} catch (final Exception e) {
			Assert.fail("failed to create rest client with url " + restUrl + ", error message: " + e.getMessage());
		}
		try {
			restClient.connect();
		} catch (final RestClientException e) {
			Assert.fail("failed to connect: " + e.getMessageFormattedText());
		}
		return restClient;
	}
}
