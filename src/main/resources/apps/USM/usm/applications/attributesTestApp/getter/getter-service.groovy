import test.data.Data
service {
	numInstances 2
	maxAllowedInstances 2
	name "getter"
	type "UNDEFINED"
	
	lifecycle {
		
		start (["Win.*":"run.bat", "Linux":"run.sh"])
	}
	customCommands ([
            "getApp" : {context.attributes.thisApplication["myKey"]},
			"getGlobal" : {context.attributes.global["myKey"]},
			"getAppScript" : "get_application_context_property.groovy",
			"getInstance" : {context.attributes.thisInstance["myKey"]},
			"getInstanceScript" : "get_instance_context_property.groovy",
			"getService" : {context.attributes.thisService["myKey"]},
			"getServiceScript" : "get_service_context_property.groovy",
			"getAppCustomPojo" : {context.attributes.thisApplication["myPojo"]},
			"getAppCustom" : {x-> return(context.attributes.thisApplication[x])},
			"getGlobalCustom" : {x-> return(context.attributes.global[x])},
			"cleanService" : {context.attributes.thisService.clear()},
			"setService" : {context.attributes.thisService["myKey"] = "myValue"},
			"cleanThisApp" : {context.attributes.thisApplication.clear()},
			"cleanGlobal" : {context.attributes.global.clear()}
		])
}