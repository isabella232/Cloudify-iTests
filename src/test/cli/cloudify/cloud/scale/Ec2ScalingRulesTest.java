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

package test.cli.cloudify.cloud.scale;

import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class Ec2ScalingRulesTest extends AbstractScalingRulesCloudTest{

	@Override
	protected String getCloudName() {
		return "ec2";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}

	@BeforeMethod
	public void startExecutorService() {	
		super.startExecutorService();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 3, enabled = true)
	public void testPetclinicSimpleScalingRules() throws Exception {
		super.testPetclinicSimpleScalingRules();
	}
	
	
	@AfterMethod
	public void cleanup() {
		super.cleanup();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}

}
