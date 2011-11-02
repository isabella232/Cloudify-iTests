package test.webui.objects.console;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

public class SpaceTreeSidePanel {
	
	@SuppressWarnings("unused")
	private Selenium selenium;
	private WebDriver driver;
	
	public SpaceTreeSidePanel(Selenium selenium, WebDriver driver) {
		this.selenium = selenium;
		this.driver = driver;
	}	
	
	/**
	 * Inputs the given filter in the filter spaces input box in the console tab
	 * after this method is invoked you should only see spaces that match the filter
	 * @param filter
	 */
	public void filterSpaces(String filter) {
		
		WebElement filterInput = driver.findElement(By.id(WebConstants.ID.sidePanelSpacesTree))
			.findElement(By.tagName("input"));
		filterInput.sendKeys(filter);
		
	}
	
	/**
	 * 
	 * @param spaceName
	 * @return an instance of {@link SpaceTreeNode} representing a space in the space tree of
	 * the console side tree panel 
	 */
	public SpaceTreeNode getSpaceTreeNode(String spaceName) {
		
		int i = 1;
		NoSuchElementException exception = null;
		while (exception == null) {
			try {
				String xpath = WebConstants.Xpath.getPathToSpaceTreeNodeByIndex(i);
				WebElement element = driver.findElement(By.xpath(xpath));
				String nameAndTopology = element.findElement(By.className("x-tree3-node-text")).getText();
				String name = nameAndTopology.split(" ")[0];
				String topology = nameAndTopology.split(" ")[1];
				if (name.equals(spaceName)) {
					return new SpaceTreeNode(xpath,spaceName, topology);
				}
				i++;
			}
			catch (NoSuchElementException e) {
				exception = e;
			}
		}
		return null;
		
	}
	
	public class SpaceTreeNode {
		
		private String xpath;
		private String name;
		private String topology;
		
		public SpaceTreeNode(String xpath,String name, String topology) {
			this.xpath = xpath;
			this.name = name;
			this.topology = topology;
		}
		
		public String getTopology() {
			return this.topology;
		}
		
		public void select() {
			WebElement element = driver.findElement(By.xpath(xpath));
			element.click();
		}
		
		public void selectInstance(int index) {
			
		}
		
		public boolean isExactlyOne() {
			
			List<SpaceTreeNode> nodes = new ArrayList<SpaceTreeNode>();
			
			int i = 1;
			NoSuchElementException exception = null;
			while (exception == null) {
				try {
					String xpath = WebConstants.Xpath.getPathToSpaceTreeNodeByIndex(i);
					WebElement element = driver.findElement(By.xpath(xpath));
					String nameAndTopology = element.findElement(By.className("x-tree3-node-text")).getText();
					String name = nameAndTopology.split(" ")[0];
					String topology = nameAndTopology.split(" ")[1];
					if (name.equals(this.name)) {
						nodes.add(new SpaceTreeNode(xpath,name, topology));
					}
					i++;
				}
				catch (NoSuchElementException e) {
					exception = e;
				}
			}
			if (nodes != null) {
				if (nodes.size() == 1) return true;
			}
			return false;
			
		}
		
	}
	
}
