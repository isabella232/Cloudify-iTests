package test.webui.objects.topology;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.thoughtworks.selenium.Selenium;

import test.webui.resources.WebConstants;
import framework.utils.AssertUtils;

public class ServiceMetrics {
	
	private Selenium selenium;
	private WebDriver driver;
	
	public ServiceMetrics(Selenium selenium , WebDriver driver) {
		this.driver = driver;
		this.selenium = selenium;
	}
	
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
