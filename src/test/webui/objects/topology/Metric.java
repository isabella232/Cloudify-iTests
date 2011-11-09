package test.webui.objects.topology;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

public class Metric {
	
	WebElement metric;
	String type;

	public Metric(WebElement metric, String type) {
		this.metric = metric;
		this.type = type;
	}

	public Metric(WebElement metric) {
		this.metric = metric;
	}

	public boolean isDisplayed() {
		RemoteWebElement node = (RemoteWebElement) metric;
		return node.isDisplayed();
	}


}
