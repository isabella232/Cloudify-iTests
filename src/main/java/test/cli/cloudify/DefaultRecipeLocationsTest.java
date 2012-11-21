package test.cli.cloudify;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

public class DefaultRecipeLocationsTest extends AbstractLocalCloudTest {
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testInstallService() throws IOException, InterruptedException {
		CommandTestUtils.runCommandAndWait("connect " + restUrl + 
				";install-service --verbose tomcat" );
		CommandTestUtils.runCommandAndWait("connect " + restUrl + 
				";uninstall-service --verbose tomcat" );
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testInstallApplication() throws IOException, InterruptedException {
		CommandTestUtils.runCommandAndWait("connect " + restUrl + 
				";install-application --verbose petclinic-simple" );
		CommandTestUtils.runCommandAndWait("connect " + restUrl + 
				";uninstall-application --verbose petclinic" );
	}
	
	/**
	 * This test installs an application located in SGTest. the application uses services that extend our
	 * official recipes, but the extend property is set in this manner : extend "tomcat" (as opposed to extend "../../services/tomcat")
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testInstallApplicationWithDefaultExtend() throws IOException, InterruptedException {
		
		String applicationPath = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/petclinic-simple-default-extend");
		
		CommandTestUtils.runCommandAndWait("connect " + restUrl + 
				";install-application --verbose " + applicationPath );
		CommandTestUtils.runCommandAndWait("connect " + restUrl + 
				";uninstall-application --verbose petclinic" );
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testInstallApplicationUnderCurrentDirectory() throws IOException, InterruptedException {
		
		// first copy the application to the current directory
		File currentDir = new File(".");
		// just take some application that is not in the official build
		String applicationPath = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/simple");
		FileUtils.copyDirectoryToDirectory(new File(applicationPath), currentDir);
		
		CommandTestUtils.runCommandAndWait("connect " + restUrl + 
				";install-application --verbose simple" );

		CommandTestUtils.runCommandAndWait("connect " + restUrl + 
				";uninstall-application --verbose simple" );

		// delete after test ends.
		FileUtils.deleteDirectory(new File(currentDir.getAbsolutePath(),"simple"));
	}

}
