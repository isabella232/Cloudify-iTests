import java.util.concurrent.TimeUnit

println "set_application_instance_context_property"

def context = org.cloudifysource.utilitydomain.context.ServiceContextFactory.getServiceContext()
context.attributes.thisApplication["myKey"] = "myValue";


