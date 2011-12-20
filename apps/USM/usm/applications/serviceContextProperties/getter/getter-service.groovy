//import com.gigaspaces.cloudify.esc.examples;
service {
//	numInstances 2
	name "getter"
	icon "icon.png"
	type "WEB"
	
	lifecycle {
		
		start (["Win.*":"run.bat", "Linux":"run.sh"])
	}
		customCommands ([
	            "getApp" : {context.attributes.thisApplication["myKey"]},
				"getAppScript" : "get_application_context_property.groovy",
				"getInstance" : {context.attributes.thisinstance["myKey"]},
				"getInstanceScript" : "get_instance_context_property.groovy",
				"getService" : {context.attributes.thisService["myKey"]},
				"getServiceScript" : "get_service_context_property.groovy",
//				"getServiceCustomPojo" : {context.attributes.thisService["myPojo"]},
				"getServiceDouble" : {context.attributes.thisService["myDouble"]},
				"getAppCustom" : {context.attributes.thisApplication[x]},
				"setService" : {context.attributes.thisErvice["myKey"] = "myValue"}
			])
}