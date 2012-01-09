import test.data.Data
service {
	numInstances 2
	name "setter"
	icon "icon.png"
	type "WEB"
	
	lifecycle {
		
		start (["Win.*":"run.bat", "Linux":"run.sh"])
	}
	customCommands ([
	            "setApp" : {context.attributes.thisApplication.myKey = "myValue"},
				"setAppScript" : "set_application_context_property.groovy",
				"setInstance" : {context.attributes.thisInstance.myKey = "myValue"},
				"setInstanceScript" : "set_instance_context_property.groovy",
				"setService" : {context.attributes.thisService.myKey = "myValue"},
				"setService2" : {context.attributes.thisService.myKey2 = "myValue2"},
				"setServiceScript" : "set_service_context_property.groovy",
				"setAppCustom" : {x, y -> return(context.attributes.thisApplication[x] = y)},
                "getAppCustom" : {x -> return(context.attributes.thisApplication[x])},
				"setInstance1" : {context.attributes.thisService.instances[1].myKey = "myValue1"},
				"setInstance2" : {context.attributes.thisService.instances[2].myKey = "myValue2"},
				"setAppCustomPojo" : { 
					Data data = new Data(1)
					data.data = "data"
					context.attributes.thisApplication["myPojo"] = data},
				"setServiceDouble" : {context.attributes.thisService["myDouble"] = new Double(2)},
				"getInstance" : {context.attributes.thisInstance["myKey"]},
				"getApp" : {context.attributes.thisApplication.myKey},
				"getService" : {context.attributes.thisService.myKey},
				"getService2" : {context.attributes.thisService.myKey2},
				"iterateInstances" : {
                    result = ""
                    context.attributes.thisService.instances.each{result += it.myKey + ","}
                    return result},
                "setInstanceCustom1" : {x, y -> return(context.attributes.thisService.instances[1].x = y)},
                "setInstanceCustom2" : {x, y -> return(context.attributes.thisService.instances[2].x = y)},
                "getInstanceCustom1" : {x -> return(context.attributes.thisService.instances[1].x)},
                "getInstanceCustom2" : {x -> return(context.attributes.thisService.instances[2].x)},
                "getServiceByName" : {context.attributes.setter.myKey},
                "setServiceByName" : {context.attributes.setter.myKey = "myValueName"},
				"removeService" : {context.attributes.thisService.remove("myKey")},
				"cleanService" : {context.attributes.thisService.clean},
				"removeInstanceByServiceName" : {context.attributes.getter.instances[1].remove("myKey")},
				"cleanThisInstance" : {context.attributes.thisInstance.clear()},
				"cleanThisApp" : {context.attributes.thisApplication.clear()},
				
			])
}