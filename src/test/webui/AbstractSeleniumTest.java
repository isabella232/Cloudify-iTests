package test.webui;

import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverBackedSelenium;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import test.AbstractTest;
import test.webui.objects.LoginPage;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

import framework.tools.SGTestHelper;
import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.IOUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.ScriptUtils;
import framework.utils.ScriptUtils.RunScript;


/**
 * This abstract class is the super class of all Selenium tests, every test class must inherit this class. 
 * Contains only annotated methods witch are invoked according to the annotation.
 * @author elip
 *
 */

public abstract class AbstractSeleniumTest extends AbstractTest {
	
    private final String scriptName = "../tools/gs-webui/gs-webui";
    private final static String baseUrlApache = "http://localhost:" + System.getenv("apache.port")  + "/gs-webui/";
    private final static String apachelb = "../tools/apache/apache-lb-agent -apache " + '"' + System.getenv("apache.home") + '"';
    
    protected final static String baseUrl = "http://localhost:8099/";
    protected final static String originalAlertXml = SGTestHelper.getSGTestRootDir() + "/src/test/webui/resources/alerts/alerts.xml";
    protected final static int FIREFOX = 0;
    protected final static int CHROME = 1;

    protected final static String SUB_TYPE_CONTEXT_PROPERTY = "com.gs.service.type";
    protected final static String APPLICATION_CONTEXT_PROPERY = "com.gs.application";
    protected final static String DEPENDS_ON_CONTEXT_PROPERTY = "com.gs.application.dependsOn";
    protected final static String LICENSE_PATH = SGTestHelper.getBuildDir() + "/gslicense.xml";
    
    protected static long waitingTime = 30000;

    private RunScript scriptWebUI;
    private RunScript scriptLoadBalancer;
    private WebDriver driver;
    private Selenium selenium;
    private ProcessingUnit webSpace;
    private GridServiceManager webUIGSM;
    private ProcessingUnit gswebui;
    
    private final String defaultBrowser = 
    	(System.getProperty("selenium.browser") != null) ? System.getProperty("selenium.browser"): "Firefox";
    
    private List<Selenium> seleniumBrowsers = new ArrayList<Selenium>();
    
    public GridServiceManager getWebuiManagingGsm() {
    	return webUIGSM;
    }
    
    public boolean isStartWebServerFromBatch() {
    	return true;
    }
    
    @BeforeSuite(alwaysRun = true)
    public void getDefaultBrowser() {
    	LogUtils.log("default browser is : " + defaultBrowser);
    }
    
    /**
     * starts the web-ui browser from the batch file in gigaspaces
     * also opens a browser and connect to the server
     * @throws Exception 
     */
    @BeforeMethod(alwaysRun = true)
    public void startWebServices() throws Exception { 
    	if (isStartWebServerFromBatch()) {
    		startWebServer();
    	}
    	else {
   			replaceBalancerTemplate();
    		startLoadBalancerWebServer();
    	}
    	startWebBrowser(baseUrl);
    }
    
    /**
     * stops the server and kills all open browsers
     * @throws InterruptedException 
     * @throws IOException 
     */
    @AfterMethod(alwaysRun = true)
    public void killWebServices() throws IOException, InterruptedException {  
    	try {
    		if (isStartWebServerFromBatch()) {
    			stopWebServer();
    		}
    		else {
    			stopLoadBalancerWebServer();
    		}
    		stopWebBrowser();
    	}
    	finally {
    		restorePreviousBrowser();
    	}

    }
    
    public void startWebServer() throws Exception {	
    	LogUtils.log("Starting webui server...");
    	scriptWebUI = ScriptUtils.runScript(scriptName);
    	Thread.sleep(5000);
    }
    
    public void stopWebServer() throws IOException, InterruptedException {
    	LogUtils.log("Killing web server...");
    	if (scriptWebUI != null) {
    		scriptWebUI.kill();
    	}
    	Thread.sleep(5000);
    }
    
