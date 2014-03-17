import java.util.concurrent.TimeUnit

import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.Sigar;

import com.gigaspaces.internal.sigar.SigarHolder;


service {
	name "groovy"
	type "WEB_SERVER"
	elastic true
	numInstances 1
	minAllowedInstances 1
	maxAllowedInstances 2
	locationAware true
	minAllowedInstancesPerLocation 1
	maxAllowedInstancesPerLocation 10
	
	lifecycle { 
		
		def volumeId = null;
		def device = "/dev/vdc"
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
			println "Created volume with ID: " + volumeId
			println "Attaching volume to File System"
			context.storage.attachVolume(volumeId, device)
			// Store the volume id
			context.attributes.thisInstance["volumes"] = volumeId
			
			//skipping format and mount - not required for this test	
		}
		
		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown "shutdown.groovy"
		
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

	network	 {
		template "APPLICATION_NET"
	}
}