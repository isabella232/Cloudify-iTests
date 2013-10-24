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
package org.cloudifysource.quality.iTests.test.cli.cloudify.events;

import com.google.common.io.Resources;
import iTests.framework.utils.IOUtils;
import iTests.framework.utils.ScriptUtils;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.DSLApplicationCompilatioResult;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractLocalCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * Test events are displayed correctly during install and uninstall operations.
 * see https://cloudifysource.atlassian.net/browse/CLOUDIFY-2152
 */
public class CLILifecycleEventsTest extends AbstractLocalCloudTest {
	
	private static final String[] EXPECTED_INSTALL_EVENT_STRINGS = {
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


    /**
     *
     * Case 1
     * =======
     *
     * 1. Install a service with 2 instances.
     * 2. Assert there are events for each instance.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws PackagingException
     * @throws DSLException
     */
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testServiceInstallLifecycleLogs() 
			throws IOException, InterruptedException, PackagingException, DSLException {

        String usmServicePath = getUsmServicePath("groovy");

        String output = installServiceAndWait(usmServicePath, "groovy", false);

        Service service = ServiceReader.readService(new File(usmServicePath));
		String serviceName = service.getName();
		int numInstances = service.getNumInstances();
		
		for (int i = 1; i <= numInstances; i++) {
			assertInstallationLifecycleLogs(serviceName, i, output);
		}
		
	}

    /**
     *
     * Case 2
     * ======
     *
     * 1. Install an application with multiple services.
     * 2. Assert events are present for each instance and for each service.
     *
     * @throws IOException
     * @throws DSLException
     * @throws InterruptedException
     */
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testApplicationInstallLifecycleLogs() 
			throws IOException, DSLException, InterruptedException {
		
		String usmApplicationPath = getUsmApplicationPath("groovyApp");
		File applicationFile = new File(usmApplicationPath, "groovyApp-application.groovy");

		DSLApplicationCompilatioResult application = ServiceReader.getApplicationFromFile(applicationFile);

        String output = installApplicationAndWait(usmApplicationPath, "groovyApp",
                AbstractTestSupport.OPERATION_TIMEOUT);

        for (Service service : application.getApplication().getServices()) {
			int numInstances = service.getNumInstances();
			String serviceName = service.getName();
			for (int i = 1; i <= numInstances; i++) {
				assertInstallationLifecycleLogs(serviceName, i, output);
			}
		}
	}

    /**
     *
     * Case 3
     * ======
     *
     * 1. Install application.
     * 2. Uninstall application.
     * 3. Assert all uninstall events are present.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws DSLException
     */
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testApplicationUnInstallLifecycleLogs() 
			throws IOException, InterruptedException, DSLException {

		String usmApplicationPath = getUsmApplicationPath("groovyApp");
		File applicationFile = new File(usmApplicationPath, "groovyApp-application.groovy");

        DSLApplicationCompilatioResult application = ServiceReader.getApplicationFromFile(applicationFile);

        String applicationName = application.getApplication().getName();

        installApplicationAndWait(usmApplicationPath, applicationName, AbstractTestSupport.OPERATION_TIMEOUT);

        String output = uninstallApplication(applicationName);
        for (Service service : application.getApplication().getServices()) {
			int numInstances = service.getNumInstances();
			String serviceName = service.getName();
			for (int i = 1; i <= numInstances; i++) {
				assertUnInstallLifecycleLogs(serviceName, i, output);
			}
		}
	}

    /**
     *
     * Case 4
     * ======
     *
     * 1. Install service.
     * 2. Uninstall service.
     * 3. Assert all uninstall events are present.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws DSLException
     */
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testServiceUnInstallLifecycleLogs() 
			throws IOException, InterruptedException, PackagingException, DSLException {
		
		String usmServicePath = getUsmServicePath("groovy");
		Service service = ServiceReader.readService(new File(usmServicePath));

		String serviceName = service.getName();
		int numInstances = service.getNumInstances();

        installServiceAndWait(usmServicePath, "groovy", false);

        String output = uninstallService("groovy");

		for (int i = 1; i <= numInstances; i++) {
			assertUnInstallLifecycleLogs(serviceName, i, output);
		}
		
	}

    /**
     *
     * Case 5
     * ======
     *
     * 1. Install service.
     * 2. set instances on that service.
     * 3. Assert all uninstall events are present.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws DSLException
     */
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testServiceSetInstanceLifecycleLogs() 
			throws IOException, InterruptedException, PackagingException, DSLException {
		
		String usmServicePath = getUsmServicePath("groovy");
		Service service = ServiceReader.readService(new File(usmServicePath));
		String serviceName = service.getName();
		int numInstances = service.getNumInstances();

        installServiceAndWait(usmServicePath, "groovy", false);

        ServiceInstaller installer = new ServiceInstaller(restUrl, "groovy");
        String output = installer.setInstances(4);

		for (int i = 3; i <= numInstances; i++) {
			assertUnInstallLifecycleLogs(serviceName, i, output);
		}
		
	}

    /**
     * Case 6
     * ======
     *
     * 1. Install an application with 2 services.
     * 2. One service will be installed much faster than the other one.
     * 3. Assert that events from the slow service are displayed as well.
     */
    @Test
    public void testDelayedServiceLifeCycleEvents()
            throws IOException, DSLException, InterruptedException, PackagingException {

        String slowgsc;
        if (ScriptUtils.isWindows()) {
           slowgsc = Resources.getResource("slowgsc/slowgsc.bat").getPath();
        } else {
            slowgsc = Resources.getResource("slowgsc/slowgsc.sh").getPath();
        }
        FileUtils.copyFileToDirectory(new File(slowgsc), new File(ScriptUtils.getBuildBinPath()));

        // before we install the service manipulate gsc.xml
        File gscXml = new File(ScriptUtils.getBuildPath() + "/config/gsa/gsc.xml");

        try {
            IOUtils.replaceTextInFile(gscXml.getAbsolutePath(), "/bin/gsc.", "/bin/slowgsc.");
            testApplicationInstallLifecycleLogs();
        } finally {
            IOUtils.replaceTextInFile(gscXml.getAbsolutePath(), "/bin/slowgsc.", "/bin/gsc.");
        }

    }
	
	private void assertInstallationLifecycleLogs(String serviceName, int instanceNumber, String installationOutput) {
		for (String event : EXPECTED_INSTALL_EVENT_STRINGS) {
			String expectedMessage = String.format(event, serviceName, "-" + Integer.toString(instanceNumber));
			AbstractTestSupport.assertTrue("Missing event: " + expectedMessage, installationOutput.contains(expectedMessage));
		}
	}
	
	
	private void assertUnInstallLifecycleLogs(String serviceName, int instanceNumber, String uninstallOutput) {
		for (String event : EXPECTED_UNINSTALL_EVENT_STRINGS) {
			String expectedMessage = String.format(event, serviceName, "-" + Integer.toString(instanceNumber));
			AbstractTestSupport.assertTrue("Missing event: " + expectedMessage, uninstallOutput.contains(expectedMessage));
		}
	}
	
	private String getUsmServicePath(String dirOrFilename) {
		return CommandTestUtils.getPath("src/main/resources/apps/USM/usm/" + dirOrFilename);
	}
	
	private String getUsmApplicationPath(String dirOrFilename) {
		return CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/" + dirOrFilename);
	}
}
