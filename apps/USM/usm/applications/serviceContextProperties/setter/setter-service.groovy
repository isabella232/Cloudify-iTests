//import com.gigaspaces.cloudify.esc.examples.data;
service {
	numInstances 2
	name "setter"
	icon "icon.png"
	type "WEB"
	
	lifecycle {
		
		start (["Win.*":"run.bat", "Linux":"run.sh"])
	}
	customCommands ([
	            "setApp" : {context.attributes.thisApplication["myKey"] = "myValue"},
				"setAppScript" : "set_application_context_property.groovy",
				"setInstance" : {context.attributes.thisInstance["myKey"] = "myValue"},
				"setInstanceScript" : "set_instance_context_property.groovy",
				"setService" : {context.attributes.thisService["myKey"] = "myValue"},
				"setServiceScript" : "set_service_context_property.groovy",
				"setAppCustom" : {context.attributes.thisApplication[x] = y},
				"setInstance1" : {context.attributes.thisService.instances[1]["myKey"] = "myValue1"},
				"setInstance2" : {context.attributes.thisService.instances[2]["myKey"] = "myValue2"},
//				"setServiceCustomPojo" : {context.attributes.thisService["myPojo"] = new data(1 , 2 ,null)},
				"setServiceDouble" : {context.attributes.thisService["myDouble"] = new Double(2)},
				"getInstance" : {context.attributes.thisInstance["myKey"]},
				"getApp" : {context.attributes.thisApplication["myKey"]},
				"getService" : {context.attributes.thisService["myKey"]},
				"iterateInstances" : {context.attributes.thisService.instances.each{println it.myKey}}
			])
}