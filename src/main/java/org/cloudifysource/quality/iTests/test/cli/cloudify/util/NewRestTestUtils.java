/*
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * *****************************************************************************
 */
package org.cloudifysource.quality.iTests.test.cli.cloudify.util;

import com.j_spaces.kernel.PlatformVersion;
import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyConstants.DeploymentState;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.rest.request.InstallApplicationRequest;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.rest.response.ApplicationDescription;
import org.cloudifysource.dsl.rest.response.ControllerDetails;
import org.cloudifysource.dsl.rest.response.DeploymentEvent;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.cloudifysource.dsl.rest.response.GetMachineDumpFileResponse;
import org.cloudifysource.dsl.rest.response.GetMachinesDumpFileResponse;
import org.cloudifysource.dsl.rest.response.GetPUDumpFileResponse;
import org.cloudifysource.dsl.rest.response.InstallApplicationResponse;
import org.cloudifysource.dsl.rest.response.InstallServiceResponse;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.dsl.rest.response.ShutdownManagementResponse;
import org.cloudifysource.dsl.rest.response.UninstallApplicationResponse;
import org.cloudifysource.dsl.rest.response.UninstallServiceResponse;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.util.exceptions.FailedToCreateDumpException;
import org.cloudifysource.quality.iTests.test.cli.cloudify.util.exceptions.WrongMessageException;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.exceptions.CLIException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility class for methods that use the new rest implementation.
 */
public final class NewRestTestUtils {

    /**
     * Avoid instantiation
     */
    private NewRestTestUtils() {

    }

	private static final int POLLING_INTERVAL_MILLISECONDS = 1000;

	public static InstallApplicationResponse installApplicationUsingNewRestApi(
            final String restUrl,
			final String applicationName,
			final File applicationFolder) throws IOException, DSLException, PackagingException, RestClientException,
                                                 WrongMessageException {
		return installApplicationUsingNewRestApi(restUrl, applicationName, applicationFolder, null, null);
	}

	public static InstallApplicationResponse installApplicationUsingNewRestApi(
            final String restUrl,
			final String applicationName,
			final File applicationFolder,
            final File applicationOverridesFile,
			final String expectedFailureMsg) throws IOException, DSLException, PackagingException,
                                                    RestClientException, WrongMessageException {
		// create application zip file
		final Application application =
				ServiceReader.getApplicationFromFile(applicationFolder, applicationOverridesFile).getApplication();
		final File packedAppFolder = Packager.packApplication(application, applicationFolder);
		// create rest client and connect
		final RestClient restClient = NewRestTestUtils.createAndConnect(restUrl);
		// uploads
		final String applicationFolderUploadKey = NewRestTestUtils.upload(restClient, packedAppFolder);
		final String applicationOverridesUploadKey = NewRestTestUtils.upload(restClient, applicationOverridesFile);
		// create the request
		final InstallApplicationRequest request = new InstallApplicationRequest();
		request.setApplcationFileUploadKey(applicationFolderUploadKey);
		request.setApplicationOverridesUploadKey(applicationOverridesUploadKey);
		request.setApplicationName(applicationName);
		// install application and wait
		InstallApplicationResponse installationResponse = null;
		try {
			installationResponse = restClient.installApplication(applicationName, request);
			if (expectedFailureMsg != null) {
				throw new RuntimeException("expectedFailureMsg: " + expectedFailureMsg);
			}
			final String deploymentID = installationResponse.getDeploymentID();
			NewRestTestUtils.waitForApplicationInstallation(restClient, applicationName, deploymentID);
		} catch (final RestClientException e) {
			final String actualMsg = e.getMessageFormattedText();
			if (expectedFailureMsg == null) {
				throw e;
			} else if (!actualMsg.contains(expectedFailureMsg)) {
				// failure was expected, but not this one.
				throw new WrongMessageException(actualMsg, expectedFailureMsg);				
			}
		}
		return installationResponse;
	}

