/**
 * This service is a mock for a custom service monitor
 * That SGTest can control using custom commands.
 */
service {
	name "customServiceMonitor"

	long counter = 0;
  
    lifecycle {
		start "run.groovy" 

		monitors ([
			"counter" : {
				println "counter=${counter}" 
				return counter
			}
		])
	}

	customCommands ([
				"add" : {x -> counter += (x as Long);},
				"sub" : {x -> counter -= (x as Long);},
				"set" : {x -> counter = (x as Long);},
  ])
}