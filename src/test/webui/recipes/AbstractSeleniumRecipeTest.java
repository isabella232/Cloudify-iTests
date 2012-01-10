package test.webui.recipes;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;

import test.webui.AbstractSeleniumTest;
import framework.utils.LogUtils;

public class AbstractSeleniumRecipeTest extends AbstractSeleniumTest {
	
	public void undeployNonManagementServices() {
		for (ProcessingUnit pu : admin.getProcessingUnits().getProcessingUnits()) {
			if (!pu.getName().equals("webui") && !pu.getName().equals("rest") && !pu.getName().equals("cloudifyManagementSpace")) {
				if (!pu.undeployAndWait(30, TimeUnit.SECONDS)) {
					LogUtils.log("Failed to uninstall " + pu.getName());
				}
				else {
					LogUtils.log("Uninstalled service: " + pu.getName());
				}
			}
		}
	}
	
	public boolean isBootstraped() {
		return bootstraped;
	}
}
