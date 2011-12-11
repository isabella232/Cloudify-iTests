service {
	name "setter"
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
				shutdown {println "This is the shutdown event" }
			}
		
			
	customCommands ([
	            "cmd1" : {context.properties.application["myKey"] = "myValue"},
				"cmd2" : "set_application_context_property.groovy"
			])

}