import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.internal.context.ServiceContextImpl;

service {
	name "simple"
	icon "${iconName}.png"
	type "UNDEFINED"

  
	lifecycle {
		init {Thread.sleep(70000)}
		start {println "This is the shutdown event"}
		shutdown {println "This is the shutdown event"}
		
	}
	
	customCommands ([
		"sleep" : {def service = context.waitForService("simple", 10, TimeUnit.SECONDS)
					service.invoke("sleep", null)}
	])
	
}