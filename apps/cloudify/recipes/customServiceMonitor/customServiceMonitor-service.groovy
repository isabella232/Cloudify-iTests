import java.util.concurrent.atomic.AtomicLong;
//import org.cloudifysource.dsl.autoscaling.AutoScalingStatisticsFactory as statistics;

// This service is a mock for a custom service monitor
// That SGTest can control using custom commands.
// It includes automatic scaling rules that react to changes of the monitor value
service {
  
  
	name "customServiceMonitor"
	
	// web server mock
	type "WEB_SERVER" 
	
	// In memory per-instance counter that is exposed as the "counter" monitor
	AtomicLong counter = new AtomicLong(0);
  
    lifecycle {

      //sleep forever
      start { while (true) { sleep (1000)} } //"run.groovy"

      // expose counter as a monitor with the name "counter"
      monitors ([
        "counter" : {
          long value = counter.get();
          println "counter=${value}" 
          return value
        }
      ])
	}

  // A hook for modifying the counter from the CLI
  // cloudify "connect localhost;invoke customServiceMonitors set 4"
	customCommands ([
				"set" : {x -> counter.set(x as Long);}
  ])
  
  userInterface {
    
    // defines a UI menu "counter" group with the "counter" metric
		metricGroups = ([
			metricGroup{
				name "counter"
				metrics ([
					"counter"
				])
			}
		]
		)
		
    // defines a UI line chart widget showing the "counter" metric value
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

	// global flag that enables changing number of instances for this service
	elastic true

  // the initial number of instances
  numInstances 2
     
  // The minimum number of service instances
  minNumInstances 2
    
  // The maximum number of service instances
  maxNumInstances 20
     
	// Defines an automatic scaling rule based on "counter" metric value
  autoScaling {
   
    //The time (in seconds) between two consecutive metric samples
    samplingPeriodSeconds 1
         
    // The name of the metric that is the basis for the scale rule decision
    metric "counter"
    
    // The sliding time window (in secods) for aggregating per-instance metric samples
    // The number of samples in the time windows equals the time window divided by the sampling period
    timeWindowSeconds 5
    
    // (Optional)
    // The algorithm for aggregating metric samples in the specified time window.
    // Metric samples are aggregated separately per instance.
    // Default: statistics.average
    // Possible values: statistics.average, statistics.minimum, statistics.maximum, statistics.percentile(n)
    timeStatistics statistics.average
    
    // (Optional)
    // The aggregation of all instances' timeStatistics
    // Default value: statistics.maximum
    // Possible values: statistics.average, statistics.minimum, statistics.maximum, statistics.percentile(n)
    instancesStatistics statistics.maximum
    
    // The instancesStatistics over which the number of instances is increased or decreased
    highThreshold 90
    
    // The instancesStatistics below which the number of instances is increased or decreased
    lowThreshold 10

  }
}