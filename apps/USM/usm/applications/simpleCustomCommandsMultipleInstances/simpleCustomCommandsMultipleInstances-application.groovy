
application {
	
	name="simpleCustomCommandsMultipleInstances"
	
	service {
		name = "simpleCustomCommandsMultipleInstances-1"	
	}
	
	service {
		name = "simpleCustomCommandsMultipleInstances-2"
		dependsOn = ["simpleCustomCommandsMultipleInstances-1"]
	}
	
}