	public static InstallServiceResponse installServiceUsingNewRestAPI(
            final String restUrl,
            final File serviceFolder,
            final String applicationName,
            final String serviceName,
            final File serviceOverridesFile,
            final String expectedFailureMsg) throws DSLException, IOException, PackagingException,
                                                    RestClientException, WrongMessageException {

		final Service service = ServiceReader.readService(null, serviceFolder, null, true, serviceOverridesFile);
        File packedFile = Packager.pack(serviceFolder, service, new LinkedList<File>());

		// create rest client and connect
		final RestClient restClient = NewRestTestUtils.createAndConnect(restUrl);

		// upload
		final InstallServiceRequest request = new InstallServiceRequest();
		final String serviceFolderUploadKey = NewRestTestUtils.upload(restClient, packedFile);
		request.setServiceFolderUploadKey(serviceFolderUploadKey);
		final String serviceOverridesUploadKey = NewRestTestUtils.upload(restClient, serviceOverridesFile);
		request.setServiceOverridesUploadKey(serviceOverridesUploadKey);

		// install service and wait
		InstallServiceResponse installService = null;
		try {
			installService = restClient.installService(applicationName, serviceName, request);
			final String deploymentID = installService.getDeploymentID();
			NewRestTestUtils
					.waitForServiceInstallation(restClient, applicationName, serviceName, deploymentID);
		} catch (final RestClientException e) {
			final String actualMsg = e.getMessageFormattedText();
			if (expectedFailureMsg == null) {
				throw e;
			} else if (!actualMsg.contains(expectedFailureMsg)) {
				// failure was expected, but not this one.
				throw new WrongMessageException(actualMsg, expectedFailureMsg);				
			}
		}
		return installService;
	}

	public static InstallServiceResponse installServiceUsingNewRestAPI(
            final String restUrl,
            final File serviceFolder,
            final String applicationName,
            final String serviceName,
            final String expectedFailureMsg) throws DSLException, WrongMessageException, PackagingException,
                                                   RestClientException, IOException {
		return installServiceUsingNewRestAPI(restUrl, serviceFolder, applicationName, serviceName, null,
                expectedFailureMsg);
	}

	public static InstallServiceResponse installServiceUsingNewRestAPI(
            final String restUrl,
            final File serviceFolder,
            final String applicationName,
            final String serviceName) throws DSLException, WrongMessageException, PackagingException,
                                            RestClientException, IOException {
		return installServiceUsingNewRestAPI(restUrl, serviceFolder, applicationName, serviceName, null);
	}

	public static UninstallServiceResponse uninstallServiceUsingNewRestClient(
            final String restUrl,
			final String serviceName,
            final String deploymentID,
            final int timeoutInMinutes) throws RestClientException, MalformedURLException {

		// create rest client and connect
		final RestClient restClient = NewRestTestUtils.createAndConnect(restUrl);
		// uninstall service and wait
        UninstallServiceResponse uninstallService = restClient.uninstallService(CloudifyConstants
                .DEFAULT_APPLICATION_NAME, serviceName, timeoutInMinutes);
        waitForServiceUninstall(restUrl,
                serviceName, deploymentID);
		return uninstallService;
	}

	public static UninstallApplicationResponse uninstallApplicationUsingNewRestClient(
			final String restUrl,
            final String appName,
            final String deploymentID,
            final int timeoutInMinutes) throws RestClientException, MalformedURLException {
		// create rest client and connect
		final RestClient restClient = NewRestTestUtils.createAndConnect(restUrl);
		// uninstall service and wait
        UninstallApplicationResponse uninstallResposne = restClient.uninstallApplication(appName, timeoutInMinutes);
        waitForApplicationUninstall(restUrl, appName, deploymentID);
		return uninstallResposne;
	}
	
