package test.webui.objects.console;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriver;

import com.thoughtworks.selenium.Selenium;

public class SpaceConfigurationGrid {

	private Selenium selenium;
	private WebDriver driver;
	
	public SpaceConfigurationGrid(Selenium selenium, WebDriver driver) {
		this.selenium = selenium;
		this.driver = driver;
	}
	
	/**
	 * 
	 * @return a key-value map containing specific space configurations
	 */
	public Map<String, String> getSpaceConfiguration() {
		
		Map<String, String> result = new HashMap<String, String>();
		
		return result;
	}

}
