service {
	
	name "statelessPU"
	numInstances 1
	statelessProcessingUnit {
	
		binaries "servlet.war" //can be a folder, or a war file   
		
		sla {
				//memoryCapacity 128
				//maxMemoryCapacity 256
				highlyAvailable true
				memoryCapacityPerContainer 128 
			}
	}
	
}