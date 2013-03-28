service {
	
	name "statefulPU"
	
	statefulProcessingUnit {
	
		binaries "stateful.jar" //can be a folder, or a war file   
	
		sla {
				memoryCapacity 128
				maxMemoryCapacity 128
				highlyAvailable false
				memoryCapacityPerContainer 128
			}
	
		contextProperties ([
				//this is the usual deployment properties mechanism 
				"cluster-config.mirror-service.interval-opers":"1000"
		])
	}
	
}