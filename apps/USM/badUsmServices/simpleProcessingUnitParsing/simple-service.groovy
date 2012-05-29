service {
	name "simple"
	icon "gigaspaces_logo.gif"
	type "WEB_SERVER"
	numInstances 1	
	
	dataGrid {

		sla {
				memoryCapacity 128
				maxMemoryCapacity 256
				highlyAvailable false
				memoryCapacityPerContainer 128 
		}

		contextProperties ([
				//this is the usual deployment properties mechanism 
				"cluster-config.mirror-service.interval-opers":"1000"
		])
	}
	
	lifecycle {

		preInstall "error.groovy" 
		postInstall {println "This is the postInstall event"}
		preStart {println "This is the preStart event" }

		start (["Win.*":"run.bat", "Linux":"run.sh", "Mac.*":"run.sh"])


		postStart {println "This is the postStart event" }

		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown {println "This is the shutdown event" }
	}

	
}