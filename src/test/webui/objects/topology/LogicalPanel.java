package test.webui.objects.topology;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import test.webui.objects.dashboard.ServicesGrid.Icon;
import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

import framework.utils.LogUtils;
import framework.utils.WebUiUtils;

public class LogicalPanel extends TopologySubPanel {
	
	public LogicalPanel(Selenium selenium, WebDriver driver) {
		this.driver = driver;
		this.selenium = selenium;
	}
	
	public WebUIProcessingUnitInstance getProcessingUnitInstance(String instanceName) {
		WebUIProcessingUnitInstance instance = new WebUIProcessingUnitInstance(instanceName);
		if (instance.getName() != null) {
			return instance;
		}
		return null;
	}
	
	
	
	public class WebUIProcessingUnitInstance {
		
		private String name;
		
		public WebUIProcessingUnitInstance(String name) {
			
			try {
				String id = WebConstants.ID.getPuInstanceId(name);
				@SuppressWarnings("unused")
				WebElement hostElement = driver.findElement(By.id(id));
				this.name = name;
			}
			catch (NoSuchElementException e) {
			}
			catch (WebDriverException e) {
			}
		}
		
		public boolean select() {
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					String id = WebConstants.ID.getPuInstanceId(name);
					WebElement hostElement = driver.findElement(By.id(id));
					hostElement.click();
					return true;
				}
				catch (StaleElementReferenceException e) {
					LogUtils.log("Failed to discover element due to statistics update, retyring...");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					seconds++;
				}
			}
			return false;
		}
		
		public String getName() {
			return name;
		}
		
		public Icon getIcon() {
			
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					String id = WebConstants.ID.getPuInstanceId(name);
					WebElement hostElement = driver.findElement(By.id(id));
					WebElement icon = hostElement.findElement(By.className("x-grid3-td-status")).findElement(By.tagName("span"));
					String type = icon.getAttribute("class").trim();
					if (type.equals(WebConstants.ID.okIcon)) return Icon.OK;
					if (type.equals(WebConstants.ID.criticalIcon)) return Icon.CRITICAL;
					if (type.equals(WebConstants.ID.warningIcon)) return Icon.ALERT;
					if (type.equals(WebConstants.ID.naIcon)) return Icon.NA;
					return null;
				}
				catch (StaleElementReferenceException e) {
					LogUtils.log("Failed to discover element due to statistics update, retyring...");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					seconds++;
				}
			}
			return null;
			
		}
		
		public String getHostName() {
			
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					String id = WebConstants.ID.getPuInstanceId(name);
					WebElement hostElement = driver.findElement(By.id(id));
					WebElement cores = hostElement.findElement(By.className("x-grid3-td-hostName"));
					return cores.getText();
				}
				catch (StaleElementReferenceException e) {
					LogUtils.log("Failed to discover element due to statistics update, retyring...");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					seconds++;
				}
			}
			return null;
			
		}
		
		public String showComparisonCharts() {
			
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					String id = WebConstants.ID.getPuInstanceId(name);
					WebElement instanceElement = driver.findElement(By.id(id));
					WebElement chartSelection = instanceElement.findElement(By.className("x-grid3-td-chart_selection"));
					chartSelection.click();
					String color = instanceElement.findElement(By.className("x-grid3-td-selected_color")).findElement(By.tagName("circle")).getAttribute("fill");
					return color;
				}
				catch (StaleElementReferenceException e) {
					LogUtils.log("Failed to discover element due to statistics update, retyring...");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					seconds++;
				}
			}
			return null;
			
		}
		
		public void goToLogs() {
			
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					String id = WebConstants.ID.getPuInstanceId(name);
					WebElement hostElement = driver.findElement(By.id(id));
					WebElement actions = hostElement.findElement(By.className("x-grid3-td-tools")).findElement(By.tagName("button"));
					actions.click();
					WebElement goToLogsItem = driver.findElement(By.id(WebConstants.ID.goToLogsItemID));
					goToLogsItem.click();
					break;
				}
				catch (StaleElementReferenceException e) {
					LogUtils.log("Failed to discover element due to statistics update, retyring...");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					seconds++;
				}
			}
			
		}
		
		public void restart() {
			
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					String id = WebConstants.ID.getPuInstanceId(name);
					WebElement hostElement = driver.findElement(By.id(id));
					WebElement actions = hostElement.findElement(By.className("x-grid3-td-tools")).findElement(By.tagName("button"));
					actions.click();
					WebElement restartItem = driver.findElement(By.id(WebConstants.ID.restartPuInstanceItem));
					restartItem.click();
					selenium.click(WebConstants.Xpath.acceptAlert);
					break;
				}
				catch (StaleElementReferenceException e) {
					LogUtils.log("Failed to discover element due to statistics update, retyring...");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					seconds++;
				}
			}
			
		}
	}

}
