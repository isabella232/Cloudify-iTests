package test.webui.objects.console;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import test.webui.objects.topology.physicalpanel.PhysicalPanel.OS;
import test.webui.resources.WebConstants;

import com.gigaspaces.cluster.activeelection.SpaceMode;
import com.thoughtworks.selenium.Selenium;

public class SpaceInstancesGrid {
	
	@SuppressWarnings("unused")
	private Selenium selenium;
	private WebDriver driver;

	public SpaceInstancesGrid(Selenium selenium, WebDriver driver) {
		this.selenium = selenium;
		this.driver = driver;
	}
	
	/**
	 * 
	 * @return a list of {@link SpaceInstance} containing all information about instances
	 * as presented in the console tab
	 */
	public List<SpaceInstance> getSpaceInstances() {
		
		NoSuchElementException exception = null;
		int i = 1;
		
		List<SpaceInstance> result = new ArrayList<SpaceInstance>();
		while (exception == null) {
			try {
				String xpath = WebConstants.Xpath.getPathToSpaceInstanceByIndex(i);
				WebElement element = driver.findElement(By.xpath(xpath));
				if (element.isDisplayed()) {
					result.add(new SpaceInstance(xpath));
					i++;
				}
			}
			catch (NoSuchElementException e) {
				exception = e;
			}
		}
		return result;
	}
	
	
	/**
	 * represents a space instance with all its attributes as showed in the SpaceInstancesGrid of
	 * the console tab
	 * @author elip
	 *
	 */
	public class SpaceInstance {
		
		private String xpath;

		public SpaceInstance(String xpath) {
			this.xpath = xpath;
		}
		
		
		public SpaceMode getSpaceType() {
			WebElement element = driver.findElement(By.xpath(xpath)).findElement(By.className("x-grid3-td-instnaceType"));
			String type = element.findElement(By.tagName("span")).getAttribute("class");
			if (type.equals(WebConstants.ClassNames.primarySpaceImage)) return SpaceMode.PRIMARY;
			if (type.equals(WebConstants.ClassNames.backupSpaceImage)) return SpaceMode.BACKUP;
			return null;
		}
		
		public int getID() {
			WebElement element = driver.findElement(By.xpath(xpath)).findElement(By.className("x-grid3-td-instanceId"));
			return Integer.parseInt(element.getText());
		}
		
		public int getBackupId() {
			WebElement element = driver.findElement(By.xpath(xpath)).findElement(By.className("x-grid3-td-backupId"));
			return Integer.parseInt(element.getText());
		}
		
		public String getHostAdress() {
			WebElement element = driver.findElement(By.xpath(xpath)).findElement(By.className("x-grid3-td-hostIP"));
			return element.getText();
		}
		
		public OS getOS() {
			WebElement element = driver.findElement(By.xpath(xpath)).findElement(By.className("x-grid3-td-osInfo"));
			if (element.getText().contains("Win32")) return OS.WINDOWS32;
			return null;
		}
		
		public String getJVMInfo() {
			WebElement element = driver.findElement(By.xpath(xpath)).findElement(By.className("x-grid3-td-vmDetails"));
			return element.getText();
		}
		
		public int getCount() {
			WebElement element = driver.findElement(By.xpath(xpath)).findElement(By.className("x-grid3-td-typeCount"));
			return Integer.parseInt(element.getText());
		}
		
	}

}
