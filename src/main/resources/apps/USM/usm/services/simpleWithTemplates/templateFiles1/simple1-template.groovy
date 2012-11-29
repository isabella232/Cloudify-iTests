[
				SIMPLE_TEMPLATE1 : template{
				// Mandatory. Amount of RAM available to machine.
				machineMemoryMB 1600
				// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
				remoteDirectory "/tmp/gs-files"
				// Optional. template-generic credentials. Can be overridden by specific credentials on each node, in the nodesList section.
				username "tgrid"
				password "tgrid"
				
				// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
				localDirectory "upload1"
				
				// Mandatory for BYON.
				custom ([
					// Mandatory for BYON. The nodesList custom property lists the nodes that compose this cloud-like environment.
					// For each node required:
					// An alias for this node. can be static or use a template with an dynamic-index.
					// The server's private (local) IP. can be a single IP, a list, a range or CIDR.
					//Optional - user and password for the node. can be taken from general cloud configuration.
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