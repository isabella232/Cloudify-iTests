import java.util.concurrent.TimeUnit


service {
	name "groovy"
	type "UNDEFINED"
	
	elastic true
	
	lifecycle { 

		start "run.groovy" 
				
		startDetection {
			new File(context.serviceDirectory + "/marker.txt").exists()
		}
				
		
	}
	
	customCommands ([
		"sudo": "sudo ls"
	])
		
	

}