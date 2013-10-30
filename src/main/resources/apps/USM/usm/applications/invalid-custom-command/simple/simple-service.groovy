service {
	name "simple"
	icon "icon.png"
	type "UNDEFINED"
	
	lifecycle {
		init { println "This is the init event" }
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
		"prifnt" : {println "This is the print custom command"},
		"cloudify:" : {x, y -> return("this is the custom parameters command. expecting 123: "+1+x+y)},
	])
	
}