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
package org.cloudifysource.quality.iTests.test.cli.cloudify;

import java.io.IOException;

import junit.framework.Assert;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.rest.request.SetServiceInstancesRequest;
import org.cloudifysource.dsl.rest.response.InstallServiceResponse;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.util.NewRestTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.util.exceptions.WrongMessageException;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.testng.annotations.Test;

public class SetServiceInstancesIllegalCountTest extends AbstractLocalCloudTest {
	private static final String APP_NAME = CloudifyConstants.DEFAULT_APPLICATION_NAME;
	private static final String SERVICE_NAME = "tomcat";
	private static final int SET_INSTANCES_TIMEOUT = 1;
	private static final int UNINSTALL_SERVICE_TIMEOUT = 5;
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void illegalZeroCountTest() 
			throws DSLException, WrongMessageException, PackagingException, RestClientException, IOException {
		illegalCountTest(0);
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void illegalNegativeCountTest() 
			throws DSLException, WrongMessageException, PackagingException, RestClientException, IOException  {
		illegalCountTest(-1);
	}
	
	private void illegalCountTest(final int count) 
			throws DSLException, WrongMessageException, PackagingException, RestClientException, IOException {
		
		InstallServiceResponse installServiceResponse = NewRestTestUtils.installServiceUsingNewRestAPI(restUrl, APP_NAME, SERVICE_NAME);
		
		SetServiceInstancesRequest request = new SetServiceInstancesRequest();
		request.setCount(-1);
		request.setTimeout(SET_INSTANCES_TIMEOUT);
		try {
			NewRestTestUtils.setServiceInstances(restUrl, request, APP_NAME, SERVICE_NAME, null);
			Assert.fail("expected RestClientException for count parameter equals zero in setInstances command.");
		} catch (RestClientException e) {
			String messageCode = e.getMessageCode();
			Assert.assertEquals("expected error message code to be - " + CloudifyErrorMessages.INVALID_INSTANCES_COUNT.getName() 
					+ " but got - " + messageCode, CloudifyErrorMessages.INVALID_INSTANCES_COUNT.getName(), messageCode);
		}
		
		NewRestTestUtils.uninstallServiceUsingNewRestClient(restUrl, SERVICE_NAME, installServiceResponse.getDeploymentID(), UNINSTALL_SERVICE_TIMEOUT);
	}
}
