package org.cloudifysource.quality.iTests.test.cli.cloudify;

import iTests.framework.utils.LogUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.rest.response.InvokeInstanceCommandResponse;
import org.cloudifysource.dsl.rest.response.InvokeServiceCommandResponse;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.quality.iTests.framework.utils.CommandInvoker;
import org.cloudifysource.quality.iTests.framework.utils.usm.USMTestUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.restclient.InvocationResult;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class InvokeCommandsNewRestTest extends AbstractLocalCloudTest {

	private static final String SERVICE_NAME = "simpleCustomCommandsMultipleInstances";
	private static final String CUSTOM_COMMAND_PRINT = "print";
	private static final String CUSTOM_COMMAND_PARAMS = "params";
	private static final String CUSTOM_COMMAND_CONTEXT = "context";
	private static final String CUSTOM_COMMAND_RUN_SCRIPT = "runScript";
	private static final String CUSTOM_COMMAND_EXCEPTION = "exception";
	private static final String CUSTOM_COMMAND_PRINT_EXPECTED_OUTPUT = "";
	private static final String CUSTOM_COMMAND_PARAMS_EXPECTED_OUTPUT = 
			"Result: this is the custom parameters command. expecting 123: 123";
	private static final String CUSTOM_COMMAND_CONTEXT_EXPECTED_OUTPUT = "Service Dir is:";
	private static final String CUSTOM_COMMAND_RUN_SCRIPT_EXPECTED_OUTPUT = "Result: 2";
	private static final String CUSTOM_COMMAND_EXCEPTION_EXPECTED_OUTPUT = "This is an error test";
	
	//private static final String FIRST_INSTANCE_NAME = "instance #1@127.0.0.1";
	
	CommandInvoker commandInvoker;
	private int totalInstances;
	
	@BeforeTest
	public void init() 
	throws MalformedURLException {
		commandInvoker = new CommandInvoker(restUrl);
		installService();
	}
	
	
	@AfterTest
	public void cleanup() throws IOException, InterruptedException {
		super.uninstallService(SERVICE_NAME);
	}
	
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testPrintCommand() throws Exception {
		final List<String> params = new ArrayList<String>();
		
		LogUtils.log("Checking print command on all instances");
		testCommandInvocationOnService(CUSTOM_COMMAND_PRINT, params, CUSTOM_COMMAND_PRINT_EXPECTED_OUTPUT, false);
		
		LogUtils.log("Starting to check print command by instance id");
		for(int i=1 ; i<= totalInstances ; i++) {
			testCommandInvocationOnInstance(i, CUSTOM_COMMAND_PRINT, params, CUSTOM_COMMAND_PRINT_EXPECTED_OUTPUT, false);
		}
	}
	
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testParamsCommand() throws Exception {
		final List<String> params = new ArrayList<String>();
		params.add("2");
		params.add("3");
		
		LogUtils.log("Checking params command on all instances");
		testCommandInvocationOnService(CUSTOM_COMMAND_PARAMS, params, CUSTOM_COMMAND_PARAMS_EXPECTED_OUTPUT, false);
		
		LogUtils.log("Starting to check params command by instance id");
		for(int i=1 ; i<= totalInstances ; i++) {
			testCommandInvocationOnInstance(i, CUSTOM_COMMAND_PARAMS, params, CUSTOM_COMMAND_PARAMS_EXPECTED_OUTPUT, false);
		}
	}
	
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testContextCommand() throws Exception {
		final List<String> params = new ArrayList<String>();
		
		LogUtils.log("Checking context command on all instances");
		testCommandInvocationOnService(CUSTOM_COMMAND_CONTEXT, params, CUSTOM_COMMAND_CONTEXT_EXPECTED_OUTPUT, false);
		
		LogUtils.log("Starting to check context command by instance id");
		for(int i=1 ; i<= totalInstances ; i++) {
			testCommandInvocationOnInstance(i, CUSTOM_COMMAND_CONTEXT, params, CUSTOM_COMMAND_CONTEXT_EXPECTED_OUTPUT, false);
		}
	}
	
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testRunScriptCommand() throws Exception {
		final List<String> params = new ArrayList<String>();
		
		LogUtils.log("Checking runScript command on all instances");
		testCommandInvocationOnService(CUSTOM_COMMAND_RUN_SCRIPT, params, CUSTOM_COMMAND_RUN_SCRIPT_EXPECTED_OUTPUT, false);
		
		LogUtils.log("Starting to check runScript command by instance id");
		for(int i=1 ; i<= totalInstances ; i++) {
			testCommandInvocationOnInstance(i, CUSTOM_COMMAND_RUN_SCRIPT, params, CUSTOM_COMMAND_RUN_SCRIPT_EXPECTED_OUTPUT, false);
		}
	}
	
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testExceptionCommand() throws Exception {
		final List<String> params = new ArrayList<String>();
		
		LogUtils.log("Checking exception command on all instances");
		testCommandInvocationOnService(CUSTOM_COMMAND_EXCEPTION, params, CUSTOM_COMMAND_EXCEPTION_EXPECTED_OUTPUT, true);
		
		LogUtils.log("Starting to check exception command by instance id");
		for(int i=1 ; i<= totalInstances ; i++) {
			testCommandInvocationOnInstance(i, CUSTOM_COMMAND_EXCEPTION, params, CUSTOM_COMMAND_EXCEPTION_EXPECTED_OUTPUT, true);
		}
	}
	
	
	private void testCommandInvocationOnService(final String commandName, final List<String> params,
			final String expectedOutput, final boolean expectToFail) throws RestClientException {
		
		Map<String, InvocationResult> invocationResults = null;
		
		InvokeServiceCommandResponse invokeResponse = commandInvoker.restInvokeServiceCommand(DEFAULT_APPLICATION_NAME, 
				SERVICE_NAME, commandName, params);
		
		try {
			invocationResults = parseInvocationResults(invokeResponse.getInvocationResultPerInstance());
		} catch (Exception e) {
			buildParsingErrorMessage(DEFAULT_APPLICATION_NAME, SERVICE_NAME, ""/*instanceId*/, commandName, params, 
					e.getMessage());
		}
		
		final List<InvocationResult> resultsList = new ArrayList<InvocationResult>(invocationResults.values());
		Collections.sort(resultsList);
		String successMessages = getAllSuccessMessages(resultsList);
		String failureMessages = getAllFailureMessages(resultsList);
		String commandOutput = successMessages + System.getProperty("line.separator") + failureMessages;
		LogUtils.log("Command results: " + commandOutput);

		
		AbstractTestSupport.assertTrue(buildTestFailureErrorMessage(DEFAULT_APPLICATION_NAME, SERVICE_NAME, 
				""/*instanceId*/, commandName, params, commandOutput), 
				expectToFail ? !StringUtils.isBlank(failureMessages) : StringUtils.isBlank(failureMessages));

		String statusString = "OK from instance #";
		if (expectToFail) {
			statusString = "FAILED from instance #";
		}
		
		for(int i=1 ; i <= totalInstances ; i++) {
			AbstractTestSupport.assertTrue("Custom command \"" + commandName + "\" returned unexpected result from "
					+ "instance #" + i + ": " + commandOutput,
					commandOutput.contains(statusString + i) && commandOutput.contains(expectedOutput));
		}
		
	}
	
	
	private void testCommandInvocationOnInstance(final int instanceId, final String commandName,
			final List<String> params, final String expectedOutput, final boolean expectToFail) throws RestClientException {
				
		InvokeInstanceCommandResponse invokeResponse = commandInvoker.restInvokeInstanceCommand(DEFAULT_APPLICATION_NAME, 
				SERVICE_NAME, instanceId, commandName, params);
		
		InvocationResult result = parseInvocationResult(invokeResponse.getInvocationResult());
		
		String commandOutput;
		if (result.isSuccess()) {
			commandOutput = getSuccessMessage(result);
		} else {
			commandOutput = getFailureMessage(result);
		}

		LogUtils.log("Command result: " + commandOutput);
		
		AbstractTestSupport.assertTrue(buildTestFailureErrorMessage(DEFAULT_APPLICATION_NAME, SERVICE_NAME, 
				""/*instanceId*/, commandName, params, commandOutput), expectToFail ? !result.isSuccess() : result.isSuccess());
		
		String statusString = "OK from instance #" + instanceId;
		if (expectToFail) {
			statusString = "FAILED from instance #" + instanceId;
		}
		
		AbstractTestSupport.assertTrue("Custom command \"" + commandName + "\" returned unexpected result from "
				+ "instance #" + instanceId  + ": " + commandOutput,
				commandOutput.contains(statusString) && commandOutput.contains(expectedOutput));
		
		// verify there are not printouts of other instances
		for(int i = 1 ; i <= totalInstances ; i++){
			if(i != instanceId) {
				// this means a the command was executed on the wrong instance
				Assert.assertFalse("Custom command \"" + commandName + "\" should not recive any output from instance #"
						+ i ,commandOutput.contains("instance #" + i));
			}
		}
	}
	
	
	private void installService() {
		installService(SERVICE_NAME);
		final String absolutePUName = ServiceUtils.getAbsolutePUName(DEFAULT_APPLICATION_NAME, SERVICE_NAME);
		final ProcessingUnit pu = admin.getProcessingUnits().waitFor(absolutePUName , AbstractTestSupport.OPERATION_TIMEOUT , TimeUnit.MILLISECONDS);
		AbstractTestSupport.assertTrue("USM Service State is NOT RUNNING", USMTestUtils.waitForPuRunningState(absolutePUName, AbstractTestSupport.OPERATION_TIMEOUT, TimeUnit.MILLISECONDS, admin));
		totalInstances = pu.getTotalNumberOfInstances();
	}
	
	
	private void uninstallService() throws IOException, InterruptedException {
		super.uninstallService(SERVICE_NAME);
	}
	
	
	
	private Map<String, InvocationResult> parseInvocationResults(
			final Map<String, Object> restInvocationResultsPerInstance) {

		final Map<String, InvocationResult> invocationResultsMap = new LinkedHashMap<String, InvocationResult>();
		
		for (final Map.Entry<String, Object> entry : restInvocationResultsPerInstance.entrySet()) {
			final String instanceName = entry.getKey();
			final Object restInvocationResult = entry.getValue();

			if (restInvocationResult == null || !(restInvocationResult instanceof Map<?, ?>)) {
				throw new IllegalArgumentException("Received an unexpected return value to the invoke command. Key: "
						+ instanceName + ", value: " + restInvocationResult);
			} else {
				@SuppressWarnings("unchecked")
				final InvocationResult invocationResult = InvocationResult.
						createInvocationResult((Map<String, String>) restInvocationResult);
				invocationResultsMap.put(instanceName, invocationResult);
			}
		}
		
		return invocationResultsMap;
	}
	
	private InvocationResult parseInvocationResult(final Object restInvocationResult) {

		InvocationResult invocationResult = null;
		
		if (restInvocationResult == null || !(restInvocationResult instanceof Map<?, ?>)) {
			throw new IllegalArgumentException("Received an unexpected return value to the invoke command: " 
					+ restInvocationResult);
		} else {
			invocationResult = InvocationResult.createInvocationResult((Map<String, String>) restInvocationResult);
		}
		
		return invocationResult;
	}
	
	
	private String getAllSuccessMessages(final List<InvocationResult> resultsList) {
		
		final StringBuilder successMessagesText = new StringBuilder();
		for (final InvocationResult invocationResult : resultsList) {
			if (invocationResult.isSuccess()) {
				String successMessage = getSuccessMessage(invocationResult);
				successMessagesText.append(successMessage).append(System.getProperty("line.separator"));
			}
		}
		
		return successMessagesText.toString();
	}
	
	
	private String getAllFailureMessages(final List<InvocationResult> resultsList) {
		
		final StringBuilder failureMessagesText = new StringBuilder();
		for (final InvocationResult invocationResult : resultsList) {
			if (!invocationResult.isSuccess()) {
				String failureMessage = getFailureMessage(invocationResult);
				failureMessagesText.append(failureMessage).append(System.getProperty("line.separator"));
			}
		}
		
		return failureMessagesText.toString();
	}
	
	
	private String getSuccessMessage(final InvocationResult invocationResult) {
		return invocationResult.getInstanceId() + ": OK from " + invocationResult.getInstanceName() 
				+ ", Result: " + invocationResult.getResult();
	}

	
	private String getFailureMessage(final InvocationResult invocationResult) {
		return invocationResult.getInstanceId() + ": FAILED from " + invocationResult.getInstanceName()
				+ " Error: " + invocationResult.getExceptionMessage();
	}
		
	
	private String buildParsingErrorMessage(final String applicationName, final String serviceName, 
			final String instanceId, final String commandName, final List<String> parameters, final String error) {
		
		StringBuilder errorMessage = new StringBuilder("Failed to parse results of custom command \"" + commandName + "\"");
		
		if (parameters != null && parameters.size() > 0) {
			errorMessage.append(" command parameters: " + parameters);	
		}
		
		errorMessage.append(" , application: " + applicationName + ", service: " + serviceName);
		
		if (StringUtils.isNotBlank(instanceId)) {
			errorMessage.append(", instance id: " + instanceId);
		}
		
		errorMessage.append(". Reported error: " + error);
		
		return errorMessage.toString();
	}
	
	
	private String buildTestFailureErrorMessage(final String applicationName, final String serviceName, 
			final String instanceId, final String commandName, final List<String> parameters, final String error) {
		
		StringBuilder errorMessage = new StringBuilder("Invocation of custom command \"" + commandName + "\" failed.");
		
		if (parameters != null && parameters.size() > 0) {
			errorMessage.append(" Command parameters: " + parameters);	
		}
		
		errorMessage.append(" , application: " + applicationName + ", service: " + serviceName);
		
		if (StringUtils.isNotBlank(instanceId)) {
			errorMessage.append(", instance id: " + instanceId);
		}
		
		errorMessage.append(". Command output: " + error);
		
		return errorMessage.toString();
	}


}
