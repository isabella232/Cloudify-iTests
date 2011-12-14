package test.webui.objects.topology.logspanel;

import org.openqa.selenium.WebDriver;

import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

public class PuLogsPanelService {
	
	private WebDriver driver;
	private Selenium selenium;
	private String puName;
	
	public PuLogsPanelService(WebDriver driver, Selenium selenium, String puName) {
		this.driver = driver;
		this.selenium = selenium;
		this.puName = puName;
	}

	public LogsMachine getMachine(String machineName) {
		if (selenium.isElementPresent(WebConstants.Xpath.getPathToLogsMachine(machineName, puName))) {
			return new LogsMachine(machineName, puName, selenium, driver);
		}
		else return null;
		
	}
	
	public void collapseOrExpandPu() {
		selenium.click(WebConstants.Xpath.getPathToPuLogsExapndButton(puName));
		
	}
}
