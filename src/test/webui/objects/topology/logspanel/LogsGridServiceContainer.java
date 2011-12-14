package test.webui.objects.topology.logspanel;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import test.webui.resources.WebConstants;


public class LogsGridServiceContainer {

	@SuppressWarnings("unused")
	private String contianerId;
	private String xpath;
	@SuppressWarnings("unused")
	private String puName;
	@SuppressWarnings("unused")
	private String agentId;
	private WebDriver driver;
	
	public LogsGridServiceContainer(String contianerId,String agentId ,String puName, WebDriver driver) {
		this.driver = driver;
		this.contianerId = contianerId;
		this.agentId = agentId;
		this.xpath = WebConstants.Xpath.getPathToLogsContianer(contianerId,agentId, puName);
		this.puName = puName;
	}
	
	public List<String> getPuInstances() {	
		Exception exception = null;
		List<String> services = new ArrayList<String>();
		int i = 1;
		while (exception == null) {
			try {
				WebElement serviceElement = driver.findElement(By.
						xpath(xpath + WebConstants.Xpath.getPathToLogsContianerPuInstanceByIndex(i)));
				String service = serviceElement.getText();
				services.add(service);
				i++;
			}
			catch (NoSuchElementException e) {
				exception = e;
			}
			catch (WebDriverException e) {
				exception = e;
			}
		}
		return services;	
	}
}
