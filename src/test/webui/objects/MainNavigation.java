package test.webui.objects;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;

import test.webui.objects.console.ConsoleTab;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.services.ServicesTab;
import test.webui.objects.topology.TopologyTab;
import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

public class MainNavigation {
	
	protected Selenium selenium;
	protected WebDriver driver;
	
	public MainNavigation() {}
	
	public MainNavigation(Selenium selenium, WebDriver driver) {
		this.selenium = selenium;
		this.driver = driver;
	}
	
	public ServicesTab switchToServices() {
		selenium.click(WebConstants.Xpath.servicesButton);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		catch (NoSuchElementException e) {
			return null;
		}
		return new ServicesTab(selenium, driver);
	}
	
	public DashboardTab switchToDashboard() {
		selenium.click(WebConstants.Xpath.dashBoardButton);
		try {
			Thread.sleep(2000);
			} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		catch (NoSuchElementException e) {
			return null;
		}
		return new DashboardTab(selenium, driver);
	}
	
	public TopologyTab switchToTopology() {
		selenium.click(WebConstants.Xpath.topologyButton);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		catch (NoSuchElementException e) {
			return null;
		}
		return new TopologyTab(selenium, driver);
	}
	
	public ConsoleTab switchToConsole() {
		selenium.click(WebConstants.Xpath.consoleButton);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		catch (NoSuchElementException e) {
			return null;
		}
		return new ConsoleTab(selenium, driver);
	}
}
