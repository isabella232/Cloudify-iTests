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

import java.util.List;

import junit.framework.Assert;

import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;

/**
 * Using the CLI for invoking templates commands.
 * @author yael
 *
 */
public final class TemplatesCommands {
	
	/**
	 * Invoking add-templates command using the CLI.
	 * @param restUrl
	 * @param templatesFolder
	 * @param expectToFail
	 * @return CLI's output
	 */
	public static String addTemplatesCLI(final String restUrl, final String templatesFolder, final boolean expectToFail) {
		final String command = "connect " + restUrl + ";add-templates " + templatesFolder;
		String output = null;
		try {
			if (expectToFail) {
				output = CommandTestUtils.runCommandExpectedFail(command);
			} else {
				output = CommandTestUtils.runCommandAndWait(command);
			}
		} catch (Exception e) {
			Assert.fail("failed to run command: " + command + ", error was: " + e.getMessage());
			e.printStackTrace();
		}
		return output;
	}

	/**
	 * Invoking remove-template command using the CLI.
	 * @param restUrl
	 * @param templateName
	 * @param expectToFail
	 * @return CLI's output
	 */
	public static String removeTemplateCLI(final String restUrl, final String templateName, final boolean expectToFail) {

		final String command = "connect " + restUrl + ";remove-template " + templateName;
		String output = null;
		try {
			if (expectToFail) {
				output = CommandTestUtils.runCommandExpectedFail(command);
			} else {
				output = CommandTestUtils.runCommandAndWait(command);
			}
		} catch (Exception e) {
			Assert.fail("failed to run command: " + command + ", error was: " + e.getMessage());
		}
		return output;
	}

	/**
	 * Invoking get-template command using the CLI.
	 * @param restUrl
	 * @param templateName
	 * @param expectToFail
	 * @return CLI's output
	 */
	public static String getTemplateCLI(final String restUrl, final String templateName, final boolean expectToFail) {
		final String command = "connect " + restUrl + ";get-template " + templateName;
		String output = null;
		try {
			if (expectToFail) {
				output = CommandTestUtils.runCommandExpectedFail(command);
			} else {
				output = CommandTestUtils.runCommandAndWait(command);
			}
		} catch (Exception e) {
			Assert.fail("failed to run command: " + command + ", error was: " + e.getMessage());
		}
		return output;
	}
	
	/**
	 * Invoking list-templates command using the CLI.
	 * @param restUrl
	 * @return CLI's output
	 */
	public static String listTemplatesCLI(final String restUrl, final boolean expectToFail) {
		final String command = "connect " + restUrl + ";list-templates";
		String output = null;
		try {
			if (expectToFail) {
				output = CommandTestUtils.runCommandExpectedFail(command);
			} else {
				output = CommandTestUtils.runCommandAndWait(command);
			}
		} catch (Exception e) {
			Assert.fail("failed to run command: " + command + ", error was: " + e.getMessage());
		}
		return output;
	}

	/**
	 * Invoking remove-template command for each template, using the CLI.
	 * @param restUrl
	 * @param listTemplates
	 */
	public static void removeTemplates(String restUrl, List<String> listTemplates) {
		for (String templateName : listTemplates) {
			removeTemplateCLI(restUrl, templateName, false);
		}
	}
	
}
