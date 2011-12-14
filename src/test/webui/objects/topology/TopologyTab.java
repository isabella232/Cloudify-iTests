package test.webui.objects.topology;

import org.openqa.selenium.WebDriver;

import test.webui.objects.MainNavigation;
import test.webui.objects.topology.applicationmap.ApplicationMap;
import test.webui.objects.topology.sidepanel.TopologySidePanel;

import com.thoughtworks.selenium.Selenium;

public class TopologyTab extends MainNavigation {

	public TopologyTab(Selenium selenium, WebDriver driver) {
		this.selenium = selenium;
		this.driver = driver;
	}

	public ApplicationMap getApplicationMap() {
		return new ApplicationMap(driver);
	}
	
	public TopologySubPanel getTopologySubPanel() {
		return new TopologySubPanel(selenium, driver);
	}
	
	public TopologySidePanel getDetailsPanel() {
		return new TopologySidePanel(driver, selenium);
	}
}
