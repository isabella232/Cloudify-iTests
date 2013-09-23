package org.cloudifysource.quality.iTests.test.rest;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.rest.response.InstallApplicationResponse;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractLocalCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.testng.annotations.Test;

/**
 * User: Sagi Bernstein
 * Date: 17/06/13
 * Time: 14:22
 */
public class NewRestApiTest extends AbstractLocalCloudTest {

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = true)
    public void installUninstallTest() throws Exception {
        ApplicationInstaller installer = new ApplicationInstaller(restUrl, "petclinic-simple");
        installer.recipePath(CommandTestUtils.getBuildApplicationsPath() + "/petclinic-simple");
        final Object[] responses = installer.restInstall();
        final InstallApplicationResponse response = (InstallApplicationResponse) responses[0];
        final String deploymentID = response.getDeploymentID();
        Assert.assertTrue(StringUtils.isNotBlank(deploymentID));

        Assert.assertTrue(ServiceUtils.isPortOccupied("localhost", 8080));

        installer.restUninstall();
        Assert.assertFalse(ServiceUtils.isPortOccupied("localhost", 8080));
    }

}
