package test.webui.objects.services;

import org.openqa.selenium.WebDriver;

import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

/**
 * represents an EDG deployment window that can be accessed through the topology tab
 * @author elip
 *
 */
public class DataGridDeployWindow extends AbstractDeployWindow {

	String dataGridName;
	
	/**
	 * constructs an instance with deployment parameters. you should not explicitly use thi
	 * constructor. in order to open a deployment window of some sort use the openDeployWindow 
	 * methods from the topology tab instance.
	 */
	public DataGridDeployWindow(Selenium selenium, WebDriver driver, String dataGridName, String isSecured, String userName, 
			String password, String numberOfInstances,
			String numberOfBackups, String clusterSchema, String maxInstPerVM,
			String maxInstPerMachine) {
		super(selenium, driver, isSecured, userName, password, numberOfInstances,
				numberOfBackups, clusterSchema, maxInstPerVM, maxInstPerMachine);
		this.dataGridName = dataGridName;
	}
	
	/**
	 * adds the data grid name to deployment
	 */
	@Override
	public void sumbitDeploySpecs() {
		selenium.type(WebConstants.ID.dataGridNameInput, dataGridName);
		super.sumbitDeploySpecs();
	}



}
