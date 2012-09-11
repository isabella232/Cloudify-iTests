import java.util.List;

service {
    
	name "simple"
	type "UNDEFINED"
	
	lifecycle {

		preInstall {
            println "This is the preInstall event" 
        }
		
        postInstall {
            println "This is the postInstall event"
		}
		
        preStart {
            println "This is the preStart event" 
         }

		start (["Win.*":"run.bat", "Linux":"run.sh"])
        
		// This does not work here
//		startDetection {
//			USMUtils.checkPortsOpen ([7777], "127.0.0.1", 60)
//		}
		
		postStart {   
            println "This is the postStart event"
        }

		preStop {
            println "This is the preStop event, going to sleep" 
            Thread.sleep(30000)
            println "__preStop__ Waking up after sleep"
        }
		
        postStop {
            println "This is the postStop event, going to sleep" 
            Thread.sleep(30000)
            println "__postStop__ Waking up after sleep"
        }
        
		shutdown {
            println "This is the shutdown event" 
        }
	}

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
			metricGroup {
				name = "process"
				metrics = ["cpu", "memory"]
			},
			metricGroup {
				name = "space"
				metrics = ["reads", "writes"]
			}
		]
		widgetGroups = [
			widgetGroup {
				name = "cpu"
				widgets = [
					balanceGauge { metric = "cpu" },
					barLineChart { metric = "cpu" }
				]
			},
			widgetGroup {
				name = "memory"
				widgets = [
					balanceGauge { metric = "memory" },
					barLineChart { 
                        metric = "memory"

						axisYUnit Unit.PERCENTAGE
                    }
				]
			}
		]
	}
}