    public void startWebBrowser(String uRL) throws InterruptedException {
    	try {
    		LogUtils.log("Launching browser...");
    		String browser = System.getProperty("selenium.browser");
    		LogUtils.log("Current browser is " + browser);
    		if (browser == null) {
    			driver = new FirefoxDriver();
    		}
    		else {
    			if (browser.equals("Firefox")) {
    				driver = new FirefoxDriver();

    			}
    			else {
    				if (browser.equals("IE")) {
    					DesiredCapabilities desired = DesiredCapabilities.internetExplorer();
    					desired.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
    					driver = new InternetExplorerDriver(desired);
    				}
    				else {
    					System.setProperty("webdriver.chrome.driver", SGTestHelper.getSGTestRootDir() + "/src/test/webui/resources/chromedriver.exe");
    					DesiredCapabilities desired = DesiredCapabilities.chrome();
    					desired.setCapability("chrome.switches", Arrays.asList("--start-maximized"));
    					driver = new ChromeDriver(desired);
    				}
    			}
    		}
    		int seconds = 0;
    		driver.get(uRL);
    		selenium = new WebDriverBackedSelenium(driver, uRL);
    		seleniumBrowsers.add(selenium);
    		Thread.sleep(3000);
    		while (seconds < 10) {
    			try {
    				driver.findElement(By.xpath(WebConstants.Xpath.loginButton));
    				LogUtils.log("Web server connection established");
    				break;
    			}
    			catch (NoSuchElementException e) {
    				LogUtils.log("Unable to connect to Web server, retrying...Attempt number " + (seconds + 1));
    				driver.navigate().refresh();
    				Thread.sleep(1000);
    				seconds++;
    			}
    		}
    		if (seconds == 10) {
    			LogUtils.log("Could not establish a connection to webui server, Test will fail");
    		}
    	}
    	catch (WebDriverException e) {
    		LogUtils.log("Failed to launch browser, The test should fail on an NPE", e);
    	}
    }
    
    public void stopWebBrowser() throws InterruptedException {
    	LogUtils.log("Killing browser...");
    	for (Selenium selenium : seleniumBrowsers) {
    		selenium.stop();
    		Thread.sleep(1000);
    	}
    }
    
