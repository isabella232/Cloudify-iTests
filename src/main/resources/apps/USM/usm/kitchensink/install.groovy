import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.context.Service;
import org.cloudifysource.dsl.context.ServiceContext;
import org.cloudifysource.dsl.context.ServiceContextFactory;
import org.cloudifysource.dsl.context.ServiceInstance;

println "install event fired"

ServiceContext context = new ServiceContextFactory().getServiceContext()

Service service =  context.waitForService("kitchensink-service", 20, TimeUnit.SECONDS);
if(service == null) { 
	throw new Exception("FAIL! could not find service")
}
ServiceInstance[] instances = service.waitForInstances(1, 30, TimeUnit.SECONDS);

if(instances == null) {
	throw new Exception("FAIL! Instances list is null")
}

if(instances.length == 0) {
	throw new Exception("FAIL! could not find an instance of the service")
}
