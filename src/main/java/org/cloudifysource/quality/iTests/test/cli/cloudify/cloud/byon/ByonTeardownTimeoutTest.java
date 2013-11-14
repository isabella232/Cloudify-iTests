/*
 * ******************************************************************************
 *  * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  ******************************************************************************
 */

package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import com.google.common.io.Resources;
import iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudServiceManager;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.byon.ByonCloudService;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URL;

/**
 * CLOUDIFY-2196.
 *
 * @author Eli Polonsky
 * @since 3.0.0
 */
public class ByonTeardownTimeoutTest extends AbstractByonCloudTest {

    /**
     * Bootstrap the cloud.
     * @throws Exception In case of an unexpected error.
     */
    @BeforeClass(alwaysRun = true)
    public void bootstrap() throws Exception {
        ByonCloudService service = (ByonCloudService) CloudServiceManager.getInstance().getCloudService(getCloudName());
        URL resource = Resources.getResource("org/cloudifysource/quality/iTests/test/byonteardowntimeouttest/byon" +
                "-cloud" +
                ".groovy");
        service.setCloudGroovy(new File(resource.getPath()));
        super.bootstrap(service);
    }

    /**
     * Change the teardown timeout to 0 and make sure a TimeoutException is shown.
     * @throws Exception In case of an unexpected error.
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void testTimeout() throws Exception {
        getService().getBootstrapper().verbose(true).teardownExpectedToFail(true);
        super.teardown();
        String teardownOutput = getService().getBootstrapper().getLastActionOutput();
        AssertUtils.assertTrue("Teardown output does not contain a timeout error",
                teardownOutput.contains("TimeoutException"));
    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        getService().getAdditionalPropsToReplace().put("\"org.cloudifysource.stop-management-timeout-in-minutes\" : 15",
                "\"org.cloudifysource.stop-management-timeout-in-minutes\" : 0");

    }
}
