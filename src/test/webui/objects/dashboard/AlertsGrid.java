package test.webui.objects.dashboard;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openspaces.admin.alert.Alert;
import org.openspaces.admin.alert.AlertSeverity;
import org.openspaces.admin.alert.AlertStatus;
import org.testng.Assert;

import test.webui.resources.WebConstants;

import com.thoughtworks.selenium.Selenium;

import framework.utils.AssertUtils;
import framework.utils.LogUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

/**
 * represents the Alerts grid found in the web-ui under the Dashboard tab
 * this class is a singleton, use getInstance to obtain the instance.
 * @author elip
 *
 */
public class AlertsGrid {
	
	public static final String REPLICATION = "Replication Channel Disconnected";
	
	public static final String GC = "Garbage Collection";

	public static final String PHYSICAL_MEMORY = "Physical Memory Utilization";

	public static final String CPU_UTILIZATION = "CPU Utilization";

	public static final String HEAP_MEMORY = "Heap Memory Utilization";

	public static final String PROVISION = "Provision Failure";

	public static String MIRROR = "Mirror Persistence Failure";
	
	public static String REDOLOG_OVERFLOW = "Replication Redo log Overflow";
	
	public static String REDOLOG_SIZE = "Replication Redo log";
	
	public static String MEMBER_ALIVE = "Member Alive Indicator";
	
	public static String ELASTIC_GSA_ALERT = "Grid Service Agent Provisioning Alert";
	public static String ELASTIC_GSC_ALERT = "Grid Service Container Provisioning Alert";
	public static final String ELASTIC_MACHINE_ALERT = "Machine Provisioning Alert";

	Selenium selenium;
	WebDriver driver;

	public AlertsGrid(Selenium selenium, WebDriver driver) {
		this.selenium = selenium;
		this.driver = driver;
	}

	public static AlertsGrid getInstance(Selenium selenium, WebDriver driver) {
		return new AlertsGrid(selenium, driver);
	}
	
