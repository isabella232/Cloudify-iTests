service {
	numInstances 2
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
	            "setApp" : {context.properties.application["myKey"] = "myValue"},
				"setAppScript" : "set_application_context_property.groovy",
				"setInstance" : {context.properties.instance["myKey"] = "myValue"},
				"setInstanceScript" : "set_instance_context_property.groovy",
				"setService" : {context.properties.service["myKey"] = "myValue"},
				"setServiceScript" : "set_service_context_property.groovy",
				"setAppCustom" : {context.properties.application[x] = y,
				"getInstance" : {context.properties.instance["myKey"]},
				"getService" : {context.properties.service["myKey"]}
			])

}