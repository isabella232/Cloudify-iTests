[
	ULIMIT : template	{		
								
							imageId ec2LinuxImageId
							machineMemoryMB 1600
							hardwareId "m2.small"
							localDirectory "upload"
					
							username "ec2-linux"
					
							remoteDirectory "/home/ec2-linux/gs-files"
							openFilesLimit "12345"
						}
]