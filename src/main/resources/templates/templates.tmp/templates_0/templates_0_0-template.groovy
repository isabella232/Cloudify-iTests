[
				templates_0_0 : computeTemplate{
				
				machineMemoryMB 1600
				remoteDirectory "/tmp/gs-files"
				username "tgrid"
				password "tgrid"
				
				localDirectory uploadDir
				fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SCP
				custom ([
					"nodesList" : ([
									([
										"id" : node_id,
										"host-list" : node_ip
									])
					])
				])
				
				// enable sudo.
				privileged false
				
				}
	]
