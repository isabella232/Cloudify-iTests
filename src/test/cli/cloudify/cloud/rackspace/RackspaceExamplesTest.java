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
 *******************************************************************************/

package test.cli.cloudify.cloud.rackspace;

import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.AbstractExamplesTest;

/**
 * This class runs two test on Rackspace cloud.
 * 
 * 1. bootstrap to cloud
 * 2. run travel
 * 3. uninstall travel.
 * 4. run petclinic
 * 5. uninstall petclinic
 * 6. teardown cloud
 * 7. scan for any leaked machines
 * 
 * @author elip
 *
 */
public class RackspaceExamplesTest extends AbstractExamplesTest {
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testTravel() throws Exception { 
		super.testTravel();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testPetclinic() throws Exception {
		super.testPetclinic();
	}

	@AfterMethod(alwaysRun = true)
	public void cleanUp() {
		super.uninstallApplicationIfFound();
	}

	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}
	
	@Override
	protected String getCloudName() {
		return "rackspace";
	}
	
	@Override
	protected void customizeCloud() {

		//removed this since privileged true should be in the dsl file
		
//		final HpCloudService hpService = (HpCloudService) cloud;
//
//		hpService.setAdditionalPropsToReplace(new HashMap<String, String>());
//		hpService.getAdditionalPropsToReplace().put("imageId \"221\"",
//				"imageId \"221\"" + System.getProperty("line.separator") + "\t\t\t\t\tprivileged true");
	}
}
