package test.webui.objects.topology;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebElement;

import test.webui.resources.WebConstants;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.LogUtils;
import framework.utils.WebUiUtils;

public class Metric {
	
	private WebElement metric;
	private WebDriver driver;
	private String location;

	public Metric(WebElement metric, WebDriver driver) {
		this.driver = driver;
		this.metric = metric;
	}

	public Metric(String location, WebDriver driver) {
		this.driver = driver;
		this.location = location;
	}

	public boolean isDisplayed() {
		RemoteWebElement node = (RemoteWebElement) metric;
		return node.isDisplayed();
	}
	
	public boolean hasBalanceGuage() {
		
		try {
			@SuppressWarnings("unused")
			WebElement element = metric.findElement(By.className(WebConstants.ClassNames.balanceGauge));
			return true;
			
		}
		catch (NoSuchElementException e) {
			return false;
		}
	}
	
	public boolean hasBarLineChart() {
		
		try {
			@SuppressWarnings("unused")
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
		final String type = metricType.getType();
		
		
		LogUtils.log("Clicking on metric button");
		WebElement element = metric.findElement(By.tagName("button"));
		element.click();
		
		LogUtils.log("Waiting for " + metricType.getType() + "Option to be visible");
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				try {
					driver.findElement(By.linkText(type));
					return true;
				}
				catch (NoSuchElementException e) {
					return false;
				}
			}
		};
		AssertUtils.repetitiveAssertTrue("could not find " + metricType.getType() + " in the metric selection menu", condition, 30000);
	
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		final WebElement nameElement = driver.findElement(By.linkText(type));
		
		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				try {
					LogUtils.log("Trying to hover over " + type + "selection menu");
					Actions builder = new Actions(driver);
					builder.moveToElement(nameElement)
					.build().perform();
					try {
						Thread.sleep(100);
					}
					catch (InterruptedException e) {
						
					}
					WebElement nameElement = driver.findElement(By.linkText(name));
					nameElement.click();
					return true;
				}
				catch (NoSuchElementException e) {
					return false;
				}
			}
		};
		AssertUtils.repetitiveAssertTrue(metricType.getType() + "|" + metricType.getName() + " is not present in the selections menu", condition, 30000);
		
	}
	
	public Double getBalance() {
		WebElement balance = metric.findElement(By.cssSelector("div[data-widget='balance-gauge']"));
		return Double.parseDouble(balance.getAttribute("data-value"));
	}
	
	/**
	 * return the number of lines currently in the Metric
	 * note - if this is an instance of the metric in health panel, do not use this method
	 * @return
	 */
	public int getNumberOfGraphs() {		
		double seconds = 0;
		while (seconds < WebUiUtils.ajaxWaitingTime) {
			try {
				WebElement metricElement = driver.findElement(By.id(WebConstants.ID.comparisonMetricTop));
				WebElement highChartsTracker = metricElement.findElement(By.tagName("svg")).findElement(By.className("highcharts-tracker"));
				List<WebElement> paths = highChartsTracker.findElements(By.tagName("path"));
				int count = 0;
				for (WebElement element : paths) {
					if (!element.getAttribute("d").equals("M 0 0")) count++;
				}
				return count;
			}
			catch (StaleElementReferenceException e) {
				LogUtils.log("Failed to discover element due to statistics update, retyring...");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				seconds = seconds + 0.1;
			}
		}
		return 0;
	}


}
