package test.webui.objects.topology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import test.webui.objects.dashboard.ServicesGrid.Icon;
import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

import framework.utils.AssertUtils;
import framework.utils.LogUtils;
import framework.utils.WebUiUtils;

/**
 * This class is a mapping of the Physical Tabular tab in the topology tab
 * It offers methods for all data retrieval possible in this panel
 * @author elip
 *
 */
public class PhysicalPanel extends TopologySubPanel {
	
	public PhysicalPanel(Selenium selenium, WebDriver driver) {
		this.driver = driver;
		this.selenium = selenium;
	}
	
	/**
	 * @param name - host name to be retrieved
	 * @return a table row of the physical tab containing all possible information about a specific host
	 */
	public Host getHost(String name) {
		Host host = new Host(name);
		if (host.getName() != null) return host;
		return null;
	}
	
	public enum OS {
		WINDOWS32,LINUX;
	}
	
	/**
	 * represents a single row in the physical panel hosts table
	 * @author elip
	 *
	 */
	public class Host {
		
		@SuppressWarnings("unused")
		private WebElement host;
		private String name;
		
		public Host(String hostName) {
			try {
				String id = WebConstants.ID.getHostId(hostName);
				WebElement hostElement = driver.findElement(By.id(id));
				this.host = hostElement;
				this.name = hostName;
			}
			catch (NoSuchElementException e) {
			}
			catch (WebDriverException e) {
			}
			
		}
		
		public String getName() {
			return name;
		}
		
		public Icon getIcon() {
			
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					String id = WebConstants.ID.getHostId(name);
					WebElement hostElement = driver.findElement(By.id(id));
					WebElement icon = hostElement.findElement(By.className("x-grid3-td-status")).findElement(By.tagName("span"));
					String type = icon.getAttribute("class").trim();
					if (type.equals(WebConstants.ID.okIcon)) return Icon.OK;
					if (type.equals(WebConstants.ID.criticalIcon)) return Icon.CRITICAL;
					if (type.equals(WebConstants.ID.warningIcon)) return Icon.ALERT;
					if (type.equals(WebConstants.ID.naIcon)) return Icon.NA;
					return null;
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
		
		/**
		 * 
		 * @return the number of cores present on the host
		 * @throws InterruptedException 
		 */
		public Integer getNumberOfCores() {
			
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					String id = WebConstants.ID.getHostId(name);
					WebElement hostElement = driver.findElement(By.id(id));
					WebElement cores = hostElement.findElement(By.className("x-grid3-td-corecpus"));
					String coreCount = cores.getText();
					if (coreCount != " ") {
						return Integer.parseInt(coreCount);
					}
					else return 0;
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
		
		
		/**
		 * 
		 * @return The specific OS the hosts operates under
		 * @throws InterruptedException 
		 */
		public OS getOS() {
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					String id = WebConstants.ID.getHostId(name);
					WebElement hostElement = driver.findElement(By.id(id));
					WebElement os = hostElement.findElement(By.className("x-grid3-td-os_type")).findElement(By.tagName("span"));
					String osType = os.getAttribute("class").trim();
					if (osType.equals(WebConstants.ClassNames.win32OS)) return OS.WINDOWS32;
					else return OS.LINUX;
				}
				catch (StaleElementReferenceException e) {
					LogUtils.log("Failed to discover element due to statistics update, retyring...");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					seconds++;
				}
			}
			return null;
		}
		
		/**
		 * 
		 * @return all ProcessingUnitInstances deployed on the host. 
		 * this is represented as a list of {@link test.webui.objects.topology.Host.PuIBox}
		 * @throws InterruptedException 
		 */
		public PuIBoxes getPUIs() {
			return new PuIBoxes(name);
			
		}
		
