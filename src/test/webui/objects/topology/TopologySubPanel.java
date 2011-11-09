package test.webui.objects.topology;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

public class TopologySubPanel {
	
	protected Selenium selenium;
	protected WebDriver driver;
	
	public TopologySubPanel() {}
	
	public TopologySubPanel(Selenium selenium, WebDriver driver) {
		this.selenium = selenium;
		this.driver = driver;
	}
	
	public String getAssociatedPuName() {
		return selenium.getText(WebConstants.Xpath.pathToTopologySubPanelName);
	}
	
	public LogsPanel switchToLogsPanel() {
		WebElement healthButton = driver.findElement(By.id(WebConstants.ID.logsPanelToggle));
		healthButton.click();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return new LogsPanel(driver, selenium);
	}
	
	public HealthPanel switchToHealthPanel() {
		WebElement healthButton = driver.findElement(By.id(WebConstants.ID.healthPanelToggle));
		healthButton.click();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return new HealthPanel(selenium, driver);
	}
	
	public PhysicalPanel switchToPhysicalPanel() {
		WebElement physicalButton = driver.findElement(By.id(WebConstants.ID.physicalPanelToggle));
		physicalButton.click();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return new PhysicalPanel(selenium, driver);
	}
	
	public LogicalPanel swtichToLogicalPanel() {
		WebElement physicalButton = driver.findElement(By.id(WebConstants.ID.logicalPanelToggle));
		physicalButton.click();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return new LogicalPanel(selenium, driver);
	}
}
