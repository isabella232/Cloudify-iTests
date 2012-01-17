package test.cli.cloudify;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.httpclient.HttpStatus;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLServiceCompilationResult;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.internal.gsm.InternalGridServiceManager;
import org.testng.annotations.Test;
/**
 * This test verifies the existence of an icon in the webUI when deploying a service or an application.
 * @author adaml
 *
 */
public class IconDisplayTest extends AbstractLocalCloudTest {
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testServiceIcon() throws PackagingException, DSLException, MalformedURLException, IOException, InterruptedException{
		
		String serviceDir = CommandTestUtils.getPath("apps/USM/usm/applications/simple/simple");
		runCommand("connect " + restUrl + ";install-service " + serviceDir + ";exit");
		
		DSLServiceCompilationResult compilationResult = ServiceReader.getServiceFromDirectory(new File(serviceDir), "default");
		Service service = compilationResult.getService();
		String iconPath = getIconPath(service, "default");
		
		//test icon path
		int responseCode = getResponseCode(iconPath);
		assertTrue("Can not find the icon under: " + iconPath, responseCode == HttpStatus.SC_OK);
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testApplicationIcon() throws PackagingException, DSLException, MalformedURLException, IOException, InterruptedException{
		String applicationDir = CommandTestUtils.getPath("apps/USM/usm/applications/simple");
		String applicationServiceDir = CommandTestUtils.getPath("apps/USM/usm/applications/simple/simple");
		
		runCommand("connect " + restUrl + ";install-application " + applicationDir + ";exit");
		
		DSLServiceCompilationResult compilationResult = ServiceReader.getServiceFromDirectory(new File(applicationServiceDir), "simple");
		Service service = compilationResult.getService();
		
		//test icon path
		String iconPath = getIconPath(service, "simple");
		int responseCode = getResponseCode(iconPath);
		
		assertTrue("Can not find the icon under: " + iconPath, responseCode == HttpStatus.SC_OK);
		
	}

	private String getIconPath(Service service, String applicationName) {
		String iconURI = service.getIcon();
		String codeBaseUrl = getCodeBaseUrl();
		String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, service.getName());
		return codeBaseUrl + "/" + absolutePUName + "/ext/" + iconURI;
		
	}

	//returns the webster url.
	private String getCodeBaseUrl() {
		GridServiceManager gsm = admin.getGridServiceManagers().waitForAtLeastOne();
		InternalGridServiceManager igsm = InternalGridServiceManager.class.cast(gsm);
		return igsm.getCodeBaseURL();
	}
	
	public static int getResponseCode(String urlString) throws MalformedURLException, IOException {
	    URL url = new URL(urlString); 
	    HttpURLConnection huc =  (HttpURLConnection)url.openConnection(); 
	    huc.setRequestMethod("GET"); 
	    huc.connect(); 
	    return huc.getResponseCode();
	}
	
}
