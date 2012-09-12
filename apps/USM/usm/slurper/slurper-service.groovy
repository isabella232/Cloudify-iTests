

service {
	icon   conf.icon
	name   conf.name3
	type   conf.type
	
	lifecycle {

		init { println "init" }

		preInstall {println "This is the preInstall event" + conf.type3 }
		postInstall {println "This is the postInstall event"
			}
		preStart {println "This is the preStart event" }

		start (["Win.*":"run.bat", "Linux":"run.sh"])

		postStart {println "This is the postStart event" }

		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown {println "This is the shutdown event" }

				details(["name" : {conf.name3},
					"icon" : {conf.icon},
					"type" : {conf.type},
					"tripletype" : {conf.type3},
					"jmx-port" : {"${jmx.ports.JMX_PORT}"},
					"placeholderProp" : {property2},
					"systemProp" : {systemProp}
				])
	}

	
	customCommands ([
		"cmd1" : { println "This is the cmd1 custom command"},
		"cmd3" : { throw new Exception("This is an error test")}
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
						port : jmx.ports.JMX_PORT
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