	public void waitForAlerts(final AlertStatus status, final String alertType, final int numberOfResolved){
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				int i = 0;
				List<WebUIAlert> webuiAlerts = getParentAlertsByType(alertType);
				for (WebUIAlert alert : webuiAlerts) {
					if (alert.getStatus().equals(status)){
						i++;
					}
				}
				if (i == numberOfResolved) return true;
				return false;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, 60000);
	}
	
	/**
	 * 
	 * @param alerts - the alerts retrieved from the webui
	 * @param adminAlerts - the alerts triggered by the test
	 */
	public void assertAlertsConsistency(List<WebUIAlert> alerts, List<Alert> adminAlerts) {
		List<String> alertGroupIDS = new ArrayList<String>();
		
		for (Alert alert : adminAlerts) {
			/* if a resolved alert appears in the admin alerts, it must have a corresponding resolved alert
			   from the webui */
			if (alert.getStatus().equals(AlertStatus.RESOLVED)) {
				boolean found = false;
				for (WebUIAlert webuiAlert : alerts) {
					if (webuiAlert.equals(alert)) {
						found = true;
						break;
					}
				}
				Assert.assertTrue(found);
			}
			/* if a raised alert appears in the admin alerts, it must have at least one corresponding 
			   alert in the ui */
			if (alert.getStatus().equals(AlertStatus.RAISED)) {
				if (!alertGroupIDS.contains(alert.getGroupUid())) {
					boolean found = false;
					for (WebUIAlert webuiAlert : alerts) {
						if (webuiAlert.equals(alert)) {
							found = true;
							break;
						}
					}
					Assert.assertTrue(found);
					alertGroupIDS.add(alert.getGroupUid());
				}
			}
		}
		
		/* here we check that every resolved alert is associated with at least one raised alert */
		if (alerts.size() != 0) {
			for (int j = 0 ; j < alerts.size() ; j++) {
				if (alerts.get(j).getStatus().equals(AlertStatus.RESOLVED)) {
					Assert.assertTrue(alerts.get(j + 1).getStatus().equals(AlertStatus.RAISED));
					Assert.assertTrue(alerts.get(j + 1).getLocation().equals(alerts.get(j).getLocation()));
				}
			}
		}
	}
	
	/**
	 * retrieves alerts from the webui that fit a given AlertStatus
	 * @param status
	 * @return
	 */
	@SuppressWarnings("unused")
	public List<WebUIAlert> getAlertsByStatus(AlertStatus status) {
		
		Exception exception = null;
		WebElement alert;
		String xPath;
		int i = 1;
		
		List<WebUIAlert> alerts = new ArrayList<WebUIAlert>();
		
		while(exception == null) {
			try {
				xPath = WebConstants.Xpath.getPathToAlertByIndex(i);
				alert = driver.findElement(By.xpath(xPath));
				WebUIAlert webUIAlert = new WebUIAlert(xPath);
				if (webUIAlert.getStatus().equals(status)) {
					LogUtils.log("found alert : " + webUIAlert);
					alerts.add(webUIAlert);
					selenium.click(xPath + WebConstants.Xpath.pathToAlertExpansionButton);
				}
				Thread.sleep(1000);
				i++;
			}
			catch (Exception e) {
				exception = e;
			}
		}
		return alerts;
	}
	
	/**
	 * retrieves alerts from web-ui apart from the specified type.
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unused")
	public List<WebUIAlert> getAlertsByType(String type) {
		
		Exception exception = null;
		WebElement alert;
		String xPath;
		int i = 1;
		
		List<WebUIAlert> alerts = new ArrayList<WebUIAlert>();
		
		while(exception == null) {
			try {
				xPath = WebConstants.Xpath.getPathToAlertByIndex(i);
				alert = driver.findElement(By.xpath(xPath));
				WebUIAlert webUIAlert = new WebUIAlert(xPath);
				if (webUIAlert.getName().equals(type)) {
					LogUtils.log("found alert : " + webUIAlert);
					alerts.add(webUIAlert);
					selenium.click(xPath + WebConstants.Xpath.pathToAlertExpansionButton);
				}
				Thread.sleep(1000);
				i++;
			}
			catch (Exception e) {
				exception = e;
			}
		}
		return alerts;
	}
	/**
	 * return all alerts appart from the specified type
	 * pass null to retrieve all alerts
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unused")
	public List<WebUIAlert> getAlertsAppartFrom(String type) {
		
		Exception exception = null;
		WebElement alert;
		String xPath;
		int i = 1;
		
		List<WebUIAlert> alerts = new ArrayList<WebUIAlert>();
		
		while(exception == null) {
			try {
				xPath = WebConstants.Xpath.getPathToAlertByIndex(i);
				alert = driver.findElement(By.xpath(xPath));
				WebUIAlert webUIAlert = new WebUIAlert(xPath);
				if (!webUIAlert.getName().equals(type)) {
					LogUtils.log("found alert : " + webUIAlert);
					alerts.add(webUIAlert);
					selenium.click(xPath + WebConstants.Xpath.pathToAlertExpansionButton);
				}
				Thread.sleep(1000);
				i++;
			}
			catch (Exception e) {
				exception = e;
			}
		}
		return alerts;
	}
	
	/**
	 * retrieves only the parent alerts. thats is, only the ones shown in the webui without expanding 
	 * the view of an alert
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unused")
	public List<WebUIAlert> getParentAlertsAppartFrom(String type) {
		
		Exception exception = null;
		WebElement alert;
		String xPath;
		int i = 1;
		
		List<WebUIAlert> alerts = new ArrayList<WebUIAlert>();
		
		while(exception == null) {
			try {
				xPath = WebConstants.Xpath.getPathToAlertByIndex(i);
				alert = driver.findElement(By.xpath(xPath));
				WebUIAlert webUIAlert = new WebUIAlert(xPath);
				if (!webUIAlert.getName().equals(type)) {
					LogUtils.log("found alert : " + webUIAlert);
					alerts.add(webUIAlert);
				}
				Thread.sleep(1000);
				i++;
			}
			catch (Exception e) {
				exception = e;
			}
		}
		return alerts;
	}
	
	@SuppressWarnings("unused")
	public List<WebUIAlert> getParentAlertsByType(String type) {
		
		Exception exception = null;
		WebElement alert;
		String xPath;
		int i = 1;
		
		List<WebUIAlert> alerts = new ArrayList<WebUIAlert>();
		
		while(exception == null) {
			try {
				xPath = WebConstants.Xpath.getPathToAlertByIndex(i);
				alert = driver.findElement(By.xpath(xPath));
				WebUIAlert webUIAlert = new WebUIAlert(xPath);
				if (webUIAlert.getName().equals(type)) {
					LogUtils.log("found alert : " + webUIAlert);
					alerts.add(webUIAlert);
				}
				Thread.sleep(1000);
				i++;
			}
			catch (Exception e) {
				exception = e;
			}
		}
		return alerts;
	}
	
	/**
	 * sorts the alert in the ui by their status
	 */
	public void sortAlertsGridByStatus() {
		selenium.click(WebConstants.Xpath.pathToStatusColumnInAlertsGrid);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public class WebUIAlert {
		
		private AlertSeverity severity;
		private String name;
		private String description;
		private String location;
		private String xPath;
		private String lastUpdated;
		private AlertStatus status;
		
		public WebUIAlert(String xPath) {
			this.xPath = xPath;
			this.name = getTypeFromUI(xPath);
			this.description = getDescriptionFromUI(xPath);
			this.location = getLocationFromUI(xPath);
			this.severity = getSeverityFromUI(xPath);
			this.lastUpdated = getLastUpdatedFromUI(xPath);
			this.status = getStatusFromUI(xPath);	
		}
		
		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}	
		
		public AlertSeverity getSeverity() {
			return severity;
		}
		
		public String getLocation() {
			return location;
		}
		
		public AlertStatus getStatus() {
			return status;
		}
		
		public void closeParrentAlert() {
			selenium.click(xPath + WebConstants.Xpath.pathToAlertExpansionButton);
		}
		
		@Override
		public String toString() {
			return severity.toString() + " | " + name + " | " + description + " | " + location + " | " + lastUpdated;
			
		}
		
		/**
		 * only fields not containing specific measurements are compared, since reading may differ
		 * slightly from the test and the ui
		 * @param alert - an alert triggered from within the test
		 * @return 
		 */
		public boolean equals(Alert alert) {		
			if (!alert.getComponentDescription().equals(this.getLocation())) return false;
			if (!alert.getName().equals(this.getName())) return false;
			if (!alert.getSeverity().equals(this.getSeverity())) return false;
			if (!alert.getStatus().equals(this.getStatus())) return false;
			return true;		
		}
		
		public int getTimeStampInSecond() {
			String time[] = this.lastUpdated.split(" ")[3].split(":");
			int hours = Integer.parseInt(time[0]) * 3600;
			int minutes = Integer.parseInt(time[1]) * 60;
			int seconds = Integer.parseInt(time[2]);
			return hours + minutes + seconds;
		}
		
		private AlertSeverity getSeverityFromUI(String xpath) {
			String severity = selenium.getText(xpath + WebConstants.Xpath.pathToAlertSeverity);
			if (severity.equals("SEVERE")) return AlertSeverity.SEVERE;
			if (severity.equals("WARNING")) return AlertSeverity.WARNING;
			return AlertSeverity.INFO;
		}
		
		private String getTypeFromUI(String xpath) {
			return selenium.getText(xpath + WebConstants.Xpath.pathToAlertType);
		}
		
		private String getDescriptionFromUI(String xpath) {
			return selenium.getText(xpath + WebConstants.Xpath.pathToAlertDescription);
		}
		
		private String getLocationFromUI(String xpath) {
			return selenium.getText(xpath + WebConstants.Xpath.pathToAlertLocation);
		}
		
		private String getLastUpdatedFromUI(String xpath) {
			return selenium.getText(xpath + WebConstants.Xpath.pathToAlertLastUpdated);
		}
		
		private AlertStatus getStatusFromUI(String xpath) {
			WebElement icon = driver.findElement(By.xpath(xpath + WebConstants.Xpath.pathToAlertIcon));
			String iconType = icon.getAttribute("class").trim();
			if (iconType.equals(WebConstants.ID.okIcon)) return AlertStatus.RESOLVED;
			if (iconType.equals(WebConstants.ID.criticalIcon)) return AlertStatus.RAISED;
			if (iconType.equals(WebConstants.ID.warningIcon)) return AlertStatus.RAISED;
			if (iconType.equals(WebConstants.ID.naIcon)) return AlertStatus.NA;
			return null;
		}
	}
}
