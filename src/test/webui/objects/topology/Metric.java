package test.webui.objects.topology;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebElement;

import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.resources.WebConstants;

public class Metric {
	
	WebElement metric;
	String type;
	WebDriver driver;

	public Metric(WebElement metric, WebDriver driver) {
		this.driver = driver;
		this.metric = metric;
	}

	public boolean isDisplayed() {
		RemoteWebElement node = (RemoteWebElement) metric;
		return node.isDisplayed();
	}
	
	public boolean hasBalanceGuage() {
		
		try {
			WebElement element = metric.findElement(By.className(WebConstants.ClassNames.balanceGauge));
			return true;
			
		}
		catch (NoSuchElementException e) {
			return false;
		}
	}
	
	public boolean hasBarLineChart() {
		
		try {
			WebElement element = metric.findElement(By.className(WebConstants.ClassNames.barLineChartContainer));
			return true;
			
		}
		catch (NoSuchElementException e) {
			return false;
		}
	}
	
	public String getName() {

		WebElement element = metric.findElement(By.tagName("button"));
		return element.getText();

	}
	
	public void swithToMetric(MetricType metricType) {
		
		final String name = metricType.getName();
		String type = metricType.getType();
		
		WebElement element = metric.findElement(By.tagName("button"));
		element.click();
		
		WebElement nameElement = driver.findElement(By.linkText(type));
		Actions builder = new Actions(driver);
		builder.moveToElement(nameElement).release()
			.build().perform();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				try {
					WebElement nameElement = driver.findElement(By.linkText(name));
					return true;
				}
				catch (NoSuchElementException e) {
					return false;
				}
			}
		};
		AssertUtils.repetitiveAssertTrue("metric name is not present in the selections menu", condition, 5000);
		driver.findElement(By.linkText(name)).click();
		
	}
	
	public Double getBalance() {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		String balance = (String) js.executeScript(WebConstants.JQuery.getMetricBalanceScript(type));
		return Double.parseDouble(balance);
	}
	
	/**
	 * return the number of lines currently in the Metric
	 * note - if this is an instance of the metric in health panel, do not use this method
	 * @return
	 */
	public int getNumberOfGraphs() {
		WebElement highChartsTracker = metric.findElement(By.className("highcharts-tracker"));
		List<WebElement> paths = highChartsTracker.findElements(By.tagName("path"));
		return paths.size();
		
	}


}
