package test.webui.objects.topology;

import org.openqa.selenium.WebDriver;

import test.webui.objects.MainNavigation;

import com.thoughtworks.selenium.Selenium;

public class TopologyTab extends MainNavigation {

	public TopologyTab(Selenium selenium, WebDriver driver) {
		this.selenium = selenium;
		this.driver = driver;
	}

	public ApplicationMap getApplicationMap() {
		return ApplicationMap.getInstance(selenium, driver);
	}
	
	public TopologySubPanel getTopologySubPanel() {
		return new TopologySubPanel(selenium, driver);
	}
	
	public DetailsPanel getDetailsPanel() {
		return DetailsPanel.getInstance(driver, selenium);
	}
}
