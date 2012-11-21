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

import java.io.File;
import java.io.IOException;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.DSLApplicationCompilatioResult;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.testng.annotations.Test;


public class CLILifecycleEventsTest extends AbstractLocalCloudTest{
	
	private static final String[] EXPECTED_STARTUP_EVENT_STRINGS = {
		"%s%s INIT invoked",
		"%s%s INIT completed, duration: ",
		"%s%s PRE_INSTALL invoked",
		"%s%s PRE_INSTALL completed, duration: ",
		"%s%s POST_INSTALL invoked",
		"%s%s POST_INSTALL completed, duration: ",
		"%s%s PRE_START invoked",
		"%s%s PRE_START completed, duration: ",
		"%s%s START invoked",
		"%s%s POST_START invoked",
		"%s%s POST_START completed, duration: "};
	
	private static final String[] EXPECTED_UNINSTALL_EVENT_STRINGS = {
		"%s%s PRE_STOP invoked",
		"%s%s PRE_STOP completed, duration: ",
		"%s%s POST_STOP invoked",
		"%s%s POST_STOP completed, duration: ",
		"%s%s SHUTDOWN invoked",
		"%s%s SHUTDOWN completed, duration: "};
	
	
	//install a service with 2 instances. 
	//test polling for lifecycle events on same service for both instances.
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testServiceInstallLifecycleLogs() 
			throws IOException, InterruptedException, PackagingException, DSLException {
		
		String usmServicePath = getUsmServicePath("groovy");
		String output = CommandTestUtils.runCommandAndWait("connect " + this.restUrl
				+ "; install-service " 
				+ usmServicePath + "; exit;");

		Service service = ServiceReader.readService(new File(usmServicePath));
		String serviceName = service.getName();
		int numInstances = service.getNumInstances();
		
		for (int i = 1; i <= numInstances; i++) {
			assertInstallationLifecycleLogs(serviceName, i, output);
		}
		
	}
	
	//install an application with 2 services
	//one has 2 instances and the other has 1.
	//tests polling for multi-service installation.
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testApplicationInstallLifecycleLogs() 
			throws IOException, DSLException, InterruptedException {
		
		String usmServicePath = getUsmApplicationPath("groovyApp/");
		File applicationFile = new File(usmServicePath, "groovyApp-application.groovy");
		DSLApplicationCompilatioResult application = ServiceReader.getApplicationFromFile(applicationFile);
		
		String applicationInstallOutput = CommandTestUtils.runCommandAndWait("connect " + this.restUrl
				+ "; install-application " 
				+ usmServicePath + "; exit;");
		
		for (Service service : application.getApplication().getServices()) {
			int numInstances = service.getNumInstances();
			String serviceName = service.getName();
			for (int i = 1; i <= numInstances; i++) {
				assertInstallationLifecycleLogs(serviceName, i, applicationInstallOutput);
			}
		}
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testApplicationUnInstallLifecycleLogs() 
			throws IOException, InterruptedException, DSLException {
		String usmServicePath = getUsmApplicationPath("groovyApp/");
		File applicationFile = new File(usmServicePath, "groovyApp-application.groovy");
		DSLApplicationCompilatioResult application = ServiceReader.getApplicationFromFile(applicationFile);
		String applicationName = application.getApplication().getName();
		
		CommandTestUtils.runCommandAndWait("connect " + this.restUrl
				+ "; install-application " 
				+ usmServicePath + "; exit;");
		
		String applicationUnInstallOutput = CommandTestUtils.runCommandAndWait("connect " + this.restUrl
				+ "; uninstall-application " 
				+ applicationName + "; exit;");
		
		for (Service service : application.getApplication().getServices()) {
			int numInstances = service.getNumInstances();
			String serviceName = service.getName();
			for (int i = 1; i <= numInstances; i++) {
				assertUnInstallLifecycleLogs(serviceName, i, applicationUnInstallOutput);
			}
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testServiceUnInstallLifecycleLogs() 
			throws IOException, InterruptedException, PackagingException, DSLException {
		
		String usmServicePath = getUsmServicePath("groovy");
		Service service = ServiceReader.readService(new File(usmServicePath));
		String serviceName = service.getName();
		int numInstances = service.getNumInstances();
		
		CommandTestUtils.runCommandAndWait("connect " + this.restUrl
				+ "; install-service " 
				+ usmServicePath + "; exit;");
		
		String uninstallOutput = CommandTestUtils.runCommandAndWait("connect " + this.restUrl
				+ "; uninstall-service " 
				+ serviceName + "; exit;");
		
		//see that the shutdown events were invoked for each service instance.
		for (int i = 1; i <= numInstances; i++) {
			assertUnInstallLifecycleLogs(serviceName, i, uninstallOutput);
		}
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testServiceSetInstanceLifecycleLogs() 
			throws IOException, InterruptedException, PackagingException, DSLException {
		
		String usmServicePath = getUsmServicePath("groovy");
		Service service = ServiceReader.readService(new File(usmServicePath));
		String serviceName = service.getName();
		int numInstances = service.getNumInstances();
		
		CommandTestUtils.runCommandAndWait("connect " + this.restUrl
				+ "; install-service " 
				+ usmServicePath + "; exit;");
		
		String incrementOutput = CommandTestUtils.runCommandAndWait("connect " + this.restUrl
				+ "; set-instances " 
				+ serviceName + " 4; exit;");
		
		//see that the shutdown events were invoked for each service instance.
		for (int i = 3; i <= numInstances; i++) {
			assertUnInstallLifecycleLogs(serviceName, i, incrementOutput);
		}
		
	}
	
	private void assertInstallationLifecycleLogs(String serviceName, int instanceNumber, String installationOutput) {
		for (String event : EXPECTED_STARTUP_EVENT_STRINGS) {
			String expectedMessage = String.format(event, serviceName, "-" + Integer.toString(instanceNumber));
			assertTrue("Missing event: " + expectedMessage, installationOutput.contains(expectedMessage));
		}
	}
	
	
	private void assertUnInstallLifecycleLogs(String serviceName, int instanceNumber, String uninstallOutput) {
		for (String event : EXPECTED_UNINSTALL_EVENT_STRINGS) {
			String expectedMessage = String.format(event, serviceName, "-" + Integer.toString(instanceNumber));
			assertTrue("Missing event: " + expectedMessage, uninstallOutput.contains(expectedMessage));
		}
	}
	
	private String getUsmServicePath(String dirOrFilename) {
		return CommandTestUtils.getPath("apps/USM/usm/" + dirOrFilename);
	}
	
	private String getUsmApplicationPath(String dirOrFilename) {
		return CommandTestUtils.getPath("apps/USM/usm/applications/" + dirOrFilename);
	}
}
