
service {
	numInstances 4
	name "simpleCustomCommandsMultipleInstances"

	
	lifecycle {	
		start (["Win.*":"run.bat", "Linux":"run.sh"])
	}

	customCommands ([
		"print" : {println "This is the print custom command"},
		"params" : {"this is the custom parameters command. expecting 123: "+1+x+y},
		"exception" : { throw new Exception("This is an error test")},
		"runScript" : "add.groovy",
		"context" : {"Service Dir is: " + context.serviceDirectory}
	])
}