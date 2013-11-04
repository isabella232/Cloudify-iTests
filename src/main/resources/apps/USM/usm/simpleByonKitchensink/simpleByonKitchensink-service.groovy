import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.Admin;
import java.util.concurrent.TimeUnit;

service {
	numInstances 1
	name "simpleByonKitchensink"
	type "UNDEFINED"
	
	lifecycle {	
		start (["Win.*":"run.bat", "Linux":"run.sh"])
	}

	customCommands ([
		"listRemoteFiles" : "list_remote_files.sh"
	])
}