	public static ShutdownManagementResponse shutdownManagers(
            final String restUrl,
            final String expectedFailureMsg,
            final long timeoutMinutes,
            final File managersFile) throws RestClientException, InterruptedException, TimeoutException,
                                            CLIException, IOException, WrongMessageException {
		
		// connect to the REST
		RestClient restClient = createAndConnect(restUrl);
		
		// shutdown the managers
		ShutdownManagementResponse response = null;
		try {
			response = restClient.shutdownManagers();
		} catch (RestClientException e) {
			final String actualMsg = e.getMessageFormattedText();
			if (expectedFailureMsg == null) {
				throw e;
			} else if (!actualMsg.contains(expectedFailureMsg)) {
				// failure was expected, but not this one.
				throw new WrongMessageException(actualMsg, expectedFailureMsg);				
			}
		}
		
		// write managers to file
		if (managersFile != null) {
			final List<ControllerDetails> managers = Arrays.asList(response.getControllers());
			final ObjectMapper mapper = new ObjectMapper();
			final String managersAsString = mapper.writeValueAsString(managers);
			FileUtils.writeStringToFile(managersFile, managersAsString);
		}
		
		// wait for shutdown
		waitForManagersToShutDown(response.getControllers(), new URL(restUrl).getPort(), timeoutMinutes);
		
		return response;
	}

	public static File getProcessingUnitsDumpFile(
            final String restUrl,
            final long fileSizeLimit,
            final String errMessageContain)
            throws IOException, RestClientException, WrongMessageException, FailedToCreateDumpException {

		// connect to the REST
		RestClient restClient = createAndConnect(restUrl);

		// get dump data using REST API
		GetPUDumpFileResponse response;
		try {
			response = restClient.getPUDumpFile(fileSizeLimit);
			if (errMessageContain != null) {
				throw new WrongMessageException("", errMessageContain);
			}
			// write the result data to a temporary file.
			File file = File.createTempFile("dump", ".zip");
			FileUtils.writeByteArrayToFile(file , response.getDumpData());
			return file;
		} catch (RestClientException e) {
			String message = e.getMessageFormattedText();
			if (errMessageContain == null) {
				throw new FailedToCreateDumpException(message);
			} else {
				if  (!message.contains(errMessageContain)) {
					throw new WrongMessageException(message, errMessageContain);
				}
				return null;
			}
		}
	}
	
	public static File getMachineDumpFile(
            final String restUrl,
            final String ip,
			final String processors,
            final long fileZiseLimit,
            final String errMessageContain) throws IOException, RestClientException,
                                                   FailedToCreateDumpException, WrongMessageException {
		// connect to the REST
		RestClient restClient = createAndConnect(restUrl);

		// get dump data using REST API
		GetMachineDumpFileResponse response = null;
		try {
			response = restClient.getMachineDumpFile(ip, fileZiseLimit, processors);
			if (errMessageContain != null) {
				LogUtils.log("RestClientException expected [" + errMessageContain + "]");
			}
			// write the result data to a temporary file.
			File file = File.createTempFile("dump", ".zip");
			FileUtils.writeByteArrayToFile(file , response.getDumpBytes());
			return file;
		} catch (RestClientException e) {
			String message = e.getMessageFormattedText();
			if (errMessageContain == null) {
                throw new FailedToCreateDumpException(message);
			} else {
                if (!message.contains(errMessageContain)) {
                	throw new WrongMessageException(message, errMessageContain);
                }
                return null;
			}
		}
	}

	public static Map<String, File> getMachinesDumpFile(
            final String restUrl,
            final long fileZiseLimit) throws IOException, RestClientException,
                                             WrongMessageException, FailedToCreateDumpException {
		return getMachinesDumpFile(restUrl, "", "", null, fileZiseLimit, null);
	}

	public static Map<String, File> getMachinesDumpFile(
            final String restUrl) throws IOException, RestClientException,
                                         WrongMessageException, FailedToCreateDumpException {
		return getMachinesDumpFile(restUrl, "", "", null, 0, null);
	}

	public static Map<String, File> getMachinesDumpFile(
            final String restUrl,
			final String processors,
            final long fileZiseLimit,
            final String errMessageContain) throws IOException, RestClientException,
                                                   WrongMessageException, FailedToCreateDumpException {
		return getMachinesDumpFile(restUrl, "", "", processors, fileZiseLimit, errMessageContain);
	}
	
	public static Map<String, File> getMachinesDumpFile(
            final String restUrl,
			final String username,
            final String password) throws IOException, RestClientException,
                                         WrongMessageException, FailedToCreateDumpException {
		return getMachinesDumpFile(restUrl, username, password, null, 0, null);
	}

