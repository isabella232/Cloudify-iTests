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

import junit.framework.Assert;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.ProcessorTypes;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.util.NewRestTestUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * @author yael
 *
 */
public class GetDumpFileTest extends AbstractLocalCloudTest {
	
	
	private static final String LOCAL_HOST = "127.0.0.1";
	


	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testGetPUDumpFile()
            throws Exception {
		File dumpFile = NewRestTestUtils.getProcessingUnitsDumpFile(restUrl, 0, null);
		assertMachineDumpFiles(dumpFile, ProcessorTypes.PROCESSING_UNITS.getName());
	}
	
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testGetPUDumpFileTooLarge()
            throws Exception {
		NewRestTestUtils.getProcessingUnitsDumpFile(restUrl, 5, "exceeds file size limit [5]");
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testGetMachineDumpFile()
            throws Exception {
		File dumpFile = NewRestTestUtils.getMachineDumpFile(restUrl, LOCAL_HOST, null, 0, null);
		assertMachineDumpFiles(dumpFile, null);
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testGetMachineDumpFileLogProcessor()
            throws Exception {
		File dumpFile = NewRestTestUtils.getMachineDumpFile(restUrl, LOCAL_HOST, ProcessorTypes.LOG.getName(), 0, null);
		assertMachineDumpFiles(dumpFile, ProcessorTypes.LOG.getName());
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testGetMachineDumpFileSummaryAndPUProcessors()
            throws Exception {
		String processorsList = ProcessorTypes.PROCESSING_UNITS.getName() + "," + ProcessorTypes.SUMMARY.getName();
		File dumpFile = NewRestTestUtils.getMachineDumpFile(restUrl, LOCAL_HOST, processorsList, 0, null);
		assertMachineDumpFiles(dumpFile, processorsList);
	}


	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testGetMachineDumpFileBlankProcessors()
            throws Exception {
		File dumpFile = NewRestTestUtils.getMachineDumpFile(restUrl, LOCAL_HOST, "", 0, null);
		assertMachineDumpFiles(dumpFile, null);
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testGetMachineDumpFileUnknownProcessor()
            throws Exception {
		NewRestTestUtils.getMachineDumpFile(restUrl, LOCAL_HOST, "unknown", 5, "Unknown processor type: unknown");
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testGetMachineDumpFileTooLarge()
            throws Exception {
		NewRestTestUtils.getMachineDumpFile(restUrl, LOCAL_HOST, null, 5, "exceeds file size limit [5]");
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testGetMachinesDumpFile()
            throws Exception {
		Map<String, File> dumpFiles = NewRestTestUtils.getMachinesDumpFile(restUrl);
		for (Entry<String, File> entry : dumpFiles.entrySet()) {
			assertMachineDumpFiles(entry.getValue(), null);
		}
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testGetMachinesDumpFileTooLarge()
            throws Exception {
		Map<String, File> dumpFiles = NewRestTestUtils.getMachinesDumpFile(restUrl, 0);
		for (Entry<String, File> entry : dumpFiles.entrySet()) {
			assertMachineDumpFiles(entry.getValue(), null);
		}
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testGetMachinesDumpFileUnknownProcessor()
            throws Exception {
		NewRestTestUtils.getMachinesDumpFile(restUrl, "unknown", 5, "Unknown processor type: unknown");
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testGetMachinesDumpFilelogProcessor()
            throws Exception {
		Map<String, File> dumpFiles = NewRestTestUtils.getMachinesDumpFile(restUrl, ProcessorTypes.LOG.getName(), 0, null);
		for (Entry<String, File> entry : dumpFiles.entrySet()) {
			assertMachineDumpFiles(entry.getValue(), ProcessorTypes.LOG.getName());
		}
	}
	
	private Map<String, String[]> getFiles(File dumpFile) throws IOException {
		Map<String, String[]> files = new HashMap<String, String[]>();

		String prefix = FilenameUtils.getBaseName(dumpFile.getName());
		File directory = new File(dumpFile.getParent(), prefix);
		directory.mkdir();
		try {
			ZipUtils.unzip(dumpFile, directory);
			File[] listFiles = directory.listFiles();
			for (File file : listFiles) {
				files.put(file.getName(), file.list());
			}
			return files;
		} finally {
			directory.delete();
		}
	}
	
	private void assertMachineDumpFiles(final File dumpZipFile, final String processors) throws IOException {
		String actualProcessors = processors;
		if (StringUtils.isBlank(processors)) {
			actualProcessors = ProcessorTypes.DEFAULT_PROCESSORS;
		}
		Assert.assertTrue(dumpZipFile.getName().endsWith(".zip"));
		Map<String, String[]> filesMap = getFiles(dumpZipFile);
		for (Entry<String, String[]> entry : filesMap.entrySet()) {
			String folderName = entry.getKey();	
			String[] files = entry.getValue();
			for (String name : actualProcessors.split(",")) {
				ProcessorTypes type = ProcessorTypes.fromStringName(name);
				if (ProcessorTypes.PROCESSING_UNITS.equals(type)) {
					if (!folderName.startsWith("gsc")) {
						continue;
					}
				}
				String fileName = type.getFileName();
				Assert.assertTrue("file " + name + " doesn't exist at " + folderName, ArrayUtils.contains(files, fileName));
			}
		}
	}
}
