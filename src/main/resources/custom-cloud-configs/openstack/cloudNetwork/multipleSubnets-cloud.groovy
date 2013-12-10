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
                    },
                    subnet {
                        name "Cloudify-Management-Subnet2"
                        range "177.80.0.0/24"
                        options ([ "gateway" : "null" ])
                    }
                ])
                custom ([ "associateFloatingIpOnBootstrap" : "true" ])
            }
        }
        templates ([
            "APPLICATION_NET" : networkConfiguration {
                name "Cloudify-Application-Network"
                subnets {
                    subnet {
                        name "Cloudify-Application-Subnet1"
                        range "160.1.0.0/24"
                    }
                    subnet {
                        name "Cloudify-Application-Subnet2"
                        range "160.2.0.0/24"
                    }
                }
                custom ([ "associateFloatingIpOnBootstrap" : "true" ])
            },
            "APPLICATION_NET2" : networkConfiguration {
                name "Cloudify-Application-Network2"
                subnets {
                    subnet {
                        name "Cloudify-Application-Subnet1"
                        range "162.1.0.0/24"
                    }
                    subnet {
                        name "Cloudify-Application-Subnet2"
                        range "162.2.0.0/24"
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