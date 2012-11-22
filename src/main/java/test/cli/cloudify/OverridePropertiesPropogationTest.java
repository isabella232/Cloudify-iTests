package test.cli.cloudify;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.testng.annotations.Test;

import framework.tools.SGTestHelper;
import framework.utils.ApplicationInstaller;
import framework.utils.AssertUtils;
import framework.utils.ServiceInstaller;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

public class OverridePropertiesPropogationTest extends AbstractLocalCloudTest {
	
	private static final String TOMCAT_SERVICE_PATH = SGTestHelper.getBuildDir() + "/recipes/services/tomcat";
	private static final String TRAVEL_APP_PATH = SGTestHelper.getBuildDir() + "/recipes/apps/travel";
	private static final Map<String, Object> overrides = new HashMap<String, Object>();
	
	static {
		try {
			ConfigObject config = new ConfigSlurper().parse(new File(TOMCAT_SERVICE_PATH, "tomcat-service.properties").toURI().toURL());
			overrides.put("port", (Integer) config.get("port") + 1);
		} catch (MalformedURLException e) {
			
		}
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testInstallService() throws IOException, InterruptedException {
		
		ServiceInstaller tomcatInstaller = new ServiceInstaller(restUrl, "tomcat");
		tomcatInstaller.setRecipePath(TOMCAT_SERVICE_PATH);
		tomcatInstaller.setOverrides(overrides);
		
		tomcatInstaller.install();
		
		AssertUtils.assertTrue("Port " + overrides.get("port") + " should have occupied!", ServiceUtils.isPortOccupied((Integer)overrides.get("port")));
		
		tomcatInstaller.uninstall();
		
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testInstallApplication() throws IOException, InterruptedException {

		ApplicationInstaller travelInstaller = new ApplicationInstaller(restUrl, "travel");
		travelInstaller.setRecipePath(TRAVEL_APP_PATH);
		travelInstaller.setOverrides(overrides);
		
		travelInstaller.install();
		
		AssertUtils.assertTrue("Port " + overrides.get("port") + " should have occupied!", ServiceUtils.isPortOccupied((Integer)overrides.get("port")));
		
		travelInstaller.uninstall();
	}

}
