package test.webui.objects.services;

import org.openqa.selenium.WebDriver;

import test.webui.interfaces.IDeployWindow;
import test.webui.objects.MainNavigation;
import test.webui.objects.ProcessingUnitDeployWindow;
import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

/**
 * represents the Services tab of thw web-ui
 * @author elip
 *
 */
public class ServicesTab extends MainNavigation {

	/**
	 * constructs an empty instance
	 * @param selenium
	 * @param driver
	 */
	public ServicesTab(Selenium selenium, WebDriver driver) {
		this.selenium = selenium;
		this.driver = driver;
	}

	/**
	 * opens the EDG deployment menu with certain deployment parameters
	 * @param selenium
	 * @param driver
	 * @param dataGridName
	 * @param isSecured
	 * @param userName
	 * @param password
	 * @param numberOfInstances
	 * @param numberOfBackups
	 * @param clusterSchema
	 * @param maxInstPerVM
	 * @param maxInstPerMachine
	 * @return a DeployWindow object representing a specific deployment window
	 */
	public IDeployWindow openEDGDeployWindow(String dataGridName, String isSecured,
			String userName, String password, String numberOfInstances,
			String numberOfBackups, String clusterSchema, String maxInstPerVM,
			String maxInstPerMachine) {
		selenium.click(WebConstants.Xpath.deployMenuButton);
		selenium.click(WebConstants.ID.deployEDGOption);
		return new DataGridDeployWindow(selenium, driver, dataGridName, isSecured, 
				userName, password, numberOfInstances, numberOfBackups, 
				clusterSchema, maxInstPerVM, maxInstPerMachine);
	}
	
	public IDeployWindow openMemcachedDeployWindow(String spaceUrl, String isSecured,
			String userName, String password, String numberOfInstances,
			String numberOfBackups, String clusterSchema, String maxInstPerVM,
			String maxInstPerMachine) {
		selenium.click(WebConstants.Xpath.deployMenuButton);
		selenium.click(WebConstants.ID.deployMemcachedOption);
		return new MemcachedDeployWindow(selenium, driver, spaceUrl, isSecured, 
				userName, password, numberOfInstances, numberOfBackups, 
				clusterSchema, maxInstPerVM, maxInstPerMachine);
	}
	
	public IDeployWindow openProcessingUnitDeployWindow(String puName, String isSecured,
			String userName, String password, String numberOfInstances,
			String numberOfBackups, String clusterSchema, String maxInstPerVM,
			String maxInstPerMachine) {
		selenium.click(WebConstants.Xpath.deployMenuButton);
		selenium.click(WebConstants.ID.deployProcessingUnitOption);
		return new ProcessingUnitDeployWindow(selenium, driver, puName, isSecured, 
				userName, password, numberOfInstances, numberOfBackups, 
				clusterSchema, maxInstPerVM, maxInstPerMachine);
	}
	
	/**
	 * retrieve the processing unit tree grid from the Topology tab
	 * @return the PuTreeGrid singelton
	 */
	public PuTreeGrid getPuTreeGrid() {
		return PuTreeGrid.getInstance(selenium, driver);
	}
	
	/**
	 * retrieve the Host and Services grid from the Topology tab
	 * @return the Host and Services singelton
	 */
	public HostsAndServicesGrid getHostAndServicesGrid() {
		return HostsAndServicesGrid.getInstance(selenium, driver);
	}
	
	

}
