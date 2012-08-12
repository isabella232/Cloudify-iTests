/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package test.cli.cloudify;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.restclient.ErrorStatusException;
import org.cloudifysource.shell.commands.CLIException;
import org.testng.annotations.Test;

/***************************
 * Checks that the USM correctly handles a service with a closure as a start element.
 * 
 * @author barakme
 * 
 */
public class StartClosureTest extends AbstractLocalCloudTest {

	// install a service with 2 instances.
	// test polling for lifecycle events on same service for both instances.
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testStartClosure()
			throws IOException, InterruptedException, PackagingException, DSLException, ErrorStatusException, CLIException {

		// this.isDevEnv = true;
		final String usmServicePath = getUsmServicePath("startClosure");
		CommandTestUtils.runCommandAndWait("connect " + restUrl
				+ "; install-service "
				+ usmServicePath + "; exit;");

		checkMonitors();
		
		CommandTestUtils.runCommandAndWait("connect " + restUrl
				+ "; uninstall-service groovy; exit;");

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testStartEmpty()
			throws IOException, InterruptedException, PackagingException, DSLException, ErrorStatusException, CLIException {

		// this.isDevEnv = true;
		final String usmServicePath = getUsmServicePath("startEmpty");
		CommandTestUtils.runCommandAndWait("connect " + restUrl
				+ "; install-service "
				+ usmServicePath + "; exit;");

		checkMonitors();
		
		CommandTestUtils.runCommandAndWait("connect " + restUrl
				+ "; uninstall-service groovy; exit;");

	}

	private void checkMonitors() throws ErrorStatusException, CLIException {
		Map<String, Object> data = this.getAdminData("ProcessingUnits/Names/default.groovy/Instances/0/Statistics/Monitors/USM/Monitors");		
		Assert.assertEquals("Expected one element in monitors response", 1, data.size());
		@SuppressWarnings("unchecked")
		List<Object> items = (List<Object>) data.values().iterator().next();
		Assert.assertTrue("Expecting process metrics!", items.size() > 5);
		Map<String, Object> pidMap = this.getAdminData("ProcessingUnits/Names/default.groovy/Instances/0/Statistics/Monitors/USM/Monitors/USM_Actual%20Process%20ID");
		
		Assert.assertEquals(1, pidMap.size());
		String pidString = (String) pidMap.values().iterator().next();
		final long pid = Long.parseLong(pidString);
		Assert.assertNotSame(0, pid);
		
	}

	private String getUsmServicePath(final String dirOrFilename) {
		return CommandTestUtils.getPath("apps/USM/usm/" + dirOrFilename);
	}

}
