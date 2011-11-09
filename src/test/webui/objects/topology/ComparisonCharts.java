package test.webui.objects.topology;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import test.webui.interfaces.RenderedWebUIElement;
import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

public class ComparisonCharts {
	
	private Selenium selenium;
	private WebDriver driver;
	
	public ComparisonCharts(Selenium selenium , WebDriver driver) {
		this.driver = driver;
		this.selenium = selenium;
	}
	
	public ComparisonMetric getTopMetric() {
		WebElement metricElement = driver.findElement(By.id(WebConstants.ID.comparisonMetricTop));
		return new ComparisonMetric(metricElement);
		
	}
	
	public ComparisonMetric getBottomMetric() {
		WebElement metricElement = driver.findElement(By.id(WebConstants.ID.comparisonMetricBottom));
		return new ComparisonMetric(metricElement);
	}
	
	
	
	public class ComparisonMetric extends Metric implements RenderedWebUIElement {

		public ComparisonMetric(WebElement metric) {
			super(metric);
			// TODO Auto-generated constructor stub
		}
		
		public boolean isGraphExists(String color) {
			return false;
		}
		
		/**
		 * return the number of lines currently in the Metric
		 * @return
		 */
		public int getNumberOfGraphs() {
			WebElement highChartsTracker = metric.findElement(By.className("highcharts-tracker"));
			List<WebElement> paths = highChartsTracker.findElements(By.tagName("path"));
			return paths.size();
			
		}
		
	}

}
