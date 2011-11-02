service {
	
	icon "icon.png"
	name "stockAnalyticsMirror"
	mirrorProcessingUnit {
		binaries "stockAnalyticsMirror.jar" //can be a folder, or a war file   		
		sla {
				highlyAvailable false
				memoryCapacityPerContainer 256 
			}
	}
	
}
