import java.util.concurrent.TimeUnit


service {
	name "groovy"
	type "WEB_SERVER"
	elastic true
	numInstances 2
	maxAllowedInstances 2
	lifecycle { 
	
		init { println "This is the init event" }
		preInstall {println "This is the preInstall event" }
		postInstall {println "This is the postInstall event"}
		preStart {println "This is the preStart event" }
		
		start "run.groovy" 
		
		postStart {println "This is the postStart event" }
		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown {println "This is the shutdown event" }
		
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
				"echo" : {x ->
					return x
				},

				"contextInvoke": { x ->
					Object[] results =
							context.waitForService("groovy", 10, TimeUnit.SECONDS)
							.invoke("echo", x + " from " + context.instanceId )
					return java.util.Arrays.toString(results)
				}



			])
}