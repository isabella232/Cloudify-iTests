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

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.response.UploadResponse;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.util.NewRestTestUtils;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.junit.Assert;
import org.testng.annotations.Test;

/**
 * Checks that the upload directory placed in the REST temp folder ({@link org.cloudifysource.dsl.internal.CloudifyConstants.REST_FOLDER}).
 * 
 * @author yael
 *
 */
public class UploadDirectoryTest extends AbstractLocalCloudTest {

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testUploadDirectory() throws RestClientException, IOException {
		File uploadDir = new File(CloudifyConstants.REST_FOLDER, CloudifyConstants.UPLOADS_FOLDER_NAME);
		Assert.assertTrue("upload directory path is not [" + uploadDir.getAbsolutePath() + "] as expected", uploadDir.exists());
		RestClient restClient = NewRestTestUtils.createAndConnect(restUrl);
		String fileName = "fileToUpload";
		File file = new File(fileName);
		String data = "abc";
		FileUtils.writeStringToFile(file, data);
		UploadResponse uploadResponse = restClient.upload(fileName, file);
		String uploadKey = uploadResponse.getUploadKey();
		File uploadFileFolder = new File(uploadDir, uploadKey);
		Assert.assertTrue("uploaded file folder does not exist at [" + uploadFileFolder.getAbsolutePath() + "]", uploadFileFolder.exists());
		File uploadFile = new File(uploadFileFolder, fileName);
		Assert.assertTrue("uploaded file does not exist at [" + uploadFile.getAbsolutePath() + "]", uploadFile.exists());
		String fileToString = FileUtils.readFileToString(uploadFile);
		Assert.assertEquals(data, fileToString);
	}
}
