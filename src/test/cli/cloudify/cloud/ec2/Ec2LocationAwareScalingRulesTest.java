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

package test.cli.cloudify.cloud.ec2;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.AbstractScalingRulesCloudTest;
import framework.utils.TestUtils;


public class Ec2LocationAwareScalingRulesTest extends AbstractScalingRulesCloudTest{

	private static final String LOCATION_AWARE_POSTFIX = "-location-aware";
	private static final String NEWLINE = System.getProperty("line.separator");

	@Override
	protected String getCloudName() {
		return "ec2" + LOCATION_AWARE_POSTFIX;
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 3, enabled = false)
	public void testPetclinicSimpleScalingRules() throws Exception {
		super.testPetclinicSimpleScalingRules();
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}
	
	@BeforeTest
	public void beforeTest() {	
		cloneApplicaitonRecipeAndInjectLocationAware();
		super.beforeTest();	
	}

	private void cloneApplicaitonRecipeAndInjectLocationAware() {
		
		try {
			FileUtils.copyDirectory(new File(super.getApplicationPath()), new File(this.getApplicationPath()));
			final File newServiceFile = new File(this.getApplicationPath(),"tomcat/tomcat-service.groovy");
			TestUtils.writeTextFile(newServiceFile,"service {" +NEWLINE +"\textend \"../../../services/tomcat\""+NEWLINE+"\tlocationAware true "+NEWLINE+"}");
		} catch (final IOException e) {
			Assert.fail("Failed to create " + this.getApplicationPath(),e);
		}
	}
	
	@AfterTest
	public void afterTest() {
		super.afterTest();
	}
	
	@AfterMethod
	public void cleanUp() {
		//The afterTest Checks for leaks
		super.afterTest();
	}

	@Override
	protected String getApplicationPath() {
		final File applicationPath = new File(super.getApplicationPath());
		final File newApplicationPath = new File(applicationPath.getParentFile(), applicationPath.getName()+LOCATION_AWARE_POSTFIX);
		return newApplicationPath.getAbsolutePath();
	}
	
	
}
