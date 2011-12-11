service {
	name "getter"
	icon "icon.png"
	type "WEB"
	lifecycle {
		
				//init { println "This is the init event" }
		
				preInstall {println "This is the preInstall event" }
				postInstall {println "This is the postInstall event"}
				
				// preStart {println "This is the preStart event" }
		
				start (["Win.*":"run.bat", "Linux":"run.sh"])
		
				postStart {println "This is the postStart event" }
		
				preStop {println "This is the preStop event" }
				postStop {println "This is the postStop event" }
				
			}
		
			
	customCommands ([
	            "cmd1" : {println context.properties.application["myKey"]},
				"cmd2" : "get_application_context_property.groovy",
				"cmd3" : {println context.properties.instance["myKey"]},
				"cmd4" : "get_instance_context_property.groovy",
				"cmd5" : {println context.properties.service["myKey"]},
				"cmd6" : "get_service_context_property.groovy",
			])

}