	public static Map<String, File> getMachinesDumpFile(
            final String restUrl,
			final String username,
            final String password,
			final String processors,
            final long fileZiseLimit,
            final String errMessageContain) throws IOException, RestClientException,
                                                   WrongMessageException, FailedToCreateDumpException {

		// connect to the REST
		RestClient restClient = createAndConnect(restUrl, username, password);

		// get dump data using REST API
		GetMachinesDumpFileResponse response;
		try {
			response = restClient.getMachinesDumpFile(processors, fileZiseLimit);
			if (errMessageContain != null) {
				Assert.fail("RestClientException expected [" + errMessageContain + "]");
			}
			// write the result data to a temporary file.
			Map<String, byte[]> dumpBytesPerIP = response.getDumpBytesPerIP();
			Map<String, File> dumpFilesPerIP = new HashMap<String, File>(dumpBytesPerIP.size());
			for (Entry<String, byte[]> entry : dumpBytesPerIP.entrySet()) {
				File file = File.createTempFile("dump", ".zip");
				file.deleteOnExit();
				FileUtils.writeByteArrayToFile(file , entry.getValue());
				dumpFilesPerIP.put(entry.getKey(), file);
			}
			return dumpFilesPerIP;
		} catch (RestClientException e) {
			String message = e.getMessageFormattedText();
			if (errMessageContain == null) {
				throw new FailedToCreateDumpException(message);
			} else {
				if (!message.contains(errMessageContain)) {
					throw new WrongMessageException(message, errMessageContain);
				}
				return null;
			}
		}
	}

	private static void waitForManagersToShutDown(
            final ControllerDetails[] managers,
            final int port,
			final long timeoutMinutes) throws InterruptedException, TimeoutException, CLIException {

		final Set<ControllerDetails> managersStillUp = new HashSet<ControllerDetails>();

        managersStillUp.addAll(Arrays.asList(managers));

		final ConditionLatch conditionLatch = new ConditionLatch()
		    .pollingInterval(POLLING_INTERVAL_MILLISECONDS, TimeUnit.MILLISECONDS)
		    .timeout(timeoutMinutes, TimeUnit.MINUTES)
		    .timeoutErrorMessage(CloudifyErrorMessages.SHUTDOWN_MANAGERS_TIMEOUT.getName());

		conditionLatch.waitFor(new ConditionLatch.Predicate() {

			@Override
			public boolean isDone() throws CLIException, InterruptedException {

				final Iterator<ControllerDetails> iterator = managersStillUp.iterator();
				while (iterator.hasNext()) {
					final ControllerDetails manager = iterator.next();
					final String host =
							manager.isBootstrapToPublicIp() ? manager.getPublicIp() : manager.getPrivateIp();
							if (ServiceUtils.isPortFree(host, port)) {
								iterator.remove();
								LogUtils.log("Manager [" + host + "] is down.");
								if (managersStillUp.isEmpty()) {
									LogUtils.log("All ports are free.");
									return true;
								}
								LogUtils.log(managersStillUp.size() + " managers more to check");
							} else {
								LogUtils.log("Manager [" + host + "] is still up.");
							}
				}
				return false;
			}
		});
	}

	public static void waitForServiceInstallation(
            final RestClient restClient,
            final String appName,
            final String serviceName,
            final String deploymentId) throws RestClientException {

		LogUtils.log("Waiting for service deployment state to be " + DeploymentState.STARTED);
		AssertUtils.repetitiveAssertTrue(serviceName + " service failed to deploy",
				new AssertUtils.RepetitiveConditionProvider() {
					@Override
					public boolean getCondition() {
						try {
							final ServiceDescription serviceDescription =
									restClient.getServiceDescription(appName, serviceName);
							if (serviceDescription != null && serviceDescription.getServiceName().equals(serviceName)) {
                                return DeploymentState.STARTED.equals(serviceDescription.getServiceState());
                            }
						} catch (final RestClientException e) {
							LogUtils.log("Failed getting service description with deploymentId " + deploymentId
									+ " error message: " + e.getMessageFormattedText());
						}
						return false;
					}
				}, AbstractTestSupport.OPERATION_TIMEOUT * 3);
	}

