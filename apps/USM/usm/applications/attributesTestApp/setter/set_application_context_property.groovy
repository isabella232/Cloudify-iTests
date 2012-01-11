import java.util.concurrent.TimeUnit

println "set_application_instance_context_property"

def context = org.cloudifysource.dsl.context.ServiceContextFactory.getServiceContext()
context.attributes.thisApplication["myKey"] = "myValue";


