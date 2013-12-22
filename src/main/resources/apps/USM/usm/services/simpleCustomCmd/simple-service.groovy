service {
	name "simple"
	type "UNDEFINED"
	elastic true
	
	lifecycle {

		preInstall {println "This is the preInstall event" }
		postInstall {println "This is the postInstall event"}
		preStart {println "This is the preStart event" }
		start {println "This is the start event" }
		postStart {println "This is the postStart event" }
		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown {println "This is the shutdown event" }

	}

	
	customCommands ([
		"init" : "init.groovy",
	])

	
}