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
package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.collections.ListUtils;
import org.cloudifysource.dsl.rest.AddTemplatesException;
import org.cloudifysource.dsl.rest.request.AddTemplatesRequest;
import org.cloudifysource.dsl.rest.response.AddTemplatesResponse;
import org.cloudifysource.dsl.rest.response.ListTemplatesResponse;
import org.cloudifysource.dsl.rest.response.UploadResponse;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;

import com.j_spaces.kernel.PlatformVersion;

/**
 * Using the new REST API for invoking templates commands.
 * 
 * @author yael
 * 
 */
public class TemplatesCommandsRestAPI {

	/**
	 * Invoking add-templates command using the REST API.
	 * 
	 * @param restUrl
	 * @param templatesPackedFile
	 * @return {@link org.cloudifysource.dsl.rest.response.AddTemplatesResponse}
	 * @throws RestClientException 
	 * @throws AddTemplatesException 
	 */
	public static AddTemplatesResponse addTemplates(final String restUrl, final File templatesPackedFile) 
			throws RestClientException, AddTemplatesException {
		return addTemplates(restUrl, templatesPackedFile, false);
	}

	/**
	 * Invoking add-templates command using the REST API.
	 * 
	 * @param restUrl
	 * @param templatesPackedFile
	 * @param expectToFail true if an AddTemplatesException expected.
	 * @return {@link org.cloudifysource.dsl.rest.response.AddTemplatesResponse}
	 * @throws RestClientException 
	 */
	public static AddTemplatesResponse addTemplates(final String restUrl, final File templatesPackedFile, final boolean expectToFail) 
			throws RestClientException {
		RestClient restClient = null;
		try {
			restClient = createRestClient(restUrl, "", "");
		} catch (final Exception e) {
			Assert.fail("failed to create rest client: " + e.getMessage());
		}
		try {
			restClient.connect();
		} catch (final RestClientException e) {
			Assert.fail("failed to connect: " + e.getMessageFormattedText());
		}
		UploadResponse uploadResponse = null;
		try {
			uploadResponse = restClient.upload(null, templatesPackedFile);
		} catch (final RestClientException e) {
			Assert.fail("failed to upload templates folder [" + templatesPackedFile.getAbsolutePath()
					+ "] error message: " + e.getMessageFormattedText());
		}
		final AddTemplatesRequest request = new AddTemplatesRequest();
		request.setUploadKey(uploadResponse.getUploadKey());
		AddTemplatesResponse addTemplatesResponse = null;
		try {
			addTemplatesResponse = restClient.addTemplates(request);
			if (expectToFail) {
				Assert.fail("addTemplates expected to failed but seccedded.");
			}
		} catch (final AddTemplatesException e) {
			if (!expectToFail) {
				Assert.fail("failed to add templates from folder [" + templatesPackedFile.getAbsolutePath()
						+ "] error message: " + e.getMessage());
			} else {
				return e.getAddTemplatesResponse();
			}
		}
		return addTemplatesResponse;
	}

	/**
	 * Invoking remove-template command using the REST API.
	 * 
	 * @param restUrl
	 * @param templateName
	 * @param expectToFail
	 * @param failureMsgContains
	 */
	public static void removeTemplate(final String restUrl, final String templateName, final boolean expectToFail,
			final String failureMsgContains) {
		RestClient restClient = null;
		try {
			restClient = createRestClient(restUrl, "", "");
		} catch (final Exception e) {
			Assert.fail("failed to create rest client: " + e.getMessage());
		}
		try {
			restClient.connect();
		} catch (final RestClientException e) {
			Assert.fail("failed to connect: " + e.getMessageFormattedText());
		}
		try {
			restClient.removeTemplate(templateName);
			if (expectToFail) {
				Assert.fail("remove template expected to fail "
						+ (failureMsgContains != null ? "[" + failureMsgContains + "]" : "") + " but succeeded.");
			}
		} catch (final RestClientException e) {
			if (!expectToFail) {
				Assert.fail("failed to remove template: " + e.getMessageFormattedText());
			} else {
				if (failureMsgContains != null) {
					Assert.assertTrue(e.getMessageFormattedText().contains(failureMsgContains));
				}
			}
		}
	}

	public static List<String> listTemplates(final String restUrl) {
		return listTemplates(restUrl, "", "");
	}

	/**
	 * Invoking list-templates command using the REST API.
	 * 
	 * @param restUrl
	 * @return list of templates as returned from the rest client.
	 */
	public static List<String> listTemplates(final String restUrl, final String username, final String password) {

		RestClient restClient = null;
		try {
			restClient = createRestClient(restUrl, username, password);
		} catch (final Exception e) {
			Assert.fail("failed to create rest client with rest url: " + restUrl + ", error was: " + e.getMessage());
		}
		try {
			restClient.connect();
		} catch (final RestClientException e) {
			Assert.fail("failed to connect: " + e.getMessageFormattedText());
		}
		ListTemplatesResponse listTemplatesResponse = null;
		try {
			listTemplatesResponse = restClient.listTemplates();
		} catch (final RestClientException e) {
			Assert.fail("failed to get list templates from rest client, error was: " + e.getMessageFormattedText());
		}
		return new LinkedList<String>(listTemplatesResponse.getTemplates().keySet());
	}

	/**
	 * Checks if the expected list equals (have the same templates names) to the actual templates list (using the CLI to
	 * invoke list-templates).
	 * 
	 * @param restUrl
	 * @param expectedListTempaltes
	 */
	public static void assertExpectedList(final String restUrl, final List<String> expectedListTempaltes,
			final String username, final String password) {
		final List<String> listTemplates = TemplatesCommandsRestAPI.listTemplates(restUrl, username, password);
		Assert.assertEquals(expectedListTempaltes.size(), listTemplates.size());
		Assert.assertTrue(ListUtils.subtract(expectedListTempaltes, listTemplates).isEmpty());
	}

	public static void assertExpectedList(final String restUrl, final List<String> expectedListTempaltes) {
		assertExpectedList(restUrl, expectedListTempaltes, "", "");
	}

	private static RestClient createRestClient(final String url, final String username, final String password)
			throws RestClientException, MalformedURLException {
		final String apiVersion = PlatformVersion.getVersion();
		return new RestClient(new URL(url), username, password, apiVersion);
	}
}
