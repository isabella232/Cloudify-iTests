[
				UBUNTU_TEST : computeTemplate{
					// Mandatory. Image ID.
					imageId ubuntuImageId

					// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
					remoteDirectory "/home/ubuntu/gs-files"
					// Mandatory. Amount of RAM available to machine.
					machineMemoryMB 1600
					// Mandatory. Hardware ID.
					hardwareId hardwareId
					// Optional. Location ID.
					locationId locationId
					// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
					localDirectory "ubuntu_upload"
					// Optional. Name of key file to use for authenticating to the remot machine. Remove this line if key files
					// are not used.
					keyFile keyFile

					username "ubuntu"
					// Additional template options.
					// When used with the default driver, the option names are considered
					// method names invoked on the TemplateOptions object with the value as the parameter.
					options ([
								"securityGroups" : ["default"]as String[],
								"keyPair" : keyPair
							])

					// Optional. Overrides to default cloud driver behavior.
					// When used with the default driver, maps to the overrides properties passed to the ComputeServiceContext a
					overrides (["jclouds.ec2.ami-query":"",
								"jclouds.ec2.cc-ami-query":""])

					// enable sudo.
					privileged true
				}
	]