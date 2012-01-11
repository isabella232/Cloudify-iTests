import java.util.concurrent.TimeUnit

println "set_instance_context_property"

def context = org.cloudifysource.dsl.context.ServiceContextFactory.getServiceContext()
context.attributes.thisInstance["myKey"] = "myValue";


