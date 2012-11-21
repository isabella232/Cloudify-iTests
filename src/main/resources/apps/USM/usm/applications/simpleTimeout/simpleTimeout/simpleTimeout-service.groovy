service {
	name "simpleTimeout"
	type "UNDEFINED"
	
	lifecycle {

		//init { println "This is the init event" }

		preInstall {println "This is the preInstall event" }
		postInstall {println "This is the postInstall event"}
		
		// preStart {println "This is the preStart event" }

		start (["Win.*":"run.bat", "Linux":"run.sh"])

		postStart {Thread.sleep(75000)}

		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown {println "This is the shutdown event" }
	}


	userInterface {
		metricGroups = [
			metricGroup{
				name = "process"
				metrics = ["cpu", "memory"]
			},
			metricGroup{
				name = "space"
				metrics = ["reads", "writes"]
			}
		]
		widgetGroups = [
			widgetGroup{
				name  ="cpu"
				widgets = [
					balanceGauge{metric = "cpu"},
					barLineChart{metric = "cpu"}
				]
			},
			widgetGroup {
				name = "memory"
				widgets = [
					balanceGauge { metric = "memory" },
					barLineChart{ metric = "memory"
						axisYUnit Unit.PERCENTAGE

						}
				]
			}
		]
	}
	
}