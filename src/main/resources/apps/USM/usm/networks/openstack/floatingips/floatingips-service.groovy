
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
        "getMachineId" : { return context.getMachineID() },
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
                def machineId= context.getMachineID()
                network.assignFloatingIP(machineId, floatingIp, null)
                context.attributes.thisInstance["floatingIp"] = "${floatingIp}"
                println "successfully assigned floating ip ${floatingIp} to ${machineId}"
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
            def machineId= context.getMachineID()
            println "releasing existing floating ip ${floatingIp}"
            network.unassignFloatingIP(machineId, floatingIp, null)
            println "successfully unassigned floating ip ${floatingIp} from machine ${machineId}"
            network.releaseFloatingIP(floatingIp, null)
            println "successfully released floating ip ${floatingIp} from machine ${machineId}"
            context.attributes.thisInstance["floatingIp"] = null
            return floatingIp
        }
        
    ])
    
    network {
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

