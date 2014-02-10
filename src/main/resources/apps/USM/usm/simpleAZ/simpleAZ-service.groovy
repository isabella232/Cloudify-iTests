service {
	name "simpleAZ"
	type "UNDEFINED"
	elastic true
	numInstances 4
	minAllowedInstances 4
	maxAllowedInstances 4
	
	compute {
		template "SMALL_LINUX"
	}
	
	storage {
		template "SMALL_BLOCK"
	}
	
	lifecycle {

		//init "init.groovy";//{ println "This is the init event" }

		preInstall {println "This is the preInstall event" }
		postInstall {println "This is the postInstall event"
			//throw new IllegalStateException("HAHA") 
			}
		preStart {println "This is the preStart event" }

		start (["Win.*":"run.bat", "Linux":"run.sh"])

		postStart {println "This is the postStart event" }

		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown {println "This is the shutdown event" }
		
		details { return ["1":1, "2":2] }
		monitors { return ["3":3, "4":4] }
	}

	
	customCommands ([
		"cmd1" : { println "This is the cmd1 custom command"},
		"cmd3" : { throw new Exception("This is an error test")},
		"listRemoteFiles" : "listRemote.sh"
	])

	
	plugins = [
		plugin {
			name "jmx"
			className "org.cloudifysource.usm.jmx.JmxMonitor"
			config ([

						"Details" : [
							"org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess",
							"Details"
						],
						"Counter" : [
							"org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess",
							"Counter"
						],
						"Type" : [
							"org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess",
							"Type"
						],
						port : 9999
					])
		}
	]

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