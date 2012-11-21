
service {
	name "notepad-service"
	
	lifecycle {
		init {

			println "hello"
			println "world"
			println "instance ID: " + context.instanceId
		}
		preStart (["Win.*":"echo starting notepad"])

		start (["Linux" : "./np.sh"])
		//postStart "postStart.groovy"

		/*postStop {
			println "This is the postStop event"
			println "Context: " + context
			println "Context Service: " + context.service
			println "Service Name: " + context.service.name
			println "Admin: " + context.admin
			println "Service Planned Instances: " + context.service.numberOfPlannedInstances
			println "Service Actual Instances: " + context.service.numberOfActualInstances
			if(context.service.numberOfActualInstances > 0) {
				context.service.instances.each {
					println "Service Instance ID: " + it.instanceID
					println "Service Instance Host Address: " + it.hostAddress
					println "Service Instance Host Name: " + it.hostName
				}
			}
		}*/
	}

	customCommands ([
				"cmd1" : customCommand {
					name "cmd1"
					executeOnce { println "This is the execute once"}
					executeOnAllInstances { println "This is the execute on all instances" }
				},
				"cmd2" : customCommand {
					name "cmd2"
					executeOnce { println "This is the execute once"}
					executeOnAllInstances { println "This is the execute on all instances" }
				}
			])

	plugins ([
		plugin {
			config ([port : 1234 , someval : "a:b:c"])
			name "jmx"
			className "org.cloudifysource.usm.jmx.JmxMonitor"
		}
	])

	userInterface {
		metricGroups = [
			metricGroup {
				name 'processEquals'
				metrics = ['cpu', 'memory']
			},
			metricGroup {
				name 'space'
				metrics = ['reads', 'writes']
			}
		]

		widgetGroups = [
			widgetGroup {
				name 'cpu'
				widgets = [
					balanceGauge { metric = 'cpu' },
					barLineChart { metric = 'cpu' }
				]
			},
			widgetGroup {
				name = 'memory'
				widgets = [
					balanceGauge { metric = 'memory' },
					barLineChart { metric = 'memory' }
				]
			}
		]
	}
}