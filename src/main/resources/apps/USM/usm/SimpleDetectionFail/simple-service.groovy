service {
	name "simple"
	type "WEB_SERVER"
	
	lifecycle {
		startDetection "startDetection.groovy"

		start (["Win.*":"run.bat", "Linux":"run.sh"])

		postStart {println "This is the postStart event" }

	}

	compute {
		template "SMALL_LINUX"
	}
	
}