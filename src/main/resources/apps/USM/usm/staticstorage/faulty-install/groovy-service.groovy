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

		init { println "This is the init event" }
		preInstall {println "This is the preInstall event" }
		postInstall {
            throw new Exception("Failing installation on purpose")
        }
		preStart {println "This is the preStart event" }
		
		start "run.groovy" 

		preStop {println "This is the preStop event" }
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
	
	compute {
		
		template "ENTER_TEMPLATE"	
	}

    storage {

        template "SMALL_BLOCK"
    }
}