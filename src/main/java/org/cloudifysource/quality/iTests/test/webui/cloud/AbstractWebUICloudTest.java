package org.cloudifysource.quality.iTests.test.webui.cloud;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverBackedSelenium;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.WebConstants;
import com.thoughtworks.selenium.Selenium;

public abstract class AbstractWebUICloudTest extends NewAbstractCloudTest {
	
	private WebDriver driver;
	private Selenium selenium;
	private ChromeDriverService chromeService;
	
	@BeforeMethod
	public void launchWebui() throws InterruptedException {
		startWebBrowser(getWebuiUrl());		
	}
	
	@AfterMethod(alwaysRun = true)
	public void killWebServices() throws InterruptedException {
		stopWebBrowser();
	} 
	
    private void startWebBrowser(String uRL) throws InterruptedException {
    	LogUtils.log("Launching browser...");
    	String browser = System.getProperty("selenium.browser");
    	LogUtils.log("Current browser is " + browser);
    	for (int i = 0 ; i < 3 ; i++) {
    		try {
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
    						DesiredCapabilities desired = DesiredCapabilities.chrome();
							desired.setCapability("chrome.switches", Arrays.asList("--start-maximized"));
							String chromeDriverExePath = SGTestHelper.getSGTestRootDir() + "/src/main/resources/webui/chromedriver.exe";
							chromeService = new ChromeDriverService.Builder().usingAnyFreePort().usingDriverExecutable(new File(chromeDriverExePath)).build();
							LogUtils.log("Starting Chrome Driver Server...");
							chromeService.start();
							driver = new RemoteWebDriver(chromeService.getUrl(), desired);
    					}
    				}
    			}
    			break;

    		}
    		catch (WebDriverException e) {
    			LogUtils.log("Failed to lanch browser, retyring...Attempt number " + (i + 1));
    		} catch (IOException e) {
				Assert.fail("Failed to lanch browser", e);
			}
    	}
    	if (driver == null) {
    		LogUtils.log("unable to lauch browser, test will fail on NPE");
    	}
    	int seconds = 0;
    	if (driver != null) {
        	driver.get(uRL);
        	selenium = new WebDriverBackedSelenium(driver, uRL);
        	Thread.sleep(3000);
        	while (seconds < 30) {
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
        	if (seconds == 30) {
        		LogUtils.log("Could not establish a connection to webui server, Test will fail");
        	}
    	}
    }


	public void shutdownWebui() throws InterruptedException {
		driver.quit();
	}
	
	public void stopWebBrowser() throws InterruptedException {
    	LogUtils.log("Killing browser...");
    	
    	if(selenium != null)
    		selenium.stop();
    	
		selenium = null;
		Thread.sleep(1000);
		
		if (chromeService != null && chromeService.isRunning()) {
			LogUtils.log("Chrome Driver Server is still running, shutting it down...");
			chromeService.stop();
			chromeService = null;
		}
    }
	
	public LoginPage getLoginPage() {
		return new LoginPage(selenium,driver);
	}
	
	public void setBrowser(String browser) {
		System.setProperty("selenium.browser", browser);
	}

}
