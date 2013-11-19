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
import org.cloudifysource.dsl.internal.CloudifyConstants.InvocationStatus;
import org.cloudifysource.dsl.rest.response.InvokeInstanceCommandResponse;
import org.cloudifysource.dsl.rest.response.InvokeServiceCommandResponse;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.quality.iTests.framework.utils.CommandInvoker;
import org.cloudifysource.quality.iTests.framework.utils.usm.USMTestUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.restclient.InvocationResult;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.exceptions.RestClientResponseException;
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
	
	private static final String INVOCATION_SUCCESS_OUTPUT = "OK from instance #";
	private static final String INVOCATION_FAILURE_OUTPUT = "FAILED from instance #";
	
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
		testCommandInvocationOnServiceExpectSuccess(CUSTOM_COMMAND_PRINT, params, CUSTOM_COMMAND_PRINT_EXPECTED_OUTPUT);
		
		LogUtils.log("Starting to check print command by instance id");
		for(int i=1 ; i<= totalInstances ; i++) {
			testCommandInvocationOnInstanceExpectSuccess(i, CUSTOM_COMMAND_PRINT, params, 
					CUSTOM_COMMAND_PRINT_EXPECTED_OUTPUT);
		}
	}
	
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testParamsCommand() throws Exception {
		final List<String> params = new ArrayList<String>();
		params.add("2");
		params.add("3");
		
		LogUtils.log("Checking params command on all instances");
		testCommandInvocationOnServiceExpectSuccess(CUSTOM_COMMAND_PARAMS, params, 
				CUSTOM_COMMAND_PARAMS_EXPECTED_OUTPUT);
		
		LogUtils.log("Starting to check params command by instance id");
		for(int i=1 ; i<= totalInstances ; i++) {
			testCommandInvocationOnInstanceExpectSuccess(i, CUSTOM_COMMAND_PARAMS, params, 
					CUSTOM_COMMAND_PARAMS_EXPECTED_OUTPUT);
		}
	}
	
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testContextCommand() throws Exception {
		final List<String> params = new ArrayList<String>();
		
		LogUtils.log("Checking context command on all instances");
		testCommandInvocationOnServiceExpectSuccess(CUSTOM_COMMAND_CONTEXT, params, 
				CUSTOM_COMMAND_CONTEXT_EXPECTED_OUTPUT);
		
		LogUtils.log("Starting to check context command by instance id");
		for(int i=1 ; i<= totalInstances ; i++) {
			testCommandInvocationOnInstanceExpectSuccess(i, CUSTOM_COMMAND_CONTEXT, params, 
					CUSTOM_COMMAND_CONTEXT_EXPECTED_OUTPUT);
		}
	}
	
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testRunScriptCommand() throws Exception {
		final List<String> params = new ArrayList<String>();
		
		LogUtils.log("Checking runScript command on all instances");
		testCommandInvocationOnServiceExpectSuccess(CUSTOM_COMMAND_RUN_SCRIPT, params, 
				CUSTOM_COMMAND_RUN_SCRIPT_EXPECTED_OUTPUT);
		
		LogUtils.log("Starting to check runScript command by instance id");
		for(int i=1 ; i<= totalInstances ; i++) {
			testCommandInvocationOnInstanceExpectSuccess(i, CUSTOM_COMMAND_RUN_SCRIPT, params, 
					CUSTOM_COMMAND_RUN_SCRIPT_EXPECTED_OUTPUT);
		}
	}
	
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testMissingParams() throws Exception {
		final List<String> params = new ArrayList<String>();
		
		LogUtils.log("Checking params command with missing params on all instances");
		testCommandInvocationOnServiceExpectTheUnexpected(CUSTOM_COMMAND_PARAMS, params, 
				"Invoke command on instance #1@127.0.0.1 returned an unexpected value");
		
		LogUtils.log("Checking params command with missing params on instance id 1");
		try {
			commandInvoker.restInvokeInstanceCommand(DEFAULT_APPLICATION_NAME, 
					SERVICE_NAME, 1, CUSTOM_COMMAND_PARAMS, params);
			// an exception was not thrown - something is wrong
			Assert.fail("Custom command \"" + CUSTOM_COMMAND_PRINT + "\" was invoked on an invalid instance number but"
					+ " an exception wasn't thrown");
		} catch (RestClientResponseException e) {
			// if we're here - good! let's verify this is the correct exception
			assertTrue("wrong status code, expected 400, found: " + e.getStatusCode(), e.getStatusCode() == 400);
			assertTrue("wrong error message: " + e.getMessageFormattedText(), e.getMessageFormattedText().contains(
					"Error invoking pu instance default.simpleCustomCommandsMultipleInstances:1. Cause: java.lang.ClassNotFoundException: dslEntity$_run_closure3_closure6"));
		} catch (Exception e) {
			AssertFail("Invalid exception caught, expected to catch RestClientResponseException", e);
		}

	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testWrongInstanceNumber() throws Exception {
		final List<String> params = new ArrayList<String>();
		
		LogUtils.log("Invoking print command on wrong instance number (2)");
		try {
			commandInvoker.restInvokeInstanceCommand(DEFAULT_APPLICATION_NAME, 
					SERVICE_NAME, 2, CUSTOM_COMMAND_PRINT, params);
			// an exception was not thrown - something is wrong
			Assert.fail("Custom command \"" + CUSTOM_COMMAND_PRINT + "\" was invoked on an invalid instance number but"
					+ " an exception wasn't thrown");
		} catch (RestClientResponseException e) {
			// if we're here - good! let's verify this is the correct exception
			assertTrue("wrong status code, expected 400, found: " + e.getStatusCode(), e.getStatusCode() == 400);
			assertTrue("wrong error message: " + e.getMessageFormattedText(), e.getMessageFormattedText().contains(
					"Instance 2 of service default.simpleCustomCommandsMultipleInstances of application default "
					+ "could not be reached"));
		} catch (Exception e) {
			AssertFail("Invalid exception caught, expected to catch RestClientResponseException", e);
		}
	}
	
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testExceptionCommand() throws Exception {
		final List<String> params = new ArrayList<String>();
		
		LogUtils.log("Checking exception command on all instances");
		testCommandInvocationOnServiceExpectFailure(CUSTOM_COMMAND_EXCEPTION, params, 
				CUSTOM_COMMAND_EXCEPTION_EXPECTED_OUTPUT);
		
		LogUtils.log("Starting to check exception command by instance id");
		for(int i=1 ; i<= totalInstances ; i++) {
			testCommandInvocationOnInstanceExpectFailure(i, CUSTOM_COMMAND_EXCEPTION, params, 
					CUSTOM_COMMAND_EXCEPTION_EXPECTED_OUTPUT);
		}
	}
	
	
	private void testCommandInvocationOnServiceExpectSuccess(final String commandName, final List<String> params,
			final String expectedOutput) throws RestClientException {
		
		final List<InvocationResult> totalResultsList;
		final List<InvocationResult> unexpectedResultsList;
		final List<InvocationResult> failureResultsList;
		
		InvokeServiceCommandResponse invokeResponse = commandInvoker.restInvokeServiceCommand(DEFAULT_APPLICATION_NAME, 
				SERVICE_NAME, commandName, params);
		
		Map<String, InvocationResult> invocationResults = 
				parseInvocationResults(invokeResponse.getInvocationResultPerInstance());
		totalResultsList = new ArrayList<InvocationResult>(invocationResults.values());
		Collections.sort(totalResultsList);
		
		unexpectedResultsList = getAllUnexpectedResults(totalResultsList);
		failureResultsList = getAllFailureResults(totalResultsList);
		String commandOutput = getCommandOutputSummary(totalResultsList);
		LogUtils.log("Command results: " + commandOutput);
		
		AbstractTestSupport.assertTrue(buildErrorMessage("Command invocation failed.",
				DEFAULT_APPLICATION_NAME, SERVICE_NAME, ""/*instanceId*/, commandName, params, commandOutput),
				(unexpectedResultsList.size() == 0) && (failureResultsList.size() == 0));
		
		for(int i=1 ; i <= totalInstances ; i++) {
			AbstractTestSupport.assertTrue("Custom command \"" + commandName + "\" returned a wrong result from "
					+ "instance #" + i + ": " + commandOutput,
					commandOutput.contains(INVOCATION_SUCCESS_OUTPUT + i) && commandOutput.contains(expectedOutput));
		}
	}
	
	
	private void testCommandInvocationOnServiceExpectFailure(final String commandName, final List<String> params,
			final String expectedOutput) throws RestClientException {
		
		final List<InvocationResult> totalResultsList;
		final List<InvocationResult> successResultsList;
		final List<InvocationResult> unexpectedResultsList;
		
		InvokeServiceCommandResponse invokeResponse = commandInvoker.restInvokeServiceCommand(DEFAULT_APPLICATION_NAME, 
				SERVICE_NAME, commandName, params);
		
		Map<String, InvocationResult> invocationResults = 
				parseInvocationResults(invokeResponse.getInvocationResultPerInstance());
		totalResultsList = new ArrayList<InvocationResult>(invocationResults.values());
		Collections.sort(totalResultsList);
		
		successResultsList = getAllSuccessResults(totalResultsList);
		unexpectedResultsList = getAllUnexpectedResults(totalResultsList);
		String commandOutput = getCommandOutputSummary(totalResultsList);
		LogUtils.log("Command results: " + commandOutput);
		
		AbstractTestSupport.assertTrue(buildErrorMessage("Invocation was supposed to fail but succeeded.",
				DEFAULT_APPLICATION_NAME, SERVICE_NAME, ""/*instanceId*/, commandName, params, commandOutput),
				(unexpectedResultsList.size() == 0) && (successResultsList.size() == 0));
		
		for(int i=1 ; i <= totalInstances ; i++) {
			AbstractTestSupport.assertTrue("Custom command \"" + commandName + "\" returned a wrong result from "
					+ "instance #" + i + ": " + commandOutput,
					commandOutput.contains(INVOCATION_FAILURE_OUTPUT + i) && commandOutput.contains(expectedOutput));
		}
	}
	
	
	private void testCommandInvocationOnServiceExpectTheUnexpected(final String commandName, final List<String> params,
			final String expectedOutput) throws RestClientException {
		
		final List<InvocationResult> totalResultsList;
		final List<InvocationResult> successResultsList;
		final List<InvocationResult> failureResultsList;
		
		InvokeServiceCommandResponse invokeResponse = commandInvoker.restInvokeServiceCommand(DEFAULT_APPLICATION_NAME, 
				SERVICE_NAME, commandName, params);
		
		Map<String, InvocationResult> invocationResults = 
				parseInvocationResults(invokeResponse.getInvocationResultPerInstance());
		totalResultsList = new ArrayList<InvocationResult>(invocationResults.values());
		Collections.sort(totalResultsList);
		
		successResultsList = getAllSuccessResults(totalResultsList);
		failureResultsList = getAllFailureResults(totalResultsList);
		String commandOutput = getCommandOutputSummary(totalResultsList);
		LogUtils.log("Command results: " + commandOutput);
		
		AbstractTestSupport.assertTrue(buildErrorMessage("Invocation of was supposed to return an \"unexpected\""
				+ " value but didn't", DEFAULT_APPLICATION_NAME, SERVICE_NAME, ""/*instanceId*/, commandName,
				params, commandOutput), (successResultsList.size() == 0) && (failureResultsList.size() == 0));
		
		for(int i=1 ; i <= totalInstances ; i++) {
			AbstractTestSupport.assertTrue("Custom command \"" + commandName + "\" returned a wrong result from "
					+ "instance #" + i + ": " + commandOutput, commandOutput.contains("Invoke command on instance #" 
			+ i + "@127.0.0.1 returned an unexpected value"));
		}
	}
	
	
	private void testCommandInvocationOnInstanceExpectSuccess(final int instanceId, final String commandName,
			final List<String> params, final String expectedOutput) 
					throws RestClientException {
				
		InvokeInstanceCommandResponse invokeResponse = commandInvoker.restInvokeInstanceCommand(DEFAULT_APPLICATION_NAME, 
				SERVICE_NAME, instanceId, commandName, params);
		
		InvocationResult result = parseInvocationResult(invokeResponse.getInvocationResult());
		InvocationStatus status = result.getInvocationStatus();
		String commandOutput;
		
		if (status == InvocationStatus.SUCCESS) {
			commandOutput = getSuccessMessage(result);
		} else {
			commandOutput = getFailureMessage(result);
		}
		LogUtils.log("Command result: " + commandOutput);
		
		AbstractTestSupport.assertTrue(buildErrorMessage("Command invocation failed", DEFAULT_APPLICATION_NAME, 
				SERVICE_NAME, Integer.toString(instanceId), commandName, params, commandOutput), 
				status == InvocationStatus.SUCCESS);
		
		AbstractTestSupport.assertTrue("Custom command \"" + commandName + "\" returned unexpected result from "
				+ "instance #" + instanceId  + ": " + commandOutput,
				commandOutput.contains(INVOCATION_SUCCESS_OUTPUT + instanceId) && commandOutput.contains(expectedOutput));
		
		// verify there are not printouts of other instances
		for(int i = 1 ; i <= totalInstances ; i++){
			if(i != instanceId) {
				// this means a the command was executed on the wrong instance
				Assert.assertFalse("Custom command \"" + commandName + "\" should not receive any output from instance #"
						+ i ,commandOutput.contains("instance #" + i));
			}
		}
	}
	
	
	private void testCommandInvocationOnInstanceExpectFailure(final int instanceId, final String commandName,
			final List<String> params, final String expectedOutput) 
					throws RestClientException {
				
		InvokeInstanceCommandResponse invokeResponse = commandInvoker.restInvokeInstanceCommand(DEFAULT_APPLICATION_NAME, 
				SERVICE_NAME, instanceId, commandName, params);
		
		InvocationResult result = parseInvocationResult(invokeResponse.getInvocationResult());
		InvocationStatus status = result.getInvocationStatus();
		String commandOutput;
		
		if (status == InvocationStatus.SUCCESS) {
			commandOutput = getSuccessMessage(result);
		} else {
			commandOutput = getFailureMessage(result);
		}
		LogUtils.log("Command result: " + commandOutput);
		
		AbstractTestSupport.assertTrue(buildErrorMessage("Invocation was supposed to fail but succeeded",
				DEFAULT_APPLICATION_NAME, SERVICE_NAME, Integer.toString(instanceId), commandName, params, commandOutput), 
				status == InvocationStatus.FAILURE);
		
		AbstractTestSupport.assertTrue("Custom command \"" + commandName + "\" returned unexpected result from "
				+ "instance #" + instanceId  + ": " + commandOutput,
				commandOutput.contains(INVOCATION_FAILURE_OUTPUT + instanceId) && commandOutput.contains(expectedOutput));
		
		// verify there are not printouts of other instances
		for(int i = 1 ; i <= totalInstances ; i++){
			if(i != instanceId) {
				// this means a the command was executed on the wrong instance
				Assert.assertFalse("Custom command \"" + commandName + "\" should not receive any output from instance #"
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
	
	
	private String getCommandOutputSummary(List<InvocationResult> resultsList) {
		
		String successMessages = getAllSuccessMessages(resultsList);
		String unexpectedMessages = getAllUnexpectedMessages(resultsList);
		String failureMessages = getAllFailureMessages(resultsList);
		String commandOutput = successMessages + System.getProperty("line.separator") + unexpectedMessages + 
				System.getProperty("line.separator") + failureMessages;
		
		return commandOutput;
	}
	
	private Map<String, InvocationResult> parseInvocationResults(
			final Map<String, Map<String, String>> restInvocationResultsPerInstance) {

		final Map<String, InvocationResult> invocationResultsMap = new LinkedHashMap<String, InvocationResult>();
		
		for (final Map.Entry<String, Map<String, String>> entry : restInvocationResultsPerInstance.entrySet()) {
			final String instanceName = entry.getKey();
			final Map<String, String> restInvocationResult = entry.getValue();
			invocationResultsMap.put(instanceName, parseInvocationResult(restInvocationResult));
		}
		
		return invocationResultsMap;
	}
	
	
	private InvocationResult parseInvocationResult(final Map<String, String> restInvocationResult) {

		InvocationResult invocationResult = null;		
		if (restInvocationResult != null) {
			invocationResult = InvocationResult.createInvocationResult(restInvocationResult);
		}
		
		return invocationResult;
	}
	
	
	private List<InvocationResult> getAllSuccessResults(final List<InvocationResult> resultsList) {
		
		final List<InvocationResult> successResultsList = new ArrayList<InvocationResult>();
		for (final InvocationResult invocationResult : resultsList) {
			if (invocationResult.getInvocationStatus() == InvocationStatus.SUCCESS) {
				successResultsList.add(invocationResult);
			}
		}
		
		return successResultsList;
	}
	
	
	private String getAllSuccessMessages(final List<InvocationResult> resultsList) {
		
		final StringBuilder successMessagesText = new StringBuilder();
		for (final InvocationResult invocationResult : resultsList) {
			if (invocationResult.getInvocationStatus() == InvocationStatus.SUCCESS) {
				String successMessage = getSuccessMessage(invocationResult);
				successMessagesText.append(successMessage).append(System.getProperty("line.separator"));
			}
		}
		
		return successMessagesText.toString();
	}
	
	
	private List<InvocationResult> getAllUnexpectedResults(final List<InvocationResult> resultsList) {
		
		final List<InvocationResult> unexpectedResultsList = new ArrayList<InvocationResult>();
		for (final InvocationResult invocationResult : resultsList) {
			if (invocationResult.getInvocationStatus() == InvocationStatus.UNEXPECTED) {
				unexpectedResultsList.add(invocationResult);
			}
		}
		
		return unexpectedResultsList;
	}
	
	
	private String getAllUnexpectedMessages(final List<InvocationResult> resultsList) {
		
		final StringBuilder unexpectedMessagesText = new StringBuilder();
		for (final InvocationResult invocationResult : resultsList) {
			if (invocationResult.getInvocationStatus() == InvocationStatus.UNEXPECTED) {
				String unexpectedMessage = getUnexpectedMessage(invocationResult);
				unexpectedMessagesText.append(unexpectedMessage).append(System.getProperty("line.separator"));
			}
		}
		
		return unexpectedMessagesText.toString();
	}
	
	
	private List<InvocationResult> getAllFailureResults(final List<InvocationResult> resultsList) {
		
		final List<InvocationResult> failureResultsList = new ArrayList<InvocationResult>();
		for (final InvocationResult invocationResult : resultsList) {
			if (invocationResult.getInvocationStatus() == InvocationStatus.FAILURE) {
				failureResultsList.add(invocationResult);
			}
		}
		
		return failureResultsList;
	}
	
	
	private String getAllFailureMessages(final List<InvocationResult> resultsList) {
		
		final StringBuilder failureMessagesText = new StringBuilder();
		for (final InvocationResult invocationResult : resultsList) {
			if (invocationResult.getInvocationStatus() == InvocationStatus.FAILURE) {
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
	
	
	private String getUnexpectedMessage(final InvocationResult invocationResult) {
		return "Invoke command on " + invocationResult.getInstanceName() + " returned an unexpected value: " 
				+ invocationResult.getResult();
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
	
	
	/*private String buildTestFailureErrorMessage(final String applicationName, final String serviceName, 
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
	}*/
	
	private String buildErrorMessage(final String basicError, final String applicationName, final String serviceName, 
			final String instanceId, final String commandName, final List<String> parameters, final String commandOutput) {
		
		StringBuilder errorMessage = new StringBuilder(basicError);
		
		errorMessage.append("Command name: " + commandName);		
		if (parameters != null && parameters.size() > 0) {
			errorMessage.append(", parameters: " + parameters);	
		}
		
		errorMessage.append(", application: " + applicationName + ", service: " + serviceName);		
		if (StringUtils.isNotBlank(instanceId)) {
			errorMessage.append(", instance id: " + instanceId);
		}
		
		errorMessage.append(". Command output: " + commandOutput);
		
		return errorMessage.toString();
	}


}