    private void startLoadBalancerWebServer() throws Exception {
    	
       	log("launching web server");
		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);
		
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];
		
		Machine machineA = gsaA.getMachine();
		
		log("loading GSM");
		webUIGSM = loadGSM(machineA);
		
		log("loading 2 gsc's on one machine");
		AdminUtils.loadGSCWithSystemProperty(machineA, "-Dorg.eclipse.jetty.level=ALL");	
		AdminUtils.loadGSCWithSystemProperty(machineA, "-Dorg.eclipse.jetty.level=ALL");
        log("deploying the space");
        webSpace = webUIGSM.deploy(new SpaceDeployment("webSpace").numberOfInstances(1)
                .numberOfBackups(1).maxInstancesPerVM(1).setContextProperty("com.gs.application", "gs-webui"));
        ProcessingUnitUtils.waitForDeploymentStatus(webSpace, DeploymentStatus.INTACT);
    	
    	log("launching web-ui server");
    	String gswebuiWar = ScriptUtils.getBuildPath() + "/tools/gs-webui/gs-webui.war";
		ProcessingUnitDeployment webuiDeployment = new ProcessingUnitDeployment(new File(gswebuiWar)).numberOfInstances(2).numberOfBackups(0)
			.maxInstancesPerVM(1).setContextProperty("jetty.sessions.spaceUrl", "jini://*/*/webSpace").setContextProperty("com.gs.application", "gs-webui");
		gswebui = webUIGSM.deploy(webuiDeployment);
		ProcessingUnitUtils.waitForDeploymentStatus(gswebui, DeploymentStatus.INTACT);
		log("starting gigaspaces apache load balancer client with command : " + apachelb);
		scriptLoadBalancer = ScriptUtils.runScript(apachelb);
		Thread.sleep(5000);
		log(scriptLoadBalancer.getScriptOutput());
		log("apache load balancer now running");
		log("web-ui clients should connect to : " + baseUrlApache);
    }
    
    private void stopLoadBalancerWebServer() throws IOException, InterruptedException {
    	log("undeploying webui");
    	gswebui.undeploy();
    	ProcessingUnitUtils.waitForDeploymentStatus(gswebui, DeploymentStatus.UNDEPLOYED);
    	log("undeploying webSpace");
    	webSpace.undeploy();
    	ProcessingUnitUtils.waitForDeploymentStatus(webSpace, DeploymentStatus.UNDEPLOYED);
    	scriptLoadBalancer.kill();
    	Thread.sleep(2000);
    	File gsconf = new File(System.getenv("apache.home") + "/conf/gigaspaces/gs-webui.conf");
    	gsconf.delete();
    }
    
    private void replaceBalancerTemplate() throws IOException {
		String oldFile = ScriptUtils.getBuildPath() + "/tools/apache/balancer-template.vm";
		String newFile = SGTestHelper.getSGTestRootDir() + "/src/test/webui/resources/balancer-template.vm";
		IOUtils.replaceFile(oldFile, newFile);
    }
	
	public LoginPage getLoginPage() {
		return getLoginPage(this.selenium,this.driver);
	}
	
	public WebDriver getDriver() {
		return driver;
	}
	
	public Selenium getSelenium() {
		return selenium;
	}
	
	private LoginPage getLoginPage(Selenium selenium, WebDriver driver) {
		if (admin.getGroups().length == 0) {
			throw new IllegalStateException("Expected at least one lookupgroup");
		}
		return new LoginPage(selenium, driver, admin.getGroups()[0]);
	}
	
	public boolean verifyAlertThrown() {
		return selenium.isElementPresent(WebConstants.Xpath.okAlert);
	}
	
	/**
	 * use AbstractSeleniumTest static browser fields
	 * @param version
	 * @return
	 */
	public LoginPage openAndSwitchToNewBrowser(int version) {
		WebDriver drv = null;
		switch (version) {
		case FIREFOX : {
			drv = new FirefoxDriver();
			break;
		}
		case CHROME : {
			drv = new ChromeDriver();
		}
		}
		
		drv.get(baseUrl);
		Selenium selenium_temp = new WebDriverBackedSelenium(drv, baseUrl);	
		return getLoginPage(selenium_temp, drv);
		
	}
	
	public DashboardTab refreshPage() throws InterruptedException {
		driver.navigate().refresh();
		Thread.sleep(10000);
		return new DashboardTab(selenium, driver);
	}

	public void takeScreenShot(Class<?> cls, String testMethod, String picName) {

		if (!isDevMode()) {
			File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);

			String buildDir = SGTestHelper.getSGTestRootDir() + "/deploy/local-builds/build_" + System.getProperty("sgtest.buildNumber").split("_")[1] ;

			String testLogsDir = cls.getName() + "." + testMethod + "()";

			String to = buildDir  + "/" + testLogsDir + "/" + picName + ".png";

			try {
				FileUtils.copyFile(scrFile, new File(to));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void setBrowser(String browser) {
		System.setProperty("selenium.browser", browser);
	}
	
	public void restorePreviousBrowser() {
		LogUtils.log("restoring browser setting to " + defaultBrowser);
		setBrowser(defaultBrowser);
	}
	
	/**
	 * retrieves the license key from gigaspaces installation license key
	 * @throws FactoryConfigurationError 
	 * @throws XMLStreamException 
	 * @throws IOException 
	 */
	public String getLicenseKey() throws XMLStreamException, FactoryConfigurationError, IOException {
		
		String licensekey = LICENSE_PATH.replace("lib/required/../../", "");	
		InputStream is = new FileInputStream(new File(licensekey));
		XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(is);
		int element;
		while (true) {
			element = parser.next();
			if (element == XMLStreamReader.START_ELEMENT) {
				if (parser.getName().toString().equals("licensekey")) {
					return parser.getElementText();
				}
			}
			if (element == XMLStreamReader.END_DOCUMENT) {
				break;
			}
		}
		return null;
		
	}
	
	public void repetitiveAssertTrueWithScreenshot(String message, RepetitiveConditionProvider condition, Class<?> cls, String methodName, String picName) {
		
		try {
			AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		}
		catch (AssertionError err) {
			takeScreenShot(cls, methodName, picName);
			LogUtils.log(message, err);
			AssertUtils.AssertFail("Test Failed");
		}
		
	}
	
	public void assertTrueWithScreenshot(boolean condition, Class<?> cls, String methodName, String picName) {
		
		try {
			assertTrue(condition);
		}
		catch (AssertionError err) {
			takeScreenShot(cls, methodName, picName);
			LogUtils.log("Stacktrace: ", err);
			AssertUtils.AssertFail("Test Failed");
		}
		
	}
    
	public boolean isDevMode() {
		return !System.getenv("USERNAME").equals("ca");
	}
	
}
