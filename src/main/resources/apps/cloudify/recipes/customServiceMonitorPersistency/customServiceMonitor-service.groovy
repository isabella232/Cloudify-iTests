service {
	extend "../customServiceMonitor"
	
	// The minimum number of service instances
	// Used together with scaling rules
	minAllowedInstances 1
	
	// the initial number of instances
	numInstances 1
	  
	// The maximum number of service instances
	// Used together with scaling rules
	maxAllowedInstances 3
}