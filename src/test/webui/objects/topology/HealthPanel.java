package test.webui.objects.topology;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import test.webui.interfaces.RenderedWebUIElement;
import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

public class HealthPanel extends TopologySubPanel {

	public HealthPanel(Selenium selenium, WebDriver driver) {
		this.driver = driver;
		this.selenium = selenium;
	}

	public static HealthPanel getInstance(Selenium selenium, WebDriver driver) {
		return new HealthPanel(selenium,driver);
	}
	
	public HealthMetric getMetric(String type) {
		
		type = type.replaceAll(" ", "_");
		HealthMetric metric = null;
		String classMetricName = null;
		WebElement element;
		try {
			classMetricName = WebConstants.ClassNames.getMetricClassName(type);
			element = driver.findElement(By.className(classMetricName));
			metric = new HealthMetric(element, classMetricName);
		}
		catch (WebDriverException e) {
			return null;
		}
		return metric;
	}
	
	public class HealthMetric extends Metric implements RenderedWebUIElement {

		public HealthMetric(WebElement metric, String type) {
			super(metric, type);
		}

		public Double getRotation() {
			return 0d;
		}
		
		public Double getBalance() {
			JavascriptExecutor js = (JavascriptExecutor) driver;
			String balance = (String) js.executeScript(WebConstants.JQuery.getMetricBalanceScript(type));
			return Double.parseDouble(balance);
		}
		
	}
}
