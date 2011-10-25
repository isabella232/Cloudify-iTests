package test.webui.objects.console;

import org.openqa.selenium.WebDriver;

import com.thoughtworks.selenium.Selenium;

import test.webui.objects.MainNavigation;

public class ConsoleTab extends MainNavigation {

	private SpaceConfigurationGrid spaceConfig;
	private SpaceInstancesGrid spaceInstnaces;
	private SpaceTransactionsGrid spaceTransactions;
	private SpaceTypesGrid spaceTypes;
	private SpaceTreeSidePanel spaceTreePanel;
	
	public ConsoleTab(Selenium selenium, WebDriver driver) {
		this.driver = driver;
		this.selenium = selenium;
		this.spaceConfig = new SpaceConfigurationGrid(selenium, driver);
		this.spaceInstnaces = new SpaceInstancesGrid(selenium, driver);
		this.spaceTransactions = new SpaceTransactionsGrid(selenium, driver);
		this.spaceTypes = new SpaceTypesGrid(selenium , driver);
		this.spaceTreePanel = new SpaceTreeSidePanel(selenium, driver);
	}
	
	public SpaceConfigurationGrid getSpaceConfigurationPanel() {
		return this.spaceConfig;
	}
	
	public SpaceInstancesGrid getInstancesGrid() {
		return this.spaceInstnaces;
	}
	
	public SpaceTransactionsGrid getSpaceTransactionsPanel() {
		return this.spaceTransactions;
	}
	
	public SpaceTypesGrid getSpaceTypesPanel() {
		return this.spaceTypes;
	}
	
	public SpaceTreeSidePanel getSpaceTreeSidePanel() {
		return this.spaceTreePanel;
	}
}
