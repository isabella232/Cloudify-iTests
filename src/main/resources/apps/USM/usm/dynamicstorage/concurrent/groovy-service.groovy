import java.util.concurrent.TimeUnit

import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.Sigar;

import com.gigaspaces.internal.sigar.SigarHolder;


service {
	name "groovy"
	type "WEB_SERVER"
	elastic true
	numInstances 2
	maxAllowedInstances 2
	
	isolationSLA {
		global {
			instanceCpuCores 0
			instanceMemoryMB 128
			useManagement true
		}
	}
	
	lifecycle { 
		
		def volumeId;
		def device = "/dev/sdc"
		def path = "/teststorage"
		def fs = "ext4"
		
		// this will assure different instances will use different templates
		def templateName = "INSTANCE_" + context.getInstanceId()
	
		init { println "This is the init event" }
		preInstall {println "This is the preInstall event" }
		postInstall {println "This is the postInstall event"}
		preStart {println "This is the preStart event" }
		
		start "run.groovy" 
		
		postStart {
			
			println "Creating a new storage volume"
			volumeId = context.storage.createVolume(templateName)
			println "Attaching volume to File System"
			context.storage.attachVolume(volumeId, device)
			println "Formating volume"
			context.storage.format(device, fs)
			println "Mounting volume"
			context.storage.mount(device, path)
	
		}
		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown {
			
			println "Unmounting volume"
			context.storage.unmount(device)
			println "Detaching volume with id ${volumeId} from machine."
			context.storage.detachVolume(volumeId) 
			println "Deleting volume with id ${volumeId}"
			context.storage.deleteVolume(volumeId);
		}
		
		startDetection {
			new File(context.serviceDirectory + "/marker.txt").exists()
		}
		
		locator {
			println "Sleeping for 5 secs"
			sleep(5000)
			def query = "Exe.Cwd.eq=" + context.serviceDirectory+",Args.*.eq=org.codehaus.groovy.tools.GroovyStarter"
			println "qeury is: " + query
			def pids = ServiceUtils.ProcessUtils.getPidsWithQuery(query)
			
			println "LOCATORS GOT: " + pids
			return pids;
		}
	}
	
	compute {
		
		template "ENTER_TEMPLATE"	
	}	
}