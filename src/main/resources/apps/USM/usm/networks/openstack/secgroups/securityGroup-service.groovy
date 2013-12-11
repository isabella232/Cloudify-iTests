
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
        template "APPLICATION_NET"
        accessRules {
            incoming = ([
                accessRule {
                    portRange "22"
                    type "PUBLIC"
                },
                accessRule {
                    portRange "80-90"
                    type "SERVICE"
                },
                accessRule {
                    portRange "3306"
                    type "CLUSTER"
                },
                accessRule {
                    portRange "3307"
                    type "APPLICATION"
                },
                accessRule {
                    portRange "1234"
                    type "GROUP"
                    target "default"
                },
                accessRule {
                    portRange "8080"
                    type "RANGE"
                    target "160.0.0.4/32"
                }
            ])
        }
    }
}

