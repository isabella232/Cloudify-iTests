package test.webui.objects.topology.physicalpanel;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import test.webui.resources.WebConstants;
import framework.utils.WebUiUtils;

public class PuIBox {

	private int numberOfInstances;
	private int index;
	private String color;
	private String puName;
	private WebDriver driver;

	public PuIBox(int numberOfInstances, int index, String puName, String color, WebDriver driver) {
		this.index = index;
		this.numberOfInstances = numberOfInstances;
		this.puName = puName;
		this.color = color;
		this.driver = driver;
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
