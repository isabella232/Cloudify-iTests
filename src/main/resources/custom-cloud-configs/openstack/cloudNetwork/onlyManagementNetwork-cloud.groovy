cloud {
    name = "openstack"

    configuration {
        managementMachineTemplate "LINUX"
    }
    provider {
        provider "openstack-nova"
        machineNamePrefix "cloudify-agent-"
        managementOnlyFiles ([])
        managementGroup "cloudify-manager-"
        numberOfManagementMachines 1
        reservedMemoryCapacityPerMachineInMB 1024
    }
    user {
        user "${tenant}:${user}"
        apiKey apiKey
    }
    cloudNetwork {
        management {
            networkConfiguration {
                name "Cloudify-Management-Network"
                subnets ([
                    subnet {
                        name "Cloudify-Management-Subnet1"
                        range "177.70.0.0/24"
                        options ([ "gateway" : "177.70.0.1" ])
                    }
                ])
                custom ([ "associateFloatingIpOnBootstrap" : "true" ])
            }
        }
    }
    cloudCompute {
        templates ([
            LINUX : computeTemplate{
                imageId imageId
                remoteDirectory remoteDirectory
                machineMemoryMB 1600
                hardwareId hardwareId
                localDirectory "upload"
                keyFile keyFile
                username "ubuntu"
                options ([
                    _SKIP_EXTERNAL_NETWORKING_,
                    "keyPairName" : keyPair
                ])
                overrides ([
                    "openstack.endpoint": openstackUrl
                ])
            }
        ])
    }
}