import java.util.concurrent.TimeUnit

println "set_instance_context_property"

def context = com.gigaspaces.cloudify.dsl.context.ServiceContextFactory.getServiceContext()
context.properties.instance["myKey"] = "myValue";


