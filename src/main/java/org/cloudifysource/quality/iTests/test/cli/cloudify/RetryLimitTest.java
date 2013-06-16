/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ******************************************************************************/
package org.cloudifysource.quality.iTests.test.cli.cloudify;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyConstants.USMState;
import org.cloudifysource.dsl.internal.space.ServiceInstanceAttemptData;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceLifecycleEventListener;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.openspaces.pu.service.ServiceMonitors;
import org.testng.annotations.Test;

public class RetryLimitTest extends AbstractLocalCloudTest {

	private final class AdminListener implements ProcessingUnitInstanceLifecycleEventListener {
		private int instanceAdded;
		private int instanceRemoved;
		private final Object mutex;

		public AdminListener(final Object mutex) {
			this.mutex = mutex;
		}

		@Override
		public void processingUnitInstanceRemoved(ProcessingUnitInstance pui) {
			String puName = pui.getProcessingUnit().getName();
			if (puName.equals(absolutePUName)) {
				LogUtils.log("Instance was removed");
				instanceRemoved++;
			}
		}

		@Override
		public void processingUnitInstanceAdded(ProcessingUnitInstance pui) {
			String puName = pui.getProcessingUnit().getName();
			if (puName.equals(absolutePUName)) {
				LogUtils.log("Instance was added");
				instanceAdded++;
				if (instanceAdded == 2) {
					synchronized (mutex) {
						mutex.notify();
					}
				}
			}

		}

		public void assertResults() {
			assertEquals("Expected instance added events are missing", 2, instanceAdded);
			assertEquals("Expected instance removed events are missing", 1, instanceRemoved);
		}
	}

	private final String serviceName = "groovyError";
	private final String applicationName = "default";
	private final String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
	private final String serviceDir = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/groovy-error");

	private String installCommand;
	private String unInstallCommand;

	/**
	 * The proper behavior of the start detection should be: if the start detection is a java groovy file, exiting with
	 * exit code 0 should be considered as true and any other exit code should be considered as false.
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws RestClientException
	 */
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, groups = "1")
	public void retryLimitTest() throws IOException, InterruptedException, RestClientException {

		Space adminSpace = admin.getSpaces().waitFor(CloudifyConstants.MANAGEMENT_SPACE_NAME, 10, TimeUnit.SECONDS);
		assertNotNull(adminSpace);
		GigaSpace managementSpace = adminSpace.getGigaSpace();
		ServiceInstanceAttemptData attemptData = managementSpace.read(new ServiceInstanceAttemptData());
		assertTrue("Found attempt data in space before test started", attemptData == null);

		initFields();
		final Object mutex = new Object();
		AdminListener eventListener = new AdminListener(mutex);

		try {

			this.admin.getProcessingUnits().addLifecycleListener(eventListener);

			CommandTestUtils.runCommand(installCommand);

			synchronized (mutex) {
				mutex.wait(60000);
			}
			eventListener.assertResults();

			final ProcessingUnitInstance pui = admin.getProcessingUnits().getProcessingUnit(absolutePUName).getInstances()[0];
			repetitiveAssertTrue("Instance did not reach ERROR state", new AssertUtils.RepetitiveConditionProvider() {

				@Override
				public boolean getCondition() {
					ServiceMonitors serviceMonitors = pui.getStatistics().getMonitors().get("USM");
					assertNotNull(serviceMonitors);
					Object usmState = serviceMonitors.getMonitors().get("USM_State");
					LogUtils.log("USM state is: " + usmState);
					assertEquals(USMState.ERROR.ordinal(), usmState);
					return true;
				}
			}, 30000);

		} finally {
			admin.getProcessingUnits().removeLifecycleListener(eventListener);
			CommandTestUtils.runCommandAndWait(unInstallCommand);
			ServiceInstanceAttemptData attemptDataAfterInstall = managementSpace.read(new ServiceInstanceAttemptData());
			assertTrue("Found attempt data in space after uninstall", attemptDataAfterInstall == null);

		}

	}

	private void initFields() {
		this.installCommand =
				"connect " + this.restUrl + ";install-service --verbose -timeout 1 " + serviceDir + ";exit";
		this.unInstallCommand =
				"connect " + this.restUrl + ";uninstall-service --verbose " + this.serviceName + ";exit";
	}

}
