package org.cloudifysource.quality.iTests.test.cli.cloudify;

import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.testng.annotations.Test;

public class BadUSMServiceDownAfterUninstallApplicationTest extends AbstractLocalCloudTest {

	private static final String APPLICATION_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/badUsmServices/simpleApplication");

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
    public void badUsmServiceDownTest() throws Exception {
        
        ApplicationInstaller installer = new ApplicationInstaller(restUrl, "simple");
        installer.recipePath(APPLICATION_PATH);
        installer.expectToFail(true).timeoutInMinutes(2);
        installer.install();
        
        installer.expectToFail(false);
        installer.uninstall();
    }

}
