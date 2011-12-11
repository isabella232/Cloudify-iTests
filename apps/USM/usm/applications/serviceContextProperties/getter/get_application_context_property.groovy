import java.util.concurrent.TimeUnit

println "get_application_context_property"

def context = com.gigaspaces.cloudify.dsl.context.ServiceContextFactory.getServiceContext()

println context.properties.service["myKey"];

