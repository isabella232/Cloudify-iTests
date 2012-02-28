cloud {
	
	user "0VCFNJS3FXHYC7M6Y782"
	apiKey "fPdu7rYBF0mtdJs1nmzcdA8yA/3kbV20NgInn4NO"
	provider "aws-ec2"
	localDirectory "/home/ec2-user/gs-files"
	remoteDirectory "/home/ec2-user/gs-files"
	
	imageId "us-east-1/ami-38c33651"
	machineMemoryMB "613"
	hardwareId "m1.micro"

	securityGroup "default"
	
	keyFile "cloud-demo.pem"
	keyPair "cloud-demo"
	
	cloudifyUrl "https://s3.amazonaws.com/test-repository-ec2dev/cloudify/gigaspaces.zip"
	machineNamePrefix "gs_esm_gsa_"

	dedicatedManagementMachines true
	managementOnlyFiles ([])
	connectedToPrivateIp false
	
	sshLoggingLevel java.util.logging.Level.WARNING
	managementGroup "management_machine"
	numberOfManagementMachines 2
	
	zones (["agent"])

	reservedMemoryCapacityPerMachineInMB 256
	
}