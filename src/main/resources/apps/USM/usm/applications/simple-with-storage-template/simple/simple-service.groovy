service {
	name "simple"
	icon "icon.png"
	type "UNDEFINED"
	elastic true
	
	compute {
		template "TEMPLATE_1"
	}
	
	storage {
		template "STORAGE_TEMPLATE"
	}
	
	lifecycle {
		init { println "This is the init event" }
		preInstall {println "This is the preInstall event" }
		postInstall {println "This is the postInstall event"}	
		preStart {println "This is the preStart event" }
		start (["Win.*":"run.bat", "Linux":"run.sh"])
		postStart {println "This is the postStart event" }
		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown {println "This is the shutdown event" }
	}
	
}