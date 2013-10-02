import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.Admin;
import java.util.concurrent.TimeUnit;

service {
	numInstances 1
	name "simpleRestartAgent"
	type "UNDEFINED"
	
	lifecycle {	
		start (["Win.*":"run.bat", "Linux":"run.sh"])
	}

	customCommands ([
		"restartWindows" : "restartScript.groovy",
		"restartLinux" : "restart_linux.sh",
		"restart" : "restart_java_groovy.groovy",
		"getOpSystem" : {return System.getProperty("os.name")},
		"shutdownAgent" : {processingUnitName ->
			Admin admin = context.getAdmin();
			ProcessingUnit serviceProcessingUnit = admin.getProcessingUnits().waitFor(processingUnitName, 1, TimeUnit.MINUTES);
			serviceProcessingUnit.waitFor(1);
			GridServiceAgent gridServiceAgent = serviceProcessingUnit.getInstances()[0].getMachine().getGridServiceAgent();
			gridServiceAgent.shutdown();
		},
		"startMaintenanceModeLong" : {context.startMaintenanceMode(10l, TimeUnit.MINUTES)},
		"startMaintenanceModeShort" : {context.startMaintenanceMode(1l, TimeUnit.SECONDS)},
		"startMaintenanceMode" : {x -> context.startMaintenanceMode(Long.parseLong(x, 10), TimeUnit.SECONDS)},
		"stopMaintenanceMode" : {context.stopMaintenanceMode()}
	])
}