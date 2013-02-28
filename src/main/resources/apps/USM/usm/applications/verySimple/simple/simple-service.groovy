service {
	name "simple"
	icon "icon.png"
	type "UNDEFINED"
	
	lifecycle {
		init { println "This is the init event" }
		start { println "This is the start event" }
		shutdown { println "This is the shutdown event" }
	}
}