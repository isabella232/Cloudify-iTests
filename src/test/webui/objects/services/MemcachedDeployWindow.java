package test.webui.objects.services;

import org.openqa.selenium.WebDriver;

import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

public class MemcachedDeployWindow extends AbstractDeployWindow {
	
	String spaceUrl;
	
	/**
	 * constructs an instance with deployment parameters. you should not explicitly use thi
	 * constructor. in order to open a deployment window of some sort use the openDeployWindow 
	 * methods from the topology tab instance. 
	 */
	public MemcachedDeployWindow(Selenium selenium, WebDriver driver, String spaceUrl
			, String isSecured, String userName, String password,
			String numberOfInstances, String numberOfBackups,
			String clusterSchema, String maxInstPerVM, String maxInstPerMachine) {
		super(selenium, driver, isSecured, userName, password, numberOfInstances,
				numberOfBackups, clusterSchema, maxInstPerVM, maxInstPerMachine);
		this.spaceUrl = spaceUrl;
	}
	
	@Override
	public void sumbitDeploySpecs() {
		selenium.type(WebConstants.ID.spaceUrlInput, spaceUrl);
		super.sumbitDeploySpecs();
	}
	
	
	

}
