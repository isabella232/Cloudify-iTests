package test.webui.objects.topology;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

public class LogsPanel extends TopologySubPanel {
	
	public LogsPanel(WebDriver driver, Selenium selenium) {
		this.driver = driver;
		this.selenium = selenium;
	}
	
	public void toggleSampling() {
		selenium.click(WebConstants.Xpath.pathToLogsSamplingButton);
	}
	
	public String getCurrentLog() {
		WebElement logsPanel = driver.findElement(By.id(WebConstants.ID.logsPanel));
		WebElement selectedItem  = logsPanel.findElement(By.className(WebConstants.ClassNames.selectedItemInLogsTree));
		return selectedItem.getText();
	}
	
	public PuLogsPanelService getPuLogsPanelService(String puName) {
		if (selenium.isElementPresent(WebConstants.ID.getPuNodeId(puName))) {
			return new PuLogsPanelService(driver, selenium, puName);
		}
		else return null;
	}
	
	public String getApplicationName() {
		WebElement element = driver.findElement(By.xpath(WebConstants.Xpath.pathToApplicationNameInLogsPanel));
		return element.getText();
	}

}
