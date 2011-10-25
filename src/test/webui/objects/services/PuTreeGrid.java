package test.webui.objects.services;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openspaces.admin.pu.DeploymentStatus;

import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;
import com.thoughtworks.selenium.SeleniumException;

import framework.utils.LogUtils;
import framework.utils.WebUiUtils;

/**
 * represents the Processing Unit Tree Grid found in the web-ui under the services tab.
 * @author elip
 *
 */
public class PuTreeGrid {
	
	Selenium selenium;
	WebDriver driver;
	
	public PuTreeGrid(Selenium selenium, WebDriver driver) {
		this.selenium = selenium;
		this.driver = driver;
	}
	
	public static PuTreeGrid getInstance(Selenium selenium, WebDriver driver) {
		return new PuTreeGrid(selenium, driver);
	}
	
	public WebUIProcessingUnit getProcessingUnit(String name) {
		
		Exception exception = null;
		String xPath;
		WebElement pu;
		int i = 1;
		WebUIProcessingUnit webUIProcessingUnit = null;
		
		while (exception == null) {
			
			try {
				xPath = WebConstants.Xpath.getPathToProcessingUnitByIndex(i);
				pu = driver.findElement(By.xpath(xPath));
				webUIProcessingUnit = new WebUIProcessingUnit(xPath);
				if (webUIProcessingUnit.getName().equals(name)) {
					break;
				}
				Thread.sleep(1000);
				i++;
				webUIProcessingUnit = null;
			}
			catch (NoSuchElementException e) {
				exception = e;
			}
			catch (SeleniumException e) {
				exception = e;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return webUIProcessingUnit;
	}
	
	public class WebUIProcessingUnit {
		
		private String xpath;
		private String name;

		
		public WebUIProcessingUnit(String xpath) {
			this.xpath = xpath;
			this.name = getNameFromUI();
		}
		
		public String getName() {
			return this.name;
		}
		public DeploymentStatus getStatus() {
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					WebElement status = driver.findElement(By.xpath(xpath + WebConstants.Xpath.pathToPuStatus));
					String stat = status.getAttribute("title");
					if (stat.equals("compromised")) return DeploymentStatus.COMPROMISED;
					if (stat.equals("intact")) return DeploymentStatus.INTACT;
					if (stat.equals("broken")) return DeploymentStatus.BROKEN;
					else return DeploymentStatus.SCHEDULED;
				}
				catch (StaleElementReferenceException e) {
					LogUtils.log("Failed to discover element due to statistics update, retyring...");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					seconds++;
				}
			}
			return null;
		}
		public String getType() {
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					return selenium.getText(xpath + WebConstants.Xpath.pathToPuType);
				}
				catch (StaleElementReferenceException e) {
					LogUtils.log("Failed to discover element due to statistics update, retyring...");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					seconds++;
				}
			}
			return null;
			
		}
		public Integer getActualInstances() {		
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					return Integer.parseInt(selenium.getText(xpath + WebConstants.Xpath.pathToPuActualInstances));
				}
				catch (StaleElementReferenceException e) {
					LogUtils.log("Failed to discover element due to statistics update, retyring...");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					seconds++;
				}
			}
			return null;
		}
		public Integer getPlannedInstances() {
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					return Integer.parseInt(selenium.getText(xpath + WebConstants.Xpath.pathToPuPlannedInstances));
				}
				catch (StaleElementReferenceException e) {
					LogUtils.log("Failed to discover element due to statistics update, retyring...");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					seconds++;
				}
			}
			return null;
		}
		
		private String getNameFromUI(){
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					return selenium.getText(xpath + WebConstants.Xpath.pathToPuName);				}
				catch (StaleElementReferenceException e) {
					LogUtils.log("Failed to discover element due to statistics update, retyring...");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					seconds++;
				}
			}
			return null;
		}
		
		
		/**
		 * undeploys a given processing unit by name
		 * @param processingUnitName
		 */
		public void undeploy() {
			selenium.click(this.xpath + WebConstants.Xpath.pathToPuOptionsButton);
			selenium.click(WebConstants.ID.undeployPuButton);
			selenium.click(WebConstants.Xpath.acceptAlert);
		}
		
		/**
		 * restarts a pu instance
		 */
		public void restartPuInstance(int partition) {
			selenium.click(WebConstants.Xpath.getPathToPuButton(this.name));
			selenium.click(WebConstants.Xpath.getPathToPartitionButton(this.name, partition));
			selenium.click(WebConstants.Xpath.getPathToPuInstanceButton(this.name, partition));
			selenium.click(WebConstants.ID.restartPuInstance);
			selenium.click(WebConstants.Xpath.acceptAlert);
		}
		
		public boolean isPartitioned() {
			selenium.click(WebConstants.Xpath.getPathToPuButton(this.name));
			return selenium.isTextPresent(this.name + ".");		
		}

		public void expand() {
			
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					selenium.click(WebConstants.Xpath.getPathToPuButton(this.name));
					break;
					}
				catch (StaleElementReferenceException e) {
					LogUtils.log("Failed to discover element due to statistics update, retyring...");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					seconds++;
				}
			}		
		}
		
		public void collapseAll() {
			
			Exception exception = null;
			
			int i = 1;
			while (exception == null) {
				try {
					clickOnPartition(i);
					i++;
				}
				catch (SeleniumException e) {
					exception = e;
				}
			}
			this.expand();
			
		}
		
		public boolean isProcessingUnitInstancePresent(String processingUnitInstanceName) {
			
			Exception exception = null;
			
			this.expand();
			int i = 1;
			while (exception == null) {
				try {
					clickOnPartition(i);
					if (selenium.isTextPresent(processingUnitInstanceName)) {
						clickOnPartition(i);
						this.expand();
						return true;
					}
					clickOnPartition(i);
					i++;
				}
				catch (SeleniumException e) {
					exception = e;
				}
			}
			this.expand();
			return false;
			
		}
		
		public void clickOnPartition(int index) throws SeleniumException {
			
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					selenium.click(WebConstants.Xpath.getPathToPartitionButton(this.name, index));
					break;
					}
				catch (StaleElementReferenceException e) {
					LogUtils.log("Failed to discover element due to statistics update, retyring...");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					seconds++;
				}
				catch (SeleniumException e) {
					throw e;
				}
			}
			
		}
			
	}
	
}
