
service {
    name "floatingips"
    elastic true
    numInstances 1
    maxAllowedInstances 2

    lifecycle {
        startDetectionTimeoutSecs 900
        startDetection { true }
        stopDetection { false }
        locator { NO_PROCESS_LOCATORS }
        init { context.attributes.thisInstance["floatingIp"] = context.getPublicAddress() }
    }
    
    customCommands ([
        "getMachineIp" : { return context.getPrivateAddress() },
        "getPublicAddress" : { return context.getPublicAddress() },
        "getFloatingIp" : { return context.attributes.thisInstance["floatingIp"] },
        "getApplicationNetworkIp" : { return System.getenv()['CLOUDIFY_APPLICATION_NETWORK_IP'] },
        "assignFloatingIp" : {
            println "allocating/assigning new floating ip"
            org.cloudifysource.domain.context.network.NetworkFacade network = context.getNetwork()
            // TODO Should get the pool name dynamically (net-ext)
            def floatingIp = network.allocateFloatingIP("network_ext", null)
            println "successfully allocated floating ip ${floatingIp}"
            try {
                def machineIp = context.getPrivateAddress()
                network.assignFloatingIP(machineIp, floatingIp, null)
                context.attributes.thisInstance["floatingIp"] = "${floatingIp}"
                println "successfully assigned floating ip ${floatingIp} to ${machineIp}"
                return floatingIp
            } catch(e) {
                println "couldn't assign floating ip, release ${floatingIp} from the pool."
                network.releaseFloatingIP(floatingIp, null)
                return context.attributes.thisInstance["floatingIp"]
            }
        },
        "releaseFloatingIp" : {
            org.cloudifysource.domain.context.network.NetworkFacade network = context.getNetwork()
            def floatingIp = context.attributes.thisInstance["floatingIp"]
            def machineIp = context.getPrivateAddress()
            println "releasing existing floating ip ${floatingIp}"
            network.unassignFloatingIP(machineIp, floatingIp, null)
            println "successfully unassigned floating ip ${floatingIp} from machine ${machineIp}"
            network.releaseFloatingIP(floatingIp, null)
            println "successfully released floating ip ${floatingIp} from machine ${machineIp}"
            context.attributes.thisInstance["floatingIp"] = null
            return floatingIp
        }
        
    ])
    
    network {
        template "APPLICATION_NET"
        accessRules {
            incoming = ([
                accessRule {
                    portRange "22"
                    type "PUBLIC"
                }
            ])
        }
    }
}