	public static void waitForApplicationInstallation(
            final RestClient restClient,
            final String applicationName,
            final String deploymentID) {

		LogUtils.log("Waiting for service deployment state to be " + DeploymentState.STARTED);
		AssertUtils.repetitiveAssertTrue(applicationName + " service failed to deploy",
				new AssertUtils.RepetitiveConditionProvider() {
					@Override
					public boolean getCondition() {
						try {
							final ApplicationDescription appDescription =
									restClient.getApplicationDescription(applicationName);
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
				}, AbstractTestSupport.OPERATION_TIMEOUT * 3);
	}

	public static void waitForServiceUninstall(
            final String restUrl,
            final String serviceName,
            final String deploymentId) throws RestClientException, MalformedURLException {
			final RestClient restClient = NewRestTestUtils.createAndConnect(restUrl);
        AssertUtils.repetitiveAssertTrue("uninstall service failed " + serviceName,
                new AssertUtils.RepetitiveConditionProvider() {
                    @Override
                    public boolean getCondition() {
                        try {
                            final DeploymentEvents deploymentEvents =
                                    restClient.getDeploymentEvents(deploymentId, 0, -1);
                            final List<DeploymentEvent> events = deploymentEvents.getEvents();
                            for (final DeploymentEvent deploymentEvent : events) {
                                final String description = deploymentEvent.getDescription();
                                if (description.equals(CloudifyConstants.UNDEPLOYED_SUCCESSFULLY_EVENT)) {
                                    return true;
                                }
                            }
                        } catch (final RestClientException e) {
                            LogUtils.log("Failed getting deployment events with deploymentId " + deploymentId
                                    + " error message: " + e.getMessageFormattedText());
                        }
                        return false;
                    }
                },
                AbstractTestSupport.OPERATION_TIMEOUT * 3);
	}

	public static void waitForApplicationUninstall(
            final String restUrl,
            final String appName,
            final String deploymentId) throws RestClientException, MalformedURLException {
			final RestClient restClient = NewRestTestUtils.createAndConnect(restUrl);
        AssertUtils.repetitiveAssertTrue("uninstall application failed " + appName,
                new AssertUtils.RepetitiveConditionProvider() {
                    @Override
                    public boolean getCondition() {
                        try {
                            final DeploymentEvents deploymentEvents =
                                    restClient.getDeploymentEvents(deploymentId, 0, -1);
                            final List<DeploymentEvent> events = deploymentEvents.getEvents();
                            for (final DeploymentEvent deploymentEvent : events) {
                                final String description = deploymentEvent.getDescription();
                                if (description.equals(CloudifyConstants.UNDEPLOYED_SUCCESSFULLY_EVENT)) {
                                    return true;
                                }
                            }
                        } catch (final RestClientException e) {
                            LogUtils.log("Failed getting deployment events with deploymentId " + deploymentId
                                    + " error message: " + e.getMessageFormattedText());
                        }
                        return false;
                    }
                }, AbstractTestSupport.OPERATION_TIMEOUT * 3);
	}

	public static RestClient createAndConnect(final String restUrl, final String username, final String password)
            throws RestClientException, MalformedURLException {
		RestClient restClient = create(restUrl, username, password);
        restClient.connect();
		return restClient;
	}
	
	public static RestClient createAndConnect(final String restUrl) throws RestClientException, MalformedURLException {
		RestClient restClient = create(restUrl, "", "");
        restClient.connect();
		return restClient;
	}
	
	public static RestClient create(final String restUrl, final String username, final String password)
            throws MalformedURLException, RestClientException {
		RestClient restClient = null;
        final String apiVersion = PlatformVersion.getVersion();
        restClient = new RestClient(new URL(restUrl), username, password, apiVersion);
		return restClient;
	}

	static String upload(final RestClient restClient, final File toUploadFile) throws RestClientException {
		if (toUploadFile == null) {
			return null;
		}
		String serviceFolderUploadKey;
        serviceFolderUploadKey = restClient.upload(null, toUploadFile).getUploadKey();
		return serviceFolderUploadKey;
	}

	public static HttpResponse sendGetRequest(final String uri) throws IOException {
		final SystemDefaultHttpClient httpClient = new SystemDefaultHttpClient();
		final HttpGet request = new HttpGet(uri);
        return httpClient.execute(request);
	}

}
