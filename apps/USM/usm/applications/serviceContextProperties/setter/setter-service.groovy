service {
	name "setter"
	icon "icon.png"
	lifecycle {
		
				//init { println "This is the init event" }
		
				preInstall {println "This is the preInstall event" }
				postInstall {println "This is the postInstall event"}
				
				// preStart {println "This is the preStart event" }
		
				start (["Win.*":"run.bat", "Linux":"run.sh"])
		
				postStart {println "This is the postStart event" }
		
				preStop {println "This is the preStop event" }
				postStop {println "This is the postStop event" }
				shutdown {println "This is the shutdown event" }
			}
		
			plugins = [
				plugin {
					name "jmx"
					className "com.gigaspaces.cloudify.usm.jmx.JmxMonitor"
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
	customCommands ([
	            "cmd1" : {context.properties.application["myKey"] = "myValue"},
				"cmd2" : "set_application_context_property.groovy"
			])

}