package test.webui.objects;

import net.jini.core.discovery.LookupLocator;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import test.webui.resources.WebConstants;

import com.gigaspaces.internal.utils.StringUtils;
import com.thoughtworks.selenium.Selenium;

import framework.utils.LogUtils;

/**
 * represents the login page of the web-ui
 * @author elip
 *
 */
public class LoginPage {
	
	Selenium selenium;
	WebDriver driver;
	
	private String username;
	private String password;
	private String jiniGroup;
	private LookupLocator[] locators;
	
	WebElement logginButton;
	
	/**
	 * constructs an instance with no login parameters
	 * @param selenium
	 * @param driver
	 */
	public LoginPage(Selenium selenium, WebDriver driver) {
		this.selenium = selenium;
		this.driver = driver;
	}
	
	/**
	 * constructs an instance with a specific lookup group
	 */
	public LoginPage(Selenium selenium, WebDriver driver, String lookupGroups) {
		this.driver = driver;
		this.selenium = selenium;
		this.jiniGroup = lookupGroups;
	}

	public LoginPage(Selenium selenium, WebDriver driver,
			LookupLocator[] locators) {
		this.driver = driver;
		this.selenium = selenium;
		this.locators = locators;
	}
	
	/**
	 * constructs an instance for a certain user
	 * @param selenium
	 * @param driver
	 * @param username
	 * @param password
	 */
	public LoginPage(Selenium selenium, WebDriver driver, 
			String username , String password) {
		this.username = username;
		this.password = password;
	}

	/**
	 * writes the user parameters in the text edits of the ui
	 */
	public void inputUsernameAndPassword() {
		WebElement annon = driver.findElement(By.xpath(WebConstants.Xpath.annonymusCheckbox));
		annon.click();
		WebElement usernameEl = driver.findElement(By.id(WebConstants.ID.usernameLogginInput));
		usernameEl.sendKeys(username);
		WebElement passwordEl = driver.findElement(By.id(WebConstants.ID.passwordLogginInput));
		passwordEl.sendKeys(password);
	}
	
	/**
	 * write discovery parameters in the text edits of the ui
	 */
	public void inputDiscovery() {
		if (jiniGroup != null) {
			WebElement group = driver.findElement(By.id(WebConstants.ID.jiniGroupInput));
			group.sendKeys(jiniGroup);
		}
		if (locators != null) {
			String[] locatorsString = new String[locators.length];
			for (int i = 0 ; i < locators.length ; i++) {
				locatorsString[i]=locators[i].getHost()+":"+locators[i].getPort();
			}
			WebElement locator = driver.findElement(By.id(WebConstants.ID.locatorsInput));
			
			locator.sendKeys(StringUtils.arrayToCommaDelimitedString(locatorsString));
		}
	}
	
	/**
	 * logs in to the system
	 * @return
	 * @throws InterruptedException 
	 */
	public MainNavigation login() throws InterruptedException {
		selenium.click(WebConstants.Xpath.loginButton);
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("return this.GigaSpaces.Util.Flags.isUnderTest=true");
		return new MainNavigation(selenium, driver);
	}
	
	public void assertLoginPerformed() throws InterruptedException {
		int attempts = 0;
		while (attempts < 10) {
			int seconds = 0;
			while (seconds <= 5) {
				try {
					if (seconds != 5) {
						driver.findElement(By.xpath(WebConstants.Xpath.dashBoardButton));
						Thread.sleep(1000);
						seconds++;
					}
					else {
						return;
					}
				}
				catch (NoSuchElementException e) {
					LogUtils.log("Unable to login, retrying...Attempt number " + (seconds + 1));
					if (selenium.isElementPresent(WebConstants.Xpath.loginButton)) {
						login();
						break;
					}
					else {
						Assert.fail("Unable to login to page because login page wasnt found");
					}


				}
			}
			attempts++;
		}
    	if (attempts == 10) {
    		Assert.fail("Test Failed because it was login because login is not stable");
    	}
	}
}
