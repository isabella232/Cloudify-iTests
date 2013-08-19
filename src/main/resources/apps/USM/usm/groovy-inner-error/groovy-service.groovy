
import java.util.concurrent.TimeUnit


service {
	name "groovy"
	type "WEB_SERVER"
	elastic true
	numInstances 1	
	lifecycle { 
	
		init "error_start.groovy"
		preInstall {println "This is the preInstall event" }
		install { println "Installing..." }
		
		start "run.groovy" 
		
		
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

	
}