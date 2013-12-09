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
                        name "Cloudify-Management-Subnet"
                        range "177.86.0.0/24"
                        options ([ "gateway" : "177.86.0.111" ])
                    }
                ])
                custom ([ "associateFloatingIpOnBootstrap" : "true" ])
            }
        }
        templates ([
            "APPLICATION_NET" : networkConfiguration {
                name  "Cloudify-Application-Network"
                subnets {
                    subnet {
                        name "Cloudify-Application-Subnet"
                        range "160.0.0.0/24"
                        options {
                            gateway "null"
                        }
                    }
                }
                custom ([ "associateFloatingIpOnBootstrap" : "true" ])
            }
        ])
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
                    _COMPUTE_SERVICE_NAME_,
                    _NETWORK_SERVICE_NAME_,
                    _SKIP_EXTERNAL_NETWORKING_,
                    "keyPairName" : keyPair
                ])
                overrides ([
                    "jclouds.endpoint": openstackUrl
                ])
            }
        ])
    }
}