package test.webui.objects.services;

import org.openqa.selenium.WebDriver;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;

import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;
import com.thoughtworks.selenium.SeleniumException;

import framework.utils.WebUiUtils;

/**
 * represents the Host and Services grid found in the web-ui under the Topology tab
 * this class is a singleton, use getInstance to obtain the instance.
 * @author elip
 *
 */
public class HostsAndServicesGrid {
	
	public static final int GSC = 0;
	public static final int GSM = 1;
	public static final int LUS = 2;
	
	Selenium selenium;
	WebDriver driver;
	long gsaPID;

	public HostsAndServicesGrid(Selenium selenium, WebDriver driver) {
		this.selenium = selenium;
		this.driver = driver;
	}
	
	public static HostsAndServicesGrid getInstance(Selenium selenium, WebDriver driver) {
		return new HostsAndServicesGrid(selenium, driver);
	}
	
	/**
	 * set the gsa process id you currently wish to work with.
	 * this method must be invoked before trying to start grid service components.
	 * @param gsa
	 */
	public void setGsaPID(GridServiceAgent gsa) {
		this.gsaPID = gsa.getVirtualMachine().getDetails().getPid();
	}
	
	/**
	 * starts any type of Grid Service Component. uses the gsa provided in
	 * {@link setGsaPID(GridServiceAgent gsa)}. 
	 * @param hostname - must be the host name attached to the gsa
	 * @param component - an integer value for a component. 
	 *                    use static fields in {@link HostsAndServicesGrid}}
	 * @throws InterruptedException 
	 */
	public void startGridServiceComponent(String hostname, int component) throws InterruptedException {
		WebUiUtils.waitForHost(hostname, selenium);
		selenium.click(WebConstants.Xpath.getPathToHostnameOptions(hostname));
		selenium.click(WebConstants.Xpath.getPathToGsaOption(gsaPID));
		switch (component)  {
		case 0: {
			selenium.click(WebConstants.ID.startGSC);
			break;
		}
		case 1: {
			selenium.click(WebConstants.ID.startGSM);
			break;
		}
		case 2: {
			selenium.click(WebConstants.ID.startLUS);
		}
		}
	}
	
	/**
	 * terminates a certain GSC via the web-ui
	 * @param gsc
	 */
	public void terminateGSC(GridServiceContainer gsc) {
		String gscPid = "" + gsc.getVirtualMachine().getDetails().getPid();
		int gscDivIndex = 2;
		while (true) {
			if (selenium.getText(WebConstants.Xpath.getPathToRowNumber(gscDivIndex))
				.contains(gscPid)) break;
			else gscDivIndex++;
		}
		int gscIndex = WebUiUtils.extractGSCNumber(gscDivIndex, selenium);
		selenium.click(WebConstants.Xpath.getPathToGscOption(gscPid, gscIndex));
		selenium.click(WebConstants.ID.terminateComponent);	
		selenium.click(WebConstants.Xpath.acceptAlert);
	}
	
	public void restartGSC(GridServiceContainer gsc) {
		String gscPid = "" + gsc.getVirtualMachine().getDetails().getPid();
		int gscDivIndex = 2;
		while (true) {
			if (selenium.getText(WebConstants.Xpath.getPathToRowNumber(gscDivIndex))
				.contains(gscPid)) break;
			else gscDivIndex++;
		}
		int gscIndex = WebUiUtils.extractGSCNumber(gscDivIndex, selenium);
		selenium.click(WebConstants.Xpath.getPathToGscOption(gscPid, gscIndex));
		selenium.click(WebConstants.ID.restartComponent);
		selenium.click(WebConstants.Xpath.acceptAlert);
	}
	
	public boolean isGSCPresent(GridServiceContainer gsc) {
		String processPid = "" + gsc.getVirtualMachine().getDetails().getPid();
		return selenium.isTextPresent(processPid);
	}
	
	public void clickOnHost(String hostname) {
		selenium.click(WebConstants.Xpath.getPathToHostnameOptions(hostname));
	}
	
	/**
	 * @param component , input lower case strings only
	 * @return number of components of type 'component'.
	 */
	public int countNumberOf(String component) {
		int count = 0;
		int i = 2;
		String rowText = null;
		SeleniumException exception = null;
		
		while (exception == null) {
			try {
				rowText = selenium.getText(WebConstants.Xpath.getPathToRowNumber(i));
				if (rowText.contains(component)) {
					count++;
				}
				i++;
			}
			catch (SeleniumException e) {
				exception = e;
			}
		}
		return count;
	}


}
