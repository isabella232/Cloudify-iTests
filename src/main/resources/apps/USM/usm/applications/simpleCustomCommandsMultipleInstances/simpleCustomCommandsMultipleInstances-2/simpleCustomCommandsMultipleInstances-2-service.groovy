
service {
	numInstances 2
	maxAllowedInstances 2
	name "simpleCustomCommandsMultipleInstances-2"
	type "UNDEFINED"
	
	lifecycle {	
		start "run.groovy"
		startDetection {
			println "running startDetector"
			new File(context.serviceDirectory + "/marker.log").exists()
		}
	}

	
	customCommands ([
		"print" : {println "This is the print custom command"},
		"params" : {x, y -> return("this is the custom parameters command. expecting 123: "+1+x+y)},
		"exception" : { throw new Exception("This is an error test")},
		"runScript" : "add.groovy",
		"context" : {"Service Dir is: " + context.serviceDirectory}
	])
}