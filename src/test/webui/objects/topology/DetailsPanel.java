package test.webui.objects.topology;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import test.utils.AssertUtils;
import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;
import com.thoughtworks.selenium.SeleniumException;

public class DetailsPanel {
	
	WebDriver driver;
	Selenium selenium;
	
	
	public DetailsPanel(WebDriver driver, Selenium selenium) {
		this.driver = driver;
		this.selenium = selenium;
	}
	
	public static DetailsPanel getInstance(WebDriver driver, Selenium selenium) {
		return new DetailsPanel(driver, selenium);
	}
	
	public void toggleShowHide() {
		// TODO implement
	}
	
	public void showServiceDetails() {
		selenium.click(WebConstants.Xpath.pathToServiceDetails);
	}
	
	public void showMetricSelection() {
		selenium.click(WebConstants.Xpath.pathToMetricSelection);
	}
	
	public void showComparisonCharts() {
		selenium.click(WebConstants.Xpath.pathToComparisonCharts);
	}
	
	
	public WebUIServiceDetails getServiceDetails() {
		return new WebUIServiceDetails();
	}

	public ServiceMetrics getServiceMetrics() {
		return new ServiceMetrics();
	}
	
	public ComparisonCharts getComparisonCharts() {
		return new ComparisonCharts();
	}

	public class WebUIServiceDetails {
		
		public final static String EMBEDDED_SPACE = "Embedded Space";
		
		public Map<String, Map<String,String>> getDetails() {
			
			SeleniumException exceptionOuter = null;
			int i = 1;
			HashMap<String, Map<String, String>> details = new HashMap<String, Map<String,String>>();
			while (exceptionOuter == null) {
				try {
					SeleniumException exceptionInner = null;
					String pathToGroup = null;
					pathToGroup = WebConstants.Xpath.getPathToDetailsGroup(i);
					String groupName = selenium.getText(pathToGroup + WebConstants.Xpath.pathToGroupName);
					int j = 1;
					Map<String, String> subDetails = new HashMap<String, String>();
					while (exceptionInner == null) {
						try {
							String pathToAttribute = pathToGroup + WebConstants.Xpath.getPathToAttributeInGroup(j);
							subDetails.put(selenium.getText(pathToAttribute + WebConstants.Xpath.pathToAttributeName),
									selenium.getText(pathToAttribute + WebConstants.Xpath.pathToAttributeValue));
							j++;
						}
						catch (SeleniumException e) {
							exceptionInner = e;
						}
					}
					details.put(groupName, subDetails);
					i++;
				}
				catch (SeleniumException e) {
					exceptionOuter = e;
				}
			}
			return details;
			
		}
		
	}
	
	public class ServiceMetrics {
		
		private MetricType bottom;;
		private MetricType top;
	
		public void selectTopMetric(MetricType metricType) {
				
			String type = metricType.getType();
			String name = metricType.getName();	
			WebElement selection = driver.findElement(By.xpath(WebConstants.Xpath.pathToTopMetricSelection)).findElement(By.className("gs-drop-down-button-metric-type"));
			selection.click();
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript(WebConstants.JQuery.getMouseOverMetricTypeScript(type));
			selenium.click(WebConstants.Xpath.getPathToMenuSelection(name));
			this.top = metricType;
			
		}
		
		public void selectBottomMetric(MetricType metricType) {
				
			String type = metricType.getType();
			String name = metricType.getName();	
			WebElement selection = driver.findElement(By.xpath(WebConstants.Xpath.pathToBottomMetricSelection)).findElement(By.className("gs-drop-down-button-metric-type"));
			selection.click();
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript(WebConstants.JQuery.getMouseOverMetricTypeScript(type));
			selenium.click(WebConstants.Xpath.getPathToMenuSelection(name));
			this.bottom = metricType;
		
		}
		
		public void assertBottomMetricSelected() {
			
			String type = selenium.getText(WebConstants.Xpath.pathToBottomMetric);
			AssertUtils.assertTrue(type.contains(bottom.getType() + ":" + bottom.getName()));
			
			
		}
		
		public void assertTopMetricSelected() {
			
			String type = selenium.getText(WebConstants.Xpath.pathToTopMetric); 
			AssertUtils.assertTrue(type.contains(top.getType() + ":" + top.getName()));
		}
		
	}
	
	public class ComparisonCharts {
		
		
		
	}

}
