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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.testng.annotations.Test;

/**
 * 
 * @author yael
 *
 */
public class GetDumpFileTest extends AbstractLocalCloudTest {
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testGetPUDumpFile() 
			throws IOException, RestClientException {
		File dumpFile = NewRestTestUtils.getPUDumpFile(restUrl, 0, null);
		System.out.println(dumpFile.getAbsolutePath());
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testGetMachineDumpFile() 
			throws RestClientException, IOException {
		String ip = "127.0.0.1";
		File dumpFile = NewRestTestUtils.getMachineDumpFile(restUrl, ip, null, 0, null);
		System.out.println(dumpFile.getAbsolutePath());
	}
	

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testGetMachinesDumpFile() 
			throws IOException, RestClientException {
		Map<String, File> dumpFiles;
		try {
			dumpFiles = NewRestTestUtils.getMachinesDumpFile(restUrl, null, 0, null);
		} catch (RestClientException e) {
			System.out.println(e.getMessageFormattedText());
			e.printStackTrace();
			throw e;
		}
		for (Entry<String, File> entry : dumpFiles.entrySet()) {
			System.out.println("machine [" + entry.getKey() + "] : " + entry.getValue());
		}
	}
}
