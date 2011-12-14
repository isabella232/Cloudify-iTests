package test.webui.objects.topology.sidepanel;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import test.webui.objects.topology.Metric;
import test.webui.resources.WebConstants;

public class ComparisonCharts {
	
	private WebDriver driver;
	
	public ComparisonCharts(WebDriver driver) {
		this.driver = driver;
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
