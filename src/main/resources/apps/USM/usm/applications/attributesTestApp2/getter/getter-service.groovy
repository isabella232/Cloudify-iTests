import test.data.Data
service {
	numInstances 1
	maxAllowedInstances 1
	name "getter"
	type "UNDEFINED"
	
	lifecycle {
		
		start (["Win.*":"run.bat", "Linux":"run.sh"])
	}
	customCommands ([
			"getGlobalCustom" : {x-> return(context.attributes.global[x])}
		])
}