/***************
 * Cloud configuration file for the hp-grizzly cloud
 */
cloud {
    name = "hp-grizzly"

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

    /*************
     * Cloud authentication information
     */
    user {
        user "${tenant}:${user}"
        apiKey apiKey
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
                fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SCP
                username "ubuntu"
                options ([
                    _SKIP_EXTERNAL_NETWORKING_,
                    "keyPairName" : keyPair
                ])
                overrides ([
                    "openstack.endpoint": openstackUrl
                ])
                computeNetwork {
                    networks (["SOME_INTERNAL_NETWORK_1","SOME_INTERNAL_NETWORK_2"])
                }
            }
        ])
    }
}