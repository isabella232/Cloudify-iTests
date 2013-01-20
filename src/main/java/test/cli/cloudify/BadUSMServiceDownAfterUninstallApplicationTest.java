package test.cli.cloudify;

import java.io.IOException;

import org.testng.annotations.Test;

import framework.utils.ApplicationInstaller;

public class BadUSMServiceDownAfterUninstallApplicationTest extends AbstractLocalCloudTest {

	private static final String APPLICATION_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/badUsmServices/simpleApplication");

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
    public void badUsmServiceDownTest() throws IOException, InterruptedException {
        
        ApplicationInstaller installer = new ApplicationInstaller(restUrl, "simple");
        installer.recipePath(APPLICATION_PATH);
        installer.expectToFail(true).timeoutInMinutes(2);
        installer.install();
        
        installer.expectToFail(false);
        installer.uninstall();
    }

}
