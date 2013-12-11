/***************
 * Cloud configuration file for the Openstack cloud. *
 * This configuration doesn't contain any network template (nor cloudNetwork/computeTemplate)
 */
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