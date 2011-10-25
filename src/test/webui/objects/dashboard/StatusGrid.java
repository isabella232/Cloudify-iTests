package test.webui.objects.dashboard;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import test.webui.objects.dashboard.ServicesGrid.Icon;
import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

public class StatusGrid {

	Selenium selenium;
	WebDriver driver;
	HealthGauge gauge;
	CPUCores cpuCores;
	Memory memory;

	public StatusGrid(Selenium selenium, WebDriver driver) {
		this.selenium = selenium;
		this.driver = driver;
		this.gauge = new HealthGauge();
	}

	public static StatusGrid getInstance(Selenium selenium, WebDriver driver) {
		return new StatusGrid(selenium, driver);
	}
	
	public HealthGauge getHealthGauge() {
		return this.gauge;
	}
	
	public CPUCores getCpuCores() {
		return new CPUCores();
	}
	
	public Memory getMemory() {
		return new Memory();
	}
	
	private Icon getIcon(String type) {
		if (type.equals(WebConstants.ID.okIcon)) return Icon.OK;
		if (type.equals(WebConstants.ID.criticalIcon)) return Icon.CRITICAL;
		if (type.equals(WebConstants.ID.warningIcon)) return Icon.ALERT;
		if (type.equals(WebConstants.ID.naIcon)) return Icon.NA;
		return null;
	}
	
	public String getGridHealth() {
		return selenium.getText(WebConstants.Xpath.pathToGridHealthInGridStatus);
	}
	
	public class HealthGauge {
		
		public static final double POINTER_MIN_ANGLE = -17.4;
		public static final double POINTER_MAX_ANGLE = 17.4;
		
		public double getRotation() {
			WebElement svgGaugePointer = driver.findElement(By.id(WebConstants.ID.healthGaugePointer));
			String transform = svgGaugePointer.getAttribute("transform");
			return Double.parseDouble(transform.substring(7, 13));
		}
	}
	
	public class CPUCores {
		
		public Icon getIcon() {
			WebElement icon = driver.findElement(By.xpath(WebConstants.Xpath.pathToCpuCoresInGridStatus + WebConstants.Xpath.pathToIconInResourcesPanelOfGridStatus));
			String style = icon.getAttribute("class");
			return StatusGrid.this.getIcon(style);
		}
		
		public Double getCount() {
			String perc = selenium.getText(WebConstants.Xpath.pathToCpuCoresInGridStatus + WebConstants.Xpath.pathToNumberInResourcesPanelOfGridStatus);
			int i = 0;
			while (perc.charAt(i++) != '%');
			return Double.parseDouble(perc.substring(0, i - 1));	
		}
		
	}
	
	public class Memory {
		
		public Icon getIcon() {
			WebElement icon = driver.findElement(By.xpath(WebConstants.Xpath.pathToMemoryInGridStatus + WebConstants.Xpath.pathToIconInResourcesPanelOfGridStatus));
			String style = icon.getAttribute("class");
			return StatusGrid.this.getIcon(style);
		}
		
		public Double getCount() {
			String perc = selenium.getText(WebConstants.Xpath.pathToMemoryInGridStatus + WebConstants.Xpath.pathToNumberInResourcesPanelOfGridStatus);
			int i = 0;
			while (perc.charAt(i++) != '%');
			return Double.parseDouble(perc.substring(0, i - 1));	
		}
		
	}
	
	
}
