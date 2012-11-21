service {
	
	name "datagrid"

	dataGrid {

		sla {
				memoryCapacity 128
				maxMemoryCapacity 256
				highlyAvailable false
				memoryCapacityPerContainer 128 
		}

		contextProperties ([
				//this is the usual deployment properties mechanism 
				"cluster-config.mirror-service.interval-opers":"1000"
		])
	}
	
}