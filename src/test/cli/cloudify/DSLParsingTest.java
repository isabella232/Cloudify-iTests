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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

public class DSLParsingTest extends AbstractLocalCloudTest {
	
	public static final String USM__SERVICE_WITH_EVENT_DUPLICATION = "simpleEventParsing";
	public static final String SERVICE_WITH_PROCESSING_UNIT_DUPLICATION = "simpleProcessingUnitParsing";
	private static final String SERVICE_NAME = "simple";
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testServiceEventDuplicationInServiceFile() throws IOException, InterruptedException {
		String usmBadServiceWithEventDuplication = getParsingServicePath(USM__SERVICE_WITH_EVENT_DUPLICATION);
		String absolutePUName = ServiceUtils.getAbsolutePUName(DEFAULT_APPLICATION_NAME, SERVICE_NAME);
		String output = CommandTestUtils.runCommandExpectedFail("connect " + this.restUrl +
				";install-service --verbose -timeout 1 " + usmBadServiceWithEventDuplication + 
				";disconnect;");
		
		ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(absolutePUName, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS);
		assertTrue("Deployed Successfully. Test Failed", 
			processingUnit == null || processingUnit.waitFor(0, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS));
		assertTrue("property duplication was not found.	", output.contains("Property duplication was found"));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void ProcessingUnitDuplicationInServiceFile() throws IOException, InterruptedException {
		
		String usmBadServiceWithEventDuplication = getParsingServicePath(SERVICE_WITH_PROCESSING_UNIT_DUPLICATION);
		String absolutePUName = ServiceUtils.getAbsolutePUName(DEFAULT_APPLICATION_NAME, SERVICE_NAME);
		String output = CommandTestUtils.runCommandExpectedFail("connect " + this.restUrl +
				";install-service --verbose -timeout 1 " + usmBadServiceWithEventDuplication + 
				";disconnect;");
		
		ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(absolutePUName, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS);
		assertTrue("Deployed Successfully. Test Failed", 
			processingUnit == null || processingUnit.waitFor(0, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS));
		assertTrue("processing-unit duplication was not found.	", output.contains("There may only be one type of "
								+ "processing unit defined. Found more than one"));
	}	
	
	private String getParsingServicePath(String dirOrFilename) {
		return CommandTestUtils.getPath("apps/USM/badUsmServices/" + dirOrFilename);
	}
	
}

