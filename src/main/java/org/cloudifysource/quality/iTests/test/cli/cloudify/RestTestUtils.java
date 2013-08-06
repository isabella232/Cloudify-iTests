package org.cloudifysource.quality.iTests.test.cli.cloudify;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
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
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.rest.response.DeploymentEvent;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.cloudifysource.dsl.rest.response.InstallServiceResponse;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.dsl.rest.response.UninstallServiceResponse;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.StringUtils;
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

	/**
	 * 
	 * @param restUrl
	 * @param serviceName
	 * @param serviceDir
	 * @param props
	 * @param params
	 * @param overridesFile
	 * @throws IOException
	 * @throws DSLException
	 * @throws PackagingException
	 */
	public static void installServiceUsingRestApi(final String restUrl, final String serviceName,
			final File serviceDir, final Properties props, final Map<String, String> params,
			final File overridesFile) throws IOException, DSLException, PackagingException {

		// create zip file
		final Service service = ServiceReader.readService(null, serviceDir, null, true, overridesFile);
		File packedFile = Packager.pack(serviceDir, service, new LinkedList<File>());

		// add files to post
		final Map<String, File> filesToPost = new HashMap<String, File>();
		filesToPost.put("file", packedFile);
		filesToPost.put("serviceOverridesFile", overridesFile);
		Properties properties = props;
		if (props == null) {
			properties = createServiceContextProperties(service, serviceName + DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX);
		}
		filesToPost.put("props", storePropertiesInTempFile(properties));

		// execute
		final String uri = restUrl + "/service/applications/default/services/" + serviceName + "/timeout/15"
				+ "?zone=" + serviceName + "&selfHealing=" + Boolean.toString(false);
		InputStream instream = null;
		try {
			final HttpResponse resposne = sendRestPostRequest(uri, filesToPost, params);
			instream = resposne.getEntity().getContent();
			final String responseBody = StringUtils.getStringFromStream(instream);
			final int statusCode = resposne.getStatusLine().getStatusCode();
			Assert.assertEquals("Failed to install service. status code: " + statusCode + ", response body: "
					+ responseBody, 200, statusCode);
		} catch (final Exception e) {
			Assert.fail("Failed to get response from " + uri);
			e.printStackTrace();
		} finally {
			if (instream != null) {
				instream.close();
			}
		}
	}

	public static void installApplicationUsingRestApi(final String restUrl, final String applicationName
			, final File applicationDir, final File applicationOverridesFile)
			throws IOException, DSLException, PackagingException {
		// create application zip file
		final Application application =
				ServiceReader.getApplicationFromFile(applicationDir, applicationOverridesFile).getApplication();
		final File packApplication = Packager.packApplication(application, applicationDir);

		// add files to post
		final Map<String, File> filesToPost = new HashMap<String, File>();
		filesToPost.put("file", packApplication);
		filesToPost.put(CloudifyConstants.APPLICATION_OVERRIDES_FILE_PARAM, applicationOverridesFile);

		// execute
		final String uri = restUrl + "/service/applications/" + applicationName + "/timeout/10";
		InputStream instream = null;
		try {
			final HttpResponse resposne = sendRestPostRequest(uri, filesToPost, null);
			instream = resposne.getEntity().getContent();
			final String responseBody = StringUtils.getStringFromStream(instream);
			final int statusCode = resposne.getStatusLine().getStatusCode();
			Assert.assertEquals("Failed to install service. status code: " + statusCode + ", response body: "
					+ responseBody, 200, statusCode);
		} catch (final Exception e) {
			Assert.fail("Failed to get response from " + uri);
			e.printStackTrace();
		} finally {
			if (instream != null) {
				instream.close();
			}
		}
	}

	private static File storePropertiesInTempFile(final Properties props) throws IOException {
		final File tempFile = File.createTempFile("props", ".tmp");
		Writer writer = null;
		try {
			writer = new FileWriter(tempFile);
			props.store(writer, "");
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
		return tempFile;
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

		File packedFile = null;
		try {
			packedFile = Packager.pack(serviceFolder);
		} catch (final Exception e) {
			AssertJUnit.fail("failed to zip templates folder: " + e.getMessage());
		}
		
		// create rest client and connect
		final RestClient restClient = createAndConnect(restUrl);

		// upload
		final InstallServiceRequest request = new InstallServiceRequest();
		String serviceFolderUploadKey = null;
		try {
			serviceFolderUploadKey = restClient.upload(null, packedFile).getUploadKey();
		} catch (final RestClientException e) {
			Assert.fail("failed to upload service folder [" + serviceFolder.getAbsolutePath()
					+ "], error message: " + e.getMessageFormattedText());
		}
		request.setServiceFolderUploadKey(serviceFolderUploadKey);
		request.setTimeoutInMillis(DateUtils.MILLIS_PER_MINUTE * timeoutInMinutes);
		
		// install service and wait
		InstallServiceResponse installService = null;
		try {
			installService = restClient.installService(applicationName, serviceName, request);
			String deploymentID = installService.getDeploymentID();
			waitForServiceInstall(restUrl, applicationName, serviceName, deploymentID);
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

	private static void waitForServiceInstall(final String restUrl, final String appName, final String serviceName, final String deploymentId)
			throws RestClientException {
		
		final RestClient restClient = createAndConnect(restUrl);

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
	
	private static RestClient createAndConnect(final String restUrl) {
		RestClient restClient = null;
		try {
			restClient = createRestClient(restUrl);
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
	
	private static RestClient createRestClient(final String url)
			throws RestClientException, MalformedURLException {
		final String apiVersion = PlatformVersion.getVersion();
		return new RestClient(new URL(url), "", "", apiVersion);
	}
}
