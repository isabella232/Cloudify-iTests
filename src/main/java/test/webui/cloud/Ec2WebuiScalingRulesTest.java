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

package test.webui.cloud;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.scale.AbstractScalingRulesCloudTest;
import test.webui.WebuiTestUtils;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.MainNavigation;
import com.gigaspaces.webuitf.dashboard.DashboardTab;
import com.gigaspaces.webuitf.dashboard.events.DashboardEventsGrid;
import com.gigaspaces.webuitf.events.WebUIAdminEvent;

import framework.utils.AssertUtils;
import framework.utils.LogUtils;


public class Ec2WebuiScalingRulesTest extends AbstractScalingRulesCloudTest{

	private WebuiTestUtils webuiHelper;
	private String[] expectedEvents = {"Starting a new machine", "Started new machine", "Installing agent", "Started agent", "Scaling out.", "Scaling in.", "Stopping agent", "Stopped agent", "Stopping machine", "is stopped"};
	
	@Override
	protected String getCloudName() {
		return "ec2";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
		try {
			webuiHelper = new WebuiTestUtils(cloudService);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeMethod
	public void startExecutorService() {	
		super.startExecutorService();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 3, enabled = true)
	public void testPetclinicSimpleScalingRules() throws Exception {
		
		LoginPage loginPage = webuiHelper.getLoginPage();
		MainNavigation mainNavigation = loginPage.login();
		DashboardTab dashboard = mainNavigation.switchToDashboard();
		DashboardEventsGrid eventsGrid = dashboard.getDashboardSubPanel().switchToEventsGrid();
		eventsGrid.switchToAllEvents();
		
		super.testPetclinicSimpleScalingRules();
		
		List<WebUIAdminEvent> allEvents = eventsGrid.getVisibleEvents();
		boolean found = false;
		
		for(String expectedEventDescription : expectedEvents){
			
			for(WebUIAdminEvent event : allEvents){
				if(event.getDescription().indexOf(expectedEventDescription) != -1){
					found = true;
					LogUtils.log("description: " + event.getDescription());
					break;
				}
			}
			
			if (!found){
				AssertUtils.AssertFail("could not find event with description: " + expectedEventDescription);
			}
			
			found = false;
		}
		
	}
		
	@AfterMethod
	public void cleanup() throws IOException, InterruptedException {
		super.cleanup();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
		try {
			webuiHelper.close();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected void beforeTeardown() throws Exception {
		super.uninstallApplicationIfFound("petclinic");
	}

}
