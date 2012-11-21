/*******************************************************************************
* Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
******************************************************************************/
package test.cli.cloudify;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

import test.usm.USMTestUtils;

import framework.utils.IOUtils;
import framework.utils.LogUtils;

public class StartDetectionTest extends AbstractLocalCloudTest {
	
	private final String serviceName = "simple";
	private final String applicationName = "default";
	private final String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
	private final String serviceDir = CommandTestUtils.getPath("/apps/USM/usm/SimpleDetection");
	private final String serviceFailDir = CommandTestUtils.getPath("/apps/USM/usm/SimpleDetectionFail");
	private File startDetectionFile = new File(serviceDir, "startDetection.groovy");
	private String installCommand;
	private String unInstallCommand;

	/**
	 * The proper behavior of the start detection should be:
	 * if the start detection is a java groovy file, exiting with exit code 0
	 * should be considered as true and any other exit code should be considered as false.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
	public void startDetectionUsingExitCodeTest() throws IOException, InterruptedException {
		
		initFields();
		
		LogUtils.log("Installing simpleDetection with detection exit Code 0");
		CommandTestUtils.runCommandAndWait(installCommand);

		LogUtils.log("Asserting simple service started successfully");
		ProcessingUnit simplePu = admin.getProcessingUnits().waitFor(absolutePUName, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS);
		assertTrue("Expected processing unit instance number should be 1", simplePu.waitFor(1, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS));
		CommandTestUtils.runCommandAndWait(unInstallCommand);
		
		LogUtils.log("Installing simpleDetection with detection exit Code 1");
		CommandTestUtils.runCommandExpectedFail("connect " + restUrl + ";install-service --verbose -timeout 1 " + serviceFailDir + ";exit");
		
		assertTrue("service processing unit does not exist", admin.getProcessingUnits().getProcessingUnit(absolutePUName) != null);
		assertTrue("service should not be installed.", !admin.getProcessingUnits().getProcessingUnit(absolutePUName).getStatus().equals(DeploymentStatus.INTACT));
		simplePu = admin.getProcessingUnits().waitFor(absolutePUName, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS);
		assertTrue("Expected processing unit instances of the service to be 1", simplePu.waitFor(1, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS));
		assertTrue("Service " + absolutePUName + " State is RUNNING.", 
				!USMTestUtils.waitForPuRunningState(absolutePUName, 60, TimeUnit.SECONDS, admin));
		CommandTestUtils.runCommandAndWait(unInstallCommand);
		
		LogUtils.log("Bringing detection file to initial state");
		replaceTextInFile(this.startDetectionFile, Integer.toString(1), "EXIT_CODE");
	}


	private void replaceTextInFile(File startDetectionFile, String changeFrom, String changeTo) throws IOException {
		Map<String, String> propsToReplace = new HashMap<String,String>();
		propsToReplace.put(changeFrom, changeTo);
		IOUtils.replaceTextInFile(startDetectionFile, propsToReplace);
	}

	private void initFields() {
		this.installCommand = "connect " + this.restUrl + ";install-service --verbose -timeout 1 " + serviceDir + ";exit";
		this.unInstallCommand = "connect " + this.restUrl + ";uninstall-service --verbose " + this.serviceName + ";exit";
	}
	
}

