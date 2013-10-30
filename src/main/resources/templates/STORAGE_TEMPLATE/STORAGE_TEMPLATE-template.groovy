[
				STORAGE_TEMPLATE : computeTemplate{
					// Mandatory. Image ID.
					imageId linuxImageId

					// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
					remoteDirectory "/home/gs-files"
					// Mandatory. Amount of RAM available to machine.
					machineMemoryMB 1600
					// Mandatory. Hardware ID.
					hardwareId hardwareId
					// Optional. Location ID.
					locationId locationId
					// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
					localDirectory "upload"

					username "root"

					// enable sudo.
					privileged true
				}
	]