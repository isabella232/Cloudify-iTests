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

import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * GS-11559
 *
 * @author Eli Polonsky
 */
public class UninstallAfterFaultyInstallTest extends NewAbstractCloudTest {

    /**
     * Bootstrap management machine.
     * @throws Exception - Failure.
     */
    @BeforeClass(alwaysRun = true)
    public void bootstrap() throws Exception {
        super.bootstrap();
    }

    /**
     * 1. Install a service so that it fails on memory validation (for example, large reserved memory)
     * 2. Uninstall the service.
     * 3. Verify the uninstall succeeds and doesnt get stuck in a loop.
     *
     * @throws Exception - Failure.
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4)
    public void testUninstallAfterFaultyMemoryConfigurationInstall() throws Exception {

        ServiceInstaller installer = new ServiceInstaller(getRestUrl(), "tomcat");
        installer.recipePath("tomcat");
        installer.timeoutInMinutes(5);
        installer.expectToFail(true);
        installer.install();

        installer.expectToFail(false);

        installer.uninstall();

    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();

        // increase the reserved memory capacity so that services wont be able to install.
        getService().getAdditionalPropsToReplace().put("reservedMemoryCapacityPerMachineInMB 1024",
                "reservedMemoryCapacityPerMachineInMB 10240");
        getService().getAdditionalPropsToReplace().put("machineMemoryMB 5850", "machineMemoryMB 16000");
    }

    /**
     * Teardown the management machine.
     *
     * @throws Exception - Failure.
     */
    @AfterClass(alwaysRun = true)
    public void teardown() throws Exception {
        super.teardown();
    }

    @Override
    protected String getCloudName() {
        return "byon";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }
}
