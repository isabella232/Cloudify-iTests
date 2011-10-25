package test.webui.objects;

import org.openqa.selenium.WebDriver;

import test.webui.objects.services.AbstractDeployWindow;
import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

public class ProcessingUnitDeployWindow extends AbstractDeployWindow {
	
	String puName;

	/**
	 * constructs an instance with deployment parameters. you should not explicitly use thi
	 * constructor. in order to open a deployment window of some sort use the openDeployWindow 
	 * methods from the topology tab instance. 
	 */
	public ProcessingUnitDeployWindow(Selenium selenium, WebDriver driver, String puName,
			String isSecured, String userName, String password,
			String numberOfInstances, String numberOfBackups,
			String clusterSchema, String maxInstPerVM, String maxInstPerMachine) {
		super(selenium, driver, isSecured, userName, password, numberOfInstances,
				numberOfBackups, clusterSchema, maxInstPerVM, maxInstPerMachine);
		this.puName = puName;
	}
	
	@Override
	public void sumbitDeploySpecs() {
		selenium.click(WebConstants.Xpath.deoployFromListRadioButton);
		selenium.click(WebConstants.Xpath.existingPuCombo);
		selenium.mouseDown(WebConstants.Xpath.getPathToComboSelection(puName));
		super.sumbitDeploySpecs();
	}
	
	

}
