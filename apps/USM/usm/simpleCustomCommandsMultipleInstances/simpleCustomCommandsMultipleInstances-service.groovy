import test.cli.cloudify.MyException;

service {
	numInstances 1
	name "simpleCustomCommandsMultipleInstances"
	type "UNDEFINED"
	
	lifecycle {	
		start (["Win.*":"run.bat", "Linux":"run.sh"])
	}

	customCommands ([
		"print" : {println "This is the print custom command"},
		"params" : {x, y -> return("this is the custom parameters command. expecting 123: "+1+x+y)},
		
		"exception" : { throw new MyException("This is an error test")},
		"runScript" : "add.groovy",
		"context" : {"Service Dir is: " + context.serviceDirectory}
	])
}