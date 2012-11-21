service {
	name "C"
	type "WEB_SERVER"
	
	lifecycle {


		startDetection {
			Thread.sleep(5000)
			return true
		}
		preInstall {println "This is the preInstall event" }
		postInstall {println "This is the postInstall event"}
		preStart {println "This is the preStart event" }
		start "Start.groovy"
		postStart {println "This is the postStart event" }

		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown {println "This is the shutdown event" }
	}

		
}