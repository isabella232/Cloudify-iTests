import java.util.concurrent.TimeUnit

import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.Sigar;

import com.gigaspaces.internal.sigar.SigarHolder;


service {
	name "groovy"
	type "WEB_SERVER"
	elastic true
	numInstances 1
	maxAllowedInstances 2
	
	isolationSLA {
		global {
			instanceCpuCores 0
			instanceMemoryMB 128
			useManagement true
		}
	}
	
	lifecycle { 
		def device = "/dev/sdf"
		def path = "/teststorage"
		def fs = "ext4"
		
		init { println "This is the init event" }
		preInstall {println "This is the preInstall event" }
		postInstall {println "This is the postInstall event"}
		preStart {println "This is the preStart event" }
		
		start "run.groovy" 
		
		postStart {
			
			println "Creating a new storage volume"
			volumeId = context.storage.createVolume("SMALL_BLOCK")
			// adding instanceID to attributes store. (For cleanup purposes)
			println "adding instanceID to attributes store"
			context.attributes.thisInstance["DynamicVolID"] = volumeId
			println "Attaching volume to File System"
			context.storage.attachVolume(volumeId, device)
		}
		preStop 
		{
			println "Detaching volume with ID " + context.attributes.thisInstance["DynamicVolID"]
			context.storage.detachVolume(context.attributes.thisInstance["DynamicVolID"])
			println "Deleting volume with ID " + context.attributes.thisInstance["DynamicVolID"]
			context.storage.deleteVolume(context.attributes.thisInstance["DynamicVolID"])
		}
		
		postStop {println "This is the postStop event" }
		
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

	customCommands ([
		"formatVolume" : {device, fs -> context.storage.format(device, fs)},
		"mountVolume" : {device, path -> context.storage.mount(device, path)},
		"getDevicesSpaceState" : "getDevicesStateList.groovy"
	])
	
	compute {
		
		template "SMALL_LINUX"	
	}
	
	storage {
		template "SMALL_BLOCK"
	}
}