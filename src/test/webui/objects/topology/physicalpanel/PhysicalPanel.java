package test.webui.objects.topology.physicalpanel;

import org.openqa.selenium.WebDriver;

import test.webui.objects.topology.TopologySubPanel;

import com.thoughtworks.selenium.Selenium;

/**
 * This class is a mapping of the Physical Tabular tab in the topology tab
 * It offers methods for all data retrieval possible in this panel
 * @author elip
 *
 */
public class PhysicalPanel extends TopologySubPanel {
	
	public PhysicalPanel(Selenium selenium, WebDriver driver) {
		this.driver = driver;
		this.selenium = selenium;
	}
	
	/**
	 * @param name - host name to be retrieved
	 * @return a table row of the physical tab containing all possible information about a specific host
	 */
	public HostData getHostData(String name) {
		HostData host = new HostData(name, driver);
		if (host.getName() != null) return host;
		return null;
	}
	
	public enum OS {
		WINDOWS32,LINUX;
	}
	
	
	
}
