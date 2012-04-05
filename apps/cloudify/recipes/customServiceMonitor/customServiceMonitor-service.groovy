import java.util.concurrent.atomic.AtomicLong;

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
      start "run.groovy"

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
  minAllowedInstances 2
    
  // The maximum number of service instances
  maxAllowedInstances 4
     
  // Defines an automatic scaling rule based on "counter" metric value
  scalingRules {
   
    //The time (in seconds) between two consecutive metric samples
    samplingPeriodInSeconds 1
         
    // The name of the metric that is the basis for the scale rule decision
    metric "counter"
    
    // The sliding time range (in seconds) for aggregating per-instance metric samples
    // The number of samples in the time windows equals the time window divided by the sampling period
    movingTimeRangeInSeconds 5
    
    // (Optional)
    // The algorithms for aggregating metric samples by instances and by time.
    // Metric samples are aggregated separately per instance in the specified time range,
    // and then aggregated again for all instances.
    // Default: Statistics.averageOfAverages
    // Possible values: Statistics.maximumOfAverages, Statistics.minimumOfAverages, Statistics.averageOfAverages, Statistics.percentileOfAverages(90)
    //                  Statistics.maximumOfMaximums, Statistics.minimumOfMinimums
    statistics Statistics.averageOfAverages
        
    // The instancesStatistics over which the number of instances is increased or decreased
    highThreshold 90
    
    // The instancesStatistics below which the number of instances is increased or decreased
    lowThreshold 30

  }
}