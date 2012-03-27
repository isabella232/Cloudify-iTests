/**
 * This service is a mock for a custom service monitor
 * That SGTest can control using custom commands.
 */
service {
	name "customServiceMonitor"
	type "WEB_SERVER" //dummy for test
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
  
  userInterface {

		metricGroups = ([
			metricGroup{
				name "counterGroup"
				metrics ([
					"counter"
				])
			}
		]
		)

		widgetGroups = ([
			widgetGroup{
				name "counter"
				widgets ([
					barLineChart{
						metric "counter"
						axisYUnit Unit.REGULAR
					}
				])
			}
		]
		)
	}
}