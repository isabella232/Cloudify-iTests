service {
	numInstances 2
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
	            "getApp" : {context.attributes.thisApplication["myKey"]},
				"getAppScript" : "get_application_context_property.groovy",
				"getInstance" : {context.attributes.thisinstance["myKey"]},
				"getInstanceScript" : "get_instance_context_property.groovy",
				"getService" : {context.attributes.thisService["myKey"]},
				"getServiceScript" : "get_service_context_property.groovy",
				"getAppCustom" : {context.attributes.thisService[x]
			])
}