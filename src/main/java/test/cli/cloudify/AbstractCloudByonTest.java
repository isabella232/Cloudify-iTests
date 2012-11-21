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
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.DSLException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import test.AbstractTest;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class AbstractCloudByonTest extends AbstractTest {

	private static final  String BYON_CLOUD_USER= "tgrid";
	private static final String BYON_CLOUD_PASSWORD = "tgrid";
	private static final  String BYON_SERVER_USER= "tgrid";
	private static final String BYON_SERVER_PASSWORD = "tgrid";
	
	protected static final String WEBUI_PORT = String.valueOf(8099); 
	protected static final String REST_PORT = String.valueOf(8100); 
	private static final String IP_REGEX= "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"; 
	private static final String WEBUI_URL_REGEX= "Webui service is available at: (http://" + IP_REGEX + ":" + WEBUI_PORT +")";
	private static final String REST_URL_REGEX= "Rest service is available at: (http://" + IP_REGEX + ":" + REST_PORT + ")";
	protected static final int NUM_OF_MANAGEMENT_MACHINES = 1;
	protected static final String INSTALL_TRAVEL_EXPECTED_OUTPUT = "Application travel installed successfully";
	protected static final String UNINSTALL_TRAVEL_EXPECTED_OUTPUT = "Application travel uninstalled successfully";
	
    protected URL[] restAdminUrl = new URL[NUM_OF_MANAGEMENT_MACHINES];
    protected URL[] webUIUrl = new URL[NUM_OF_MANAGEMENT_MACHINES];
    
	private File originalByonDslFile;
	private File backupByonDslFile;
	private File targetPem;
    
	@Override
    @BeforeMethod
    public void beforeTest() {
        boolean success = false;
		try {
        	bootstrapCloud();
        	success = true;
		} 
		catch (IOException e) {
			LogUtils.log("bootstrap-cloud failed.", e);
		} 
		catch (InterruptedException e) {
			LogUtils.log("bootstrap-cloud failed.", e);
		} 
		catch (Exception e) {
		    LogUtils.log("bootstrap-cloud failed.", e);
		}
		finally {
        	if (!success) {
        		teardownCloud();
        		Assert.fail("bootstrap-cloud failed.");
        	}
        }
    }
    
	private void bootstrapCloud() throws IOException, InterruptedException, DSLException {
		
		// byon plugin should include a recipe that includes secret key 
		File byonPluginDir = new File(ScriptUtils.getBuildPath() , "tools/cli/plugins/esc/byon/");
		originalByonDslFile = new File(byonPluginDir, "byon-cloud.groovy");
		backupByonDslFile = new File(byonPluginDir, "byon-cloud.backup");
		
		
		// Read file contents
		final String originalDslFileContents = FileUtils.readFileToString(originalByonDslFile);
		
		// first make a backup of the original file
		FileUtils.copyFile(originalByonDslFile, backupByonDslFile);
		
		final String modifiedDslFileContents = originalDslFileContents.replace("ENTER_CLOUD_USER", BYON_CLOUD_USER).
				replace("ENTER_CLOUD_PASSWORD", BYON_CLOUD_PASSWORD).replace("ENTER_SERVER_USER", BYON_SERVER_USER).
				replace("ENTER_SERVER_PASSWORD", BYON_SERVER_PASSWORD);
		FileUtils.write(originalByonDslFile, modifiedDslFileContents);
	
		String output = CommandTestUtils.runCommandAndWait("bootstrap-cloud --verbose byon");

		Pattern webUIPattern = Pattern.compile(WEBUI_URL_REGEX);
		Pattern restPattern = Pattern.compile(REST_URL_REGEX);
		
		Matcher webUIMatcher = webUIPattern.matcher(output);
		Matcher restMatcher = restPattern.matcher(output);
		
		// This is sort of hack.. currently we are outputting this over ssh and locally with different results
		
		assertTrue("Could not find remote (internal) webui url", webUIMatcher.find()); 
		assertTrue("Could not find remote (internal) rest url", restMatcher.find());
		
		String rawWebUIUrl = webUIMatcher.group(1);
		String rawRestAdminUrl = restMatcher.group(1);
		
		webUIUrl[0] = new URL(rawWebUIUrl);
		restAdminUrl[0] = new URL(rawRestAdminUrl);
		
		/*for (int i = 0; i < NUM_OF_MANAGEMENT_MACHINES ; i++) {
			assertTrue("Could not find actual webui url", webUIMatcher.find());
			assertTrue("Could not find actual rest url", restMatcher.find());

			String rawWebUIUrl = webUIMatcher.group(1);
			String rawRestAdminUrl = restMatcher.group(1);
			
			webUIUrl[i] = new URL(rawWebUIUrl);
			restAdminUrl[i] = new URL(rawRestAdminUrl);
		}*/
	}


	@Override
    @AfterMethod
    public void afterTest() {
        teardownCloud();
    }

	private void deleteCloudFiles() throws IOException {
		// undo all the changes we made in the local byon folder
		FileUtils.copyFile(backupByonDslFile, originalByonDslFile);
		FileUtils.deleteQuietly(backupByonDslFile);
		FileUtils.deleteQuietly(targetPem);
		
	}

	private void teardownCloud() {
		
		try {
			CommandTestUtils.runCommandAndWait("teardown-cloud --verbose -force byon");
		} catch (IOException e) {
			Assert.fail("teardown-cloud failed. SHUTDOWN VIRTUAL MACHINES MANUALLY !!!",e);
		} catch (InterruptedException e) {
			Assert.fail("teardown-cloud failed. SHUTDOWN VIRTUAL MACHINES MANUALLY !!!",e);
		}
		finally {
			try {
				deleteCloudFiles();
			} catch (IOException e) {
				AssertFail("Failed to clean up after test finished: " + e.getMessage(), e);
			}
		}
	}
	
}
