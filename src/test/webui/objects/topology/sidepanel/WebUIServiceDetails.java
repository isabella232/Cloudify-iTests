package test.webui.objects.topology.sidepanel;

import java.util.HashMap;
import java.util.Map;

import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;
import com.thoughtworks.selenium.SeleniumException;

public class WebUIServiceDetails {
	
	private Selenium selenium;
	
	public WebUIServiceDetails(Selenium selenium) {
		this.selenium = selenium;
	}
	
	public final static String EMBEDDED_SPACE = "Embedded Space";
	
	public Map<String, Map<String,String>> getDetails() {
		
		SeleniumException exceptionOuter = null;
		int i = 1;
		HashMap<String, Map<String, String>> details = new HashMap<String, Map<String,String>>();
		while (exceptionOuter == null) {
			try {
				SeleniumException exceptionInner = null;
				String pathToGroup = null;
				pathToGroup = WebConstants.Xpath.getPathToDetailsGroup(i);
				String groupName = selenium.getText(pathToGroup + WebConstants.Xpath.pathToGroupName);
				int j = 1;
				Map<String, String> subDetails = new HashMap<String, String>();
				while (exceptionInner == null) {
					try {
						String pathToAttribute = pathToGroup + WebConstants.Xpath.getPathToAttributeInGroup(j);
						subDetails.put(selenium.getText(pathToAttribute + WebConstants.Xpath.pathToAttributeName),
								selenium.getText(pathToAttribute + WebConstants.Xpath.pathToAttributeValue));
						j++;
					}
					catch (SeleniumException e) {
						exceptionInner = e;
					}
				}
				details.put(groupName, subDetails);
				i++;
			}
			catch (SeleniumException e) {
				exceptionOuter = e;
			}
		}
		return details;
		
	}

}