		/**
		 * 
		 * @return number of GridServiceContainer running on the host
		 * @throws InterruptedException 
		 */
		public Integer getGSCCount() {
			
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					String id = WebConstants.ID.getHostId(name);
					WebElement hostElement = driver.findElement(By.id(id));
					WebElement gsc = hostElement.findElement(By.className("x-grid3-td-gsc_indication"));
					String gscCount = gsc.getText();
					if (!gscCount.equals(" ")) {
						return Integer.parseInt(gscCount);
					}
					else return 0;
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
		
		public Integer getGSACount() {
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					String id = WebConstants.ID.getHostId(name);
					WebElement hostElement = driver.findElement(By.id(id));
					WebElement gsa = hostElement.findElement(By.className("x-grid3-td-gsa_indication"));
					String gsaCount = gsa.getText();
					if (!gsaCount.equals(" ")) {
						return Integer.parseInt(gsaCount);
					}
					else return 0;
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
		
		/**
		 * 
		 * @return the number GridServiceManagers running on the host
		 * @throws InterruptedException 
		 */
		public Integer getGSMCount() {
			int seconds = 0;
			while (seconds < WebUiUtils.ajaxWaitingTime) {
				try {
					String id = WebConstants.ID.getHostId(name);
					WebElement hostElement = driver.findElement(By.id(id));
					WebElement gsm = hostElement.findElement(By.className("x-grid3-td-gsm_indication"));
					String gsmCount = gsm.getText();
					if (!gsmCount.equals(" ")) {
						return Integer.parseInt(gsmCount);
					}
					else return 0;
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
		
		
		public class PuIBoxes {
			
			private List<PuIBox> puis;
			
			public PuIBoxes(String name) {
				List<PuIBox> puBoxes = new ArrayList<PuIBox>();
				List<WebElement> rectsAll = null;
				List<WebElement> textsAll = null;	
				
				List<WebElement> rects = new ArrayList<WebElement>();
				List<WebElement> texts = new ArrayList<WebElement>();
				
				List<String> textsString = null;
				List<String> puNames = null;
				List<String> colors = null;
				
				int seconds = 0;
				while (seconds < WebUiUtils.ajaxWaitingTime) {
					try {
						String id = WebConstants.ID.getHostId(name);
						WebElement hostElement = driver.findElement(By.id(id));
						rectsAll = hostElement.findElement(By.className("x-grid3-td-processing_unit_instances")).findElements(By.tagName("rect"));
						textsAll = hostElement.findElement(By.className("x-grid3-td-processing_unit_instances")).findElements(By.tagName("text"));
						
						for (int i = 0 ; i < rectsAll.size() ; i++) {
							WebElement eRect = rectsAll.get(i);
							WebElement eText = textsAll.get(i);
							if ((eRect.getAttribute("class") != null) && (eText.getAttribute("class") != null)) {
								rects.add(eRect);
								texts.add(eText);
							}
						}
						
						
						sortByPosition(rects);
						sortByPosition(texts);
						
						textsString = new ArrayList<String>();
						puNames = new ArrayList<String>();
						colors = new ArrayList<String>();
						
						for (int i = 0 ; i < rects.size() ; i++) {
							textsString.add(texts.get(i).getText());
							puNames.add(rects.get(i).getAttribute("class").trim().split(" ")[0].substring(13));
							colors.add(rects.get(i).getAttribute("class").split(" ")[1].substring(13));
						}
						
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
					
				for (int i = 0 ; i < rects.size() ; i++) {
					puBoxes.add(new PuIBox(Integer.parseInt(textsString.get(i)), i + 1, puNames.get(i), colors.get(i)));
				}
				AssertUtils.assertTrue("Test failed because it was unable to discover the element",seconds != WebUiUtils.ajaxWaitingTime);
				this.puis = puBoxes;
			
			}
			
			private void sortByPosition(List<WebElement> rects) {
			
				WebElement[] array = new WebElement[rects.size()];
				rects.toArray(array);
				
				for (int i = 0 ; i < rects.size() ; i++) {
					Arrays.sort(array, new Comparator<WebElement>() {

						@Override
						public int compare(WebElement arg0, WebElement arg1) {
							double x0 = Double.parseDouble(arg0.getAttribute("x"));
							double x1 = Double.parseDouble(arg1.getAttribute("x"));
							if (x0 == x1) return 0;
							if (x0 < x1) return -1;
							return 1;
						}
					});
				}
				
				rects.clear();
				for (WebElement e : array) {
					rects.add(e);
				}
			}

			public List<PuIBox> getBoxes() {
				return puis;
			}
			
			public List<PuIBox> getPuIBoxesOfAdifferentApplication() {
				
				List<PuIBox> result = new ArrayList<PuIBox>();
				for (PuIBox p : puis) {
					if (p.getColor().equals("null")) result.add(p);
				}
				return result;
				
			}
			
			public List<PuIBox> getPuIBoxesOfCurrentApplication() {
				
				List<PuIBox> result = new ArrayList<PuIBox>();
				for (PuIBox p : puis) {
					if (!p.getColor().equals("null")) result.add(p);
				}
				return result;
			}	
		}
		
		public class PuIBox {
			
			private int numberOfInstances;
			private int index;
			private String color;
			private String puName;
			
			public PuIBox(int numberOfInstances, int index, String puName, String color) {
				this.index = index;
				this.numberOfInstances = numberOfInstances;
				this.puName = puName;
				this.color = color;
			}
			
			public String getStroke() {
				return WebUiUtils.retrieveAttribute(By.id(WebConstants.ID.getPuBoxRectId(puName)), "stroke", driver);
			}

			/**
			 * this method should be used for making assertions on the PuIBox color in comparison
			 * to the corresponding pu node in the application map
			 * @return the color of the pu box. value is returned in hexa format.
			 */
			public String getColor() {
				return this.color;
				//return puBoxRect.getAttribute("class").substring(13);
			}
			
			public String getName() {
				return this.puName;
			}

			/**
			 * 
			 * @return number of instances of a certain pu running on a specific host
			 */
			public int getNumberOfInstances() {
				return numberOfInstances;
			}
			
			/**
			 * 
			 * @return the index of the pu relative to others. 1 is the at most left one
			 */
			public int getIndex() {
				return index;
			}
			
			
			public String getAssociatedProcessingUnitName() {
				return puName;
			}
		}
	}
	
}
