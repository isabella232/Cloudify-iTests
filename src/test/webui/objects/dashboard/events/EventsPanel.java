package test.webui.objects.dashboard.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import test.webui.resources.WebConstants;
import framework.utils.AssertUtils;
import framework.utils.LogUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.WebUiUtils;

public class EventsPanel {

	private WebDriver driver;
	private List<WebUIAdminEvent> visibleEvents;
	
	public EventsPanel(WebDriver driver) {
		this.driver = driver;
	}
	
	public WebUIAdminEvent getEvent(int index) {
		
		List<WebUIAdminEvent> events = new ArrayList<WebUIAdminEvent>();

		List<WebElement> eventIcons = driver.findElements(By.className(WebConstants.ClassNames.timelineEventIcon));
		for (WebElement icon : eventIcons) {
			try {
				icon.click();
				WebElement bubble = WebUiUtils.waitForElement(driver, By.className(WebConstants.ClassNames.timelineEventContainer), 1);
				String title = bubble.findElement(By.className(WebConstants.ClassNames.timelineEventTitle)).getText();
				String description = bubble.findElement(By.className(WebConstants.ClassNames.timelineEventBody)).getText();
				long time = Long.valueOf(icon.getAttribute("timestamp"));
				events.add(new WebUIAdminEvent(title, description , time));
				WebElement close = driver.findElement(By.className(WebConstants.ClassNames.eventPopupCloseButton));
				close.click();
			}
			catch (WebDriverException e) {
				long time = Long.valueOf(icon.getAttribute("timestamp"));
				events.add(new WebUIAdminEvent(null, null, time)); // dummy event just for the sake of sorting
			}
		}
		
		sortByTimeStamp(events);
		visibleEvents = events;
		return events.get(index - 1);
	}
	
	private void sortByTimeStamp(List<WebUIAdminEvent> visible) {
		
		WebUIAdminEvent[] array = new WebUIAdminEvent[visible.size()];
		visible.toArray(array);
		
		for (int i = 0 ; i < visible.size() ; i++) {
			Arrays.sort(array, new Comparator<WebUIAdminEvent>() {

				@Override
				public int compare(WebUIAdminEvent arg0, WebUIAdminEvent arg1) {
					
					long mili0 = arg0.getTime();
					long mili1 = arg1.getTime();
					if (mili0 == mili1) return 0;
					if (mili0 < mili1) return -1;
					return 1;
				}
			});
		}

		visible.clear();
		for (WebUIAdminEvent e : array) {
			visible.add(e);
		}
	}

	public void waitFor(final int numberOfEvents, int timeoutInMillis) {

		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				List<WebElement> eventIcons = driver.findElements(By.className(WebConstants.ClassNames.timelineEventIcon));
				return eventIcons.size() == numberOfEvents;
			}
		};
		AssertUtils.repetitiveAssertTrue("could not find " + numberOfEvents + " event after " + timeoutInMillis, condition, timeoutInMillis);

	}

	public List<WebUIAdminEvent> getVisibleEvents() {

		List<WebUIAdminEvent> result = new ArrayList<WebUIAdminEvent>();

		List<WebElement> eventIcons = driver.findElements(By.className(WebConstants.ClassNames.timelineEventIcon));
		for (WebElement icon : eventIcons) {
			try {
				icon.click();
				WebElement bubble = WebUiUtils.waitForElement(driver, By.className(WebConstants.ClassNames.timelineEventContainer), 1);
				String title = bubble.findElement(By.className(WebConstants.ClassNames.timelineEventTitle)).getText();
				String description = bubble.findElement(By.className(WebConstants.ClassNames.timelineEventBody)).getText();
				long time = Long.valueOf(icon.getAttribute("timestamp"));
				result.add(new WebUIAdminEvent(title, description , time));
				WebElement close = driver.findElement(By.className(WebConstants.ClassNames.eventPopupCloseButton));
				close.click();
			}
			catch (WebDriverException e) {		
			}
		}
		return result;
	}

	public void printAllEvents() {
		
		List<WebUIAdminEvent> events = null;
		if (visibleEvents == null) {
			events = getVisibleEvents();
		}
		else {
			events = visibleEvents;
		}
		LogUtils.log("Here are all the visible events sorted by time");
		
		for (WebUIAdminEvent event : events) {
			LogUtils.log(event.toString());
		}
	}
}
