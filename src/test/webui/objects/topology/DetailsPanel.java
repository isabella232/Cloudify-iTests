package test.webui.objects.topology;

import org.openqa.selenium.WebDriver;

import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

public class DetailsPanel {
	
	WebDriver driver;
	Selenium selenium;
	
	public DetailsPanel(WebDriver driver, Selenium selenium) {
		this.driver = driver;
		this.selenium = selenium;
	}
	
	public void toggleShowHide() {
		// TODO implement
	}
	
	public ComparisonCharts switchToComparisonCharts() {
		selenium.click(WebConstants.Xpath.pathToComparisonCharts);
		return new ComparisonCharts(selenium, driver);
	}
	
	
	public WebUIServiceDetails switchToServiceDetails() {
		selenium.click(WebConstants.Xpath.pathToServiceDetails);
		return new WebUIServiceDetails(selenium, driver);
	}
	
}
