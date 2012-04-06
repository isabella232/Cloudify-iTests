package test.webui.objects.dashboard;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import test.webui.objects.dashboard.alerts.AlertsPanel;
import test.webui.objects.dashboard.events.EventsPanel;
import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

public class DashboardSubPanel {
	
	protected Selenium selenium;
	protected WebDriver driver;
	
	public DashboardSubPanel(Selenium selenium, WebDriver driver) {
		this.driver = driver;
		this.selenium = selenium;
	}

	public EventsPanel switchToEventsPanel() {
		WebElement healthButton = driver.findElement(By.id(WebConstants.ID.dashboardeventsPanelToggle));
		healthButton.click();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return new EventsPanel(driver);
	}


	public AlertsPanel switchToAlertsPanel() {
		WebElement healthButton = driver.findElement(By.id(WebConstants.ID.dashboardAlertsPanelToggle));
		healthButton.click();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return new AlertsPanel(selenium, driver);
	}

}
