service {
	name "simple"
	type "UNDEFINED"
	
	lifecycle {


		//start (["Win.*":"run.bat", "Linux":"run.sh"])
		start "run.groovy"

		stop {println "This is the Stop event" }
		
		//startDetection {ServiceUtils.isPortOccupied(7777)}
		
		startDetection {
			new File(context.serviceDirectory + "/marker.txt").exists()
		}

	}

	
}