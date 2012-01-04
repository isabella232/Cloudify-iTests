package test.webui.recipes.services;

import java.io.IOException;

import org.junit.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.webui.objects.LoginPage;
import test.webui.objects.MainNavigation;
import test.webui.objects.topology.Metric;
import test.webui.objects.topology.MetricType;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.applicationmap.ApplicationMap;
import test.webui.objects.topology.applicationmap.ApplicationNode;
import test.webui.objects.topology.healthpanel.HealthPanel;
import test.webui.resources.WebConstants;

public class UsmPuServiceMonitorsInsteadOfDetailsTest extends AbstractSeleniumServiceRecipeTest {
	
	@Override
	@BeforeMethod
	public void install() throws IOException, InterruptedException {
		setBrowser(WebConstants.CHROME);
		setCurrentRecipe("activemq");
		super.install();
	}
	
	@Test
	public void testServiceMonitors() throws InterruptedException {
		
		boolean shouldFail = false;
		String failureMessage = "";
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		MainNavigation mainNav = loginPage.login();
		
		TopologyTab topologyTab = mainNav.switchToTopology();
		
		ApplicationMap appMap = topologyTab.getApplicationMap();
			
		appMap.selectApplication("default");
		
		ApplicationNode activemq = appMap.getApplicationNode("activemq");
		
		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();
		
		activemq.select();
		
		Metric storePercentUsage = healthPanel.getMetric("Store Percent Usage");
		
		try { 
			storePercentUsage.swithToMetric(new MetricType("Misc.", "Child Process ID"));
			shouldFail = true;
			failureMessage += "Child Process ID should not be a part of the metric selection menu\n";
		}
		catch (AssertionError err) {
			
		}
		try { 
			storePercentUsage.swithToMetric(new MetricType("Misc.", "Actual Process ID"));
			shouldFail = true;
			failureMessage += "Actual Process ID should not be a part of the metric selection menu\n";
		}
		catch (AssertionError err) {
			
		}
		try { 
			storePercentUsage.swithToMetric(new MetricType("Misc.", "USM_State"));
			shouldFail = true;
			failureMessage += "USM_State should not be a part of the metric selection menu\n";
		}
		catch (AssertionError err) {
			
		}
		if (shouldFail) {
			Assert.fail(failureMessage);
		}
	}
}
