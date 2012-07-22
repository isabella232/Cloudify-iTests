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

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TailCommandTest extends AbstractLocalCloudTest {
	
	
	private static final String EXPECTED_SYSTEM_ERR_LOG_ENTRY = "system.err: Still alive...";
	private static final String EXPECTED_SYSTEM_OUT_LOG_ENTRY = "system.out: Still alive...";
	private final String simpleServicePath = CommandTestUtils.getPath("apps/USM/usm/simple");

	@BeforeClass
	public void beforeClass() throws Exception {
		super.beforeClass();
		try {
			runCommand("connect " + this.restUrl + ";install-service " + simpleServicePath);
		} catch (IOException e) {
			e.printStackTrace();
			AssertFail(e.getMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
			AssertFail(e.getMessage());
		}
	}
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testTailByServiceInstanceId() throws IOException, InterruptedException {
		String runCommand = runCommand("connect " + this.restUrl + 
				";tail --verbose -instanceId 1 simple 30; " + "exit");
		assertTrue(runCommand.contains(EXPECTED_SYSTEM_OUT_LOG_ENTRY));
		assertTrue(runCommand.contains(EXPECTED_SYSTEM_ERR_LOG_ENTRY));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testTailByServiceInstanceHostAddress() throws IOException, InterruptedException {
		String runCommand = runCommand("connect " + this.restUrl + 
				";tail --verbose -hostAddress 127.0.0.1 simple 30; " + "exit");
		assertTrue(runCommand.contains(EXPECTED_SYSTEM_OUT_LOG_ENTRY));
		assertTrue(runCommand.contains(EXPECTED_SYSTEM_ERR_LOG_ENTRY));
	}
	
    @Override
    @AfterMethod
    public void afterTest() {
    }
}

