package test.webui.objects.topology;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;

import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

public class PuLogsPanelService {
	
	private WebDriver driver;
	private Selenium selenium;
	private String puName;
	
	public PuLogsPanelService(WebDriver driver, Selenium selenium, String puName) {
		this.driver = driver;
		this.selenium = selenium;
		this.puName = puName;
	}

	public LogsMachine getMachine(String machineName) {
		if (selenium.isElementPresent(WebConstants.Xpath.getPathToLogsMachine(machineName, puName))) {
			return new LogsMachine(machineName, puName);
		}
		else return null;
		
	}
	
	public void collapseOrExpandPu() {
		selenium.click(WebConstants.Xpath.getPathToPuLogsExapndButton(puName));
		
	}
	
	public class LogsMachine {
		
		private String machineName;
		private String xpath;
		private String puName;
		
		public LogsMachine(String machineName, String puName) {
			this.machineName = machineName;
			this.puName = puName;
			this.xpath = WebConstants.Xpath.getPathToLogsMachine(machineName, puName);
		}
		
		public LogsGridServiceContainer getContianer(GridServiceContainer contianer) {
			String contianerId = contianer.getVirtualMachine().getStatistics().getDetails().getPid() + "";
			String agentId = contianer.getAgentId() + "";
			if (selenium.isElementPresent(WebConstants.Xpath.getPathToLogsContianer(contianerId, agentId, puName))) {
				return new LogsGridServiceContainer(contianerId, agentId, puName);
			}
			else return null;
			
		}
		
		public List<String> getServices() {
			
			Exception exception = null;
			List<String> services = new ArrayList<String>();
			int i = 1;
			while (exception == null) {
				try {
					WebElement serviceElement = driver.findElement(By.
							xpath(xpath + WebConstants.Xpath.getPathToLogsMachineServiceByIndex(i)));
					String service = serviceElement.getAttribute("id").substring(32);
					services.add(service);
					i++;
				}
				catch (NoSuchElementException e) {
					exception = e;
				}
				catch (WebDriverException e) {
					exception = e;
				}
			}
			return services;
			
		}
		
		public void selectService(String serviceName) {
			selenium.click(WebConstants.ID.getLogServiceId(serviceName));
			
		}
		
		public boolean containsGridServiceContainer(GridServiceContainer gsc) {
			
			List<String> services = getServices();
			String pid = "" + gsc.getVirtualMachine().getDetails().getPid() + "";
			for (String s : services) {	
				if (s.contains("gsc") && s.contains(pid)) return true;
			}
			return false;
			
		}
		
		public boolean containsGridServiceManager(GridServiceManager gsm) {
			
			List<String> services = getServices();
			String pid = "" + gsm.getVirtualMachine().getDetails().getPid() + "";
			for (String s : services) {	
				if (s.contains("gsm") && s.contains(pid)) return true;
			}
			return false;
			
		}
		
		public void collapseOrExpandMachine() {
			selenium.click(xpath + WebConstants.Xpath.pathToLogsMachineExpandButton);
			
		}
		
		public class LogsGridServiceContainer {
		
			private String contianerId;
			private String xpath;
			private String puName;
			private String agentId;
			
			public LogsGridServiceContainer(String contianerId,String agentId ,String puName) {
				this.contianerId = contianerId;
				this.agentId = agentId;
				this.xpath = WebConstants.Xpath.getPathToLogsContianer(contianerId,agentId, puName);
				this.puName = puName;
			}
			
			public List<String> getPuInstances() {
				
				Exception exception = null;
				List<String> services = new ArrayList<String>();
				int i = 1;
				while (exception == null) {
					try {
						WebElement serviceElement = driver.findElement(By.
								xpath(xpath + WebConstants.Xpath.getPathToLogsContianerPuInstanceByIndex(i)));
						String service = serviceElement.getText();
						services.add(service);
						i++;
					}
					catch (NoSuchElementException e) {
						exception = e;
					}
					catch (WebDriverException e) {
						exception = e;
					}
				}
				return services;
				
			}
		}
	}

}
