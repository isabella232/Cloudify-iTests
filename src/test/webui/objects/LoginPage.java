package test.webui.objects;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import test.utils.LogUtils;
import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

/**
 * represents the login page of the web-ui
 * @author elip
 *
 */
public class LoginPage {
	
	Selenium selenium;
	WebDriver driver;
	
	String username, password, jiniGroup, locators;
	
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
		selenium.click(WebConstants.Xpath.annonymusCheckbox);
		selenium.type(WebConstants.ID.usernameLogginInput, username);
		selenium.type(WebConstants.ID.passwordLogginInput, password);
	}
	
	/**
	 * write discovery parameters in the text edits of the ui
	 */
	public void inputDiscovery() {
		selenium.click(WebConstants.Xpath.discoveryLegend);
		selenium.type(WebConstants.ID.jiniGroupInput, jiniGroup);
		selenium.type(WebConstants.ID.locatorsInput, locators);
	}
	
	/**
	 * logs in to the system
	 * @return
	 * @throws InterruptedException 
	 */
	public MainNavigation login() throws InterruptedException {
		selenium.windowMaximize();
		Thread.sleep(1000);
		inputDiscovery();
		selenium.click(WebConstants.Xpath.loginButton);
		Thread.sleep(1000);
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
