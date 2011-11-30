package test.webui.objects.topology;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

public class ComparisonCharts {
	
	private Selenium selenium;
	private WebDriver driver;
	
	public ComparisonCharts(Selenium selenium , WebDriver driver) {
		this.driver = driver;
		this.selenium = selenium;
	}
	
	public Metric getTopMetric() {
		WebElement metricElement = driver.findElement(By.id(WebConstants.ID.comparisonMetricTop));
		return new Metric(metricElement, driver);
		
	}
	
	public Metric getBottomMetric() {
		WebElement metricElement = driver.findElement(By.id(WebConstants.ID.comparisonMetricBottom));
		return new Metric(metricElement, driver);
	}

}
