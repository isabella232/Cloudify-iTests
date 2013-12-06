
service {
	
    name "securityGroup"
    elastic true
    numInstances 1
    maxAllowedInstances 2

    lifecycle {
        startDetectionTimeoutSecs 900
        startDetection { true }
        stopDetection { false }
        locator { NO_PROCESS_LOCATORS }
        
        init { println "This is the init event" }
        preInstall {println "This is the preInstall event" }
        postInstall {println "This is the postInstall event"}
        preStart {println "This is the preStart event" }
        postStart {println "This is the postStart event" }
        preStop {println "This is the preStop event" }
        postStop {println "This is the postStop event" }
        shutdown {println "This is the shutdown event" }
    }

    network {
		template "NOT_EXISTING_NETWORK_TEMPLATE"  
    }
}

