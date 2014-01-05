/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import java.io.File;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.byon.ByonCloudService;
import org.cloudifysource.quality.iTests.test.cli.cloudify.util.NewRestTestUtils;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ByonHostListParsingTest extends AbstractByonCloudTest {
	private final static String SIMPLE_RECIPE_FOLDER = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/simple");
	private final static String SIMPLE_NAME = "simple";
		
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		ByonCloudService service = new ByonCloudService();
		String ipList = service.getIpList();
		service.setIpList("," + ipList);
		super.bootstrap(service);
	}
	
	@Override
	protected void parseBootstrapOutput(String bootstrapOutput) throws Exception {
		
		String failedToResolvedNodeMsg = "Failed to resolve node";
		int begin = bootstrapOutput.indexOf(failedToResolvedNodeMsg);
		if (begin != -1) {			
			int end = bootstrapOutput.indexOf(CloudifyConstants.NEW_LINE, begin);
			Assert.fail("failed to resolved node: " + end);
		}
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testInstallService() throws Exception {
		NewRestTestUtils.installServiceUsingNewRestAPI(getRestUrl(), new File(SIMPLE_RECIPE_FOLDER), SIMPLE_NAME);
	}
	
}
