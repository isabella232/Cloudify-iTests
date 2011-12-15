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
	            "setApp" : {context.attributes.thisApplication["myKey"] = "myValue"},
				"setAppScript" : "set_application_context_property.groovy",
				"setInstance" : {context.attributes.thisInstance["myKey"] = "myValue"},
				"setInstanceScript" : "set_instance_context_property.groovy",
				"setService" : {context.attributes.thisService["myKey"] = "myValue"},
				"setServiceScript" : "set_service_context_property.groovy",
				"setAppCustom" : {context.attributes.thisApplication[x] = y,
				"getInstance" : {context.attributes.thisInstance["myKey"]},
				"getService" : {context.attributes.thisService["myKey"]}